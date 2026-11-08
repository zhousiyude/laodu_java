对，你的核心理解是对的：**最终某个桶里出现 `A.next = B`、`B.next = A`，形成环；后续 `put` 遍历该桶时，预期靠 `e = e.next` 走到 `null` 结束，但环里永远没有 `null`，于是死循环。**
不过要精确一点：**不是 `put` 导致了环，而是并发扩容导致了环；`put/get` 只是命中这个环后“受害者”。** 术语上也不叫“循环依赖”，更准确叫**环形链表 / 链表成环 / 死循环**。

下面是一篇从 0 到 1 的笔记。

---

# HashMap 头插法并发扩容成环笔记

## 一句话结论

> **JDK 1.7 及之前的 HashMap 扩容用头插法，会把链表顺序反转；多线程并发扩容时，两个线程交叉修改节点 `next` 指针，可能把链表接成环。环形成后，后续 `put/get` 遍历这个桶时永远走不到 `null`，导致死循环、CPU 100%。**

---

## 0. 前置知识：HashMap 的桶是链表

HashMap 底层是数组 + 链表。某个数组下标 `i` 对应的位置叫一个“桶”：

```text
table[i] -> A -> B -> null
```

`put` 一个 key 时，会先遍历这个桶的链表：

```java
Entry e = table[i];
while (e != null) {
    if (e.key.equals(key)) {
        // 覆盖
    }
    e = e.next;  // 靠 next 走到 null 结束
}
```

正常链表一定有结尾：

```text
A -> B -> null
```

遍历过程：

```text
e=A -> e=B -> e=null -> 停止
```

---

## 1. 头插法扩容：单线程下会“反转链表”

JDK 1.7 扩容时，把旧数组元素迁移到新数组。迁移链表节点用的是**头插法**：

```java
while (e != null) {
    Entry next = e.next;
    e.next = newTable[i]; // 插到新桶头部
    newTable[i] = e;
    e = next;
}
```

单线程下，假设旧桶是：

```text
旧桶: A -> B -> null
```

迁移过程：

```text
处理 A：新桶 A -> null
处理 B：新桶 B -> A -> null
```

结果：

```text
旧顺序: A -> B -> null
新顺序: B -> A -> null
```

**头插法导致链表顺序反转**。单线程没问题，但多线程下这个“反转”很危险。

---

## 2. 并发扩容事故：两个线程交叉改指针

假设两个线程 T1、T2 同时扩容，初始链表：

```text
旧桶: head -> A -> B -> null
```

T2 先执行了一点，记录下：

```text
T2: e = A
T2: next = B
```

然后 T2 被挂起。

T1 继续完成扩容。因为头插法会反转链表，T1 迁移后变成：

```text
T1 新桶: B -> A -> null
```

此时节点指针已经被 T1 改过：

```text
B.next = A
A.next = null
```

图示：

```text
T1 完成后：
B -> A -> null

即：
B.next = A
A.next = null
```

关键点：**A、B 是共享的节点对象，T1 修改了它们的 `next`。**

---

## 3. T2 恢复执行：环出现了

T2 醒来后，仍然按自己之前的认知继续：

```text
T2: e = A
T2: next = B
```

T2 开始迁移：

```text
T2 处理 A：
A.next = newTable[i]   // 此时 T2 新桶头是 null
newTable[i] = A

结果：A -> null
```

然后 T2 取下一个：

```text
e = B
next = B.next
```

注意：`B.next` 已经被 T1 改成了 `A`，所以：

```text
next = A
```

T2 继续处理 B：

```text
B.next = newTable[i]   // newTable[i] 是 A
newTable[i] = B
```

此时：

```text
B.next = A
新桶头 = B
```

T2 再取下一个：

```text
e = A
next = A.next          // 此时 A.next 是 null
```

T2 又处理一次 A：

```text
A.next = newTable[i]   // newTable[i] 是 B
newTable[i] = A
```

于是：

```text
A.next = B
B.next = A
```

环形成：

```text
A <-> B

A -> B -> A -> B -> A -> ...
```

ASCII 图：

```text
      ┌──────────┐
      │          ▼
      A          B
      ▲          │
      │          │
      └──────────┘

A.next = B
B.next = A
```

---

## 4. 后续 put 为什么死循环？

环形成后，这个桶的链表没有 `null` 结尾。

正常遍历预期：

```text
e=A -> e=B -> e=null -> 停止
```

实际遍历：

```text
e=A -> e=B -> e=A -> e=B -> e=A -> ...
```

`put` 源码里的遍历逻辑类似：

```java
Entry e = table[i];
while (e != null) {
    if (e.key.equals(key)) {
        // 找到则覆盖
    }
    e = e.next;  // 永远不为 null
}
```

图示：

```text
while (e != null) {
    e = e.next;
}

A -> B -> A -> B -> A -> B -> ...
永远走不到 null
```

结果：

```text
该线程陷入死循环
CPU 占用飙升
```

注意：不是所有 `put` 都会触发。只有新 key 哈希到**这个有环的桶**，`put` 遍历该桶时才会死循环。`get` 命中该桶同样可能死循环。

---

## 5. 你的理解检查

你说：

> 结局就是 A 的 next 为 B，B 的 next 为 A，put 新的元素预期循环到 next=null 停止，但是永远没有 null 出现，导致问题。

**对，核心正确。** 补充三点：

1. 环不是 `put` 新元素造成的，而是**并发扩容**造成的。
2. `put` 是触发死循环的操作之一，`get` 命中该桶也会。
3. 不是所有 `put` 都死循环，只有落到那个有环桶的 `put` 才会。

---

## 6. 为什么 JDK 1.8 改尾插法？

JDK 1.8 扩容迁移改成**尾插法**，迁移时保持链表原顺序，不再反转：

```text
旧: A -> B -> null
新: A -> B -> null
```

没有“反转”这个特性，就消除了这种因反转 + 并发交叉修改导致的经典成环问题。

但注意：

> **JDK 1.8 的 HashMap 仍然不是线程安全的。**
> 并发下仍可能丢数据、覆盖数据等。并发场景应该用 `ConcurrentHashMap`。

---

## 7. 记忆口诀

> **头插法扩容会翻链表；**
> **两个线程一个翻完、一个还按旧路线走；**
> **交叉改 `next`，把 A、B 接成环；**
> **环里没有 `null`，后续 `put/get` 遍历就死循环。**

---

## 最终总结

```text
单线程头插法：
A -> B -> null
扩容后：
B -> A -> null
（只是反转，没问题）

并发扩容：
T1 改出 B -> A
T2 按旧认知继续处理 A、B
交叉设置：
A.next = B
B.next = A
形成环：
A <-> B

后续 put/get：
A -> B -> A -> B -> ...
永远没有 null
=> 死循环
```

你的理解可以记成一句：

> **“环形成后，链表遍历永远找不到 `null`，所以 `put` 在该桶检查 key 时会死循环。”**
