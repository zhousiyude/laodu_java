# Java 集合并发修改异常与 fail-fast / modCount 机制笔记

> 说明：常说的“模数印记机制”通常指 Java 集合中的 **modCount（modification count，修改计数）机制**，有的资料也译作“修改印记/修改计数”。它不是数学意义上的“模数”。

## 1. 三个概念

### 1.1 ConcurrentModificationException（CME）

`ConcurrentModificationException` 是 `java.util` 下的运行时异常。集合在迭代过程中如果检测到**结构性修改**，可能抛出该异常。

注意：名字里有 “Concurrent”，但**单线程也可能抛出**。它表示“迭代过程中集合被修改了”，不一定是多线程并发。

### 1.2 fail-fast 快速失败机制

fail-fast 是 Java 集合迭代器的一种**尽力而为**的错误检测机制：

> 迭代器遍历时，如果发现集合结构被外部修改，就尽快抛出 `ConcurrentModificationException`，避免继续使用可能不一致的数据。

关键点：

- 不是线程安全保证；
- 不保证一定能检测到所有并发修改；
- 主要用于尽早暴露 bug；
- `ArrayList`、`HashMap` 等普通集合的迭代器通常是 fail-fast；
- `ConcurrentHashMap`、`CopyOnWriteArrayList` 等并发容器通常是弱一致性/fail-safe 风格，不抛 CME。

### 1.3 modCount 修改计数机制

普通集合内部维护一个修改计数器，例如 `AbstractList` 中：

```java
protected transient int modCount = 0;
```

当集合发生**结构性修改**时，`modCount++`。例如：

- `ArrayList.add()`、`remove()`、`clear()`；
- `HashMap.put()` 新增映射、`remove()` 删除映射等。

迭代器创建时会保存一个快照：

```java
int expectedModCount = modCount;
```

之后每次迭代操作检查：

```java
if (modCount != expectedModCount) {
    throw new ConcurrentModificationException();
}
```

如果集合被外部修改，`modCount` 变化，而迭代器的 `expectedModCount` 没变，就会检测到不一致并抛异常。

---

## 2. 工作机制

以 `ArrayList` 迭代器为例，简化伪代码：

```java
private class Itr implements Iteratore {
    int cursor;
    int lastRet = -1;
    int expectedModCount = modCount; // 创建迭代器时记录“修改印记”

    public boolean hasNext() {
        return cursor != size;
    }

    public E next() {
        checkForComodification();
        // 返回元素并移动 cursor
    }

    public void remove() {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();

        ArrayList.this.remove(lastRet);

        cursor = lastRet;
        lastRet = -1;
        expectedModCount = modCount; // 迭代器自己的合法删除，同步印记
    }

    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```

流程可以概括为：

1. 集合维护 `modCount`；
2. 迭代器创建时保存 `expectedModCount = modCount`；
3. 每次 `next()`、`remove()` 等操作前检查两者是否相等；
4. 不相等则抛出 `ConcurrentModificationException`；
5. 如果是迭代器自己的 `remove()`，删除后会把 `expectedModCount` 更新为最新 `modCount`，避免误报。

---

## 3. 典型触发场景

### 3.1 增强 for 循环中直接删除

```java
Liststring list = new ArrayList<>(List.of("a", "b", "c"));

for (String s : list) {
    if ("b".equals(s)) {
        list.remove(s); // 可能抛 ConcurrentModificationException
    }
}
```

增强 for 底层使用迭代器。`list.remove()` 修改了集合的 `modCount`，但迭代器的 `expectedModCount` 没更新，下一次 `next()` 检查时抛 CME。

### 3.2 多线程遍历与修改

```java
Liststring list = new ArrayList<>();
// 线程 A：遍历 list
// 线程 B：add/remove list
```

可能抛 CME。但要注意：`modCount` 不是 `volatile`，没有同步时也可能出现其他不可预期行为。**不能依赖 fail-fast 做并发控制。**

### 3.3 单线程也会抛

CME 不等于多线程问题。只要是“迭代过程中外部结构性修改”，单线程也会触发。

---

## 4. 正确写法

### 4.1 使用 Iterator.remove()

```java
Iteratorstring it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if ("b".equals(s)) {
        it.remove(); // 合法：迭代器自己的删除
    }
}
```

### 4.2 使用 removeIf()

```java
list.removeIf(s -> "b".equals(s));
```

### 4.3 并发场景使用并发容器或加锁

- 读多写少：`CopyOnWriteArrayList`；
- 高并发 Map：`ConcurrentHashMap`；
- 需要强一致遍历：对集合加锁；
- `Collections.synchronizedList()` 只保证单操作同步，**遍历时仍需要手动同步**。

```java
Liststring syncList = Collections.synchronizedList(new ArrayList<>());

synchronized (syncList) {
    for (String s : syncList) {
        // 遍历期间持有锁
    }
}
```

---

## 5. fail-fast 与 fail-safe 对比


| 特性             | fail-fast                               | fail-safe / 弱一致性                        |
| ---------------- | --------------------------------------- | ------------------------------------------- |
| 典型集合         | `ArrayList`、`HashMap`                  | `ConcurrentHashMap`、`CopyOnWriteArrayList` |
| 迭代时外部修改   | 可能抛`ConcurrentModificationException` | 通常不抛 CME                                |
| 数据一致性       | 尽早失败                                | 弱一致性，可能看到旧快照或部分更新          |
| 是否线程安全保证 | 否                                      | 并发容器提供相应并发语义                    |
| 依赖机制         | `modCount` / `expectedModCount`         | CAS、快照、并发数据结构等                   |

---

## 6. 常见误区

1. **CME 一定是多线程问题？**
   不是。单线程在 foreach 中 `list.remove()` 也会触发。
2. **fail-fast 能保证检测所有并发修改？**
   不能。它是尽力而为机制，不是并发安全机制。
3. **`modCount` 是线程同步变量？**
   不是。它通常只是普通字段，不是 `volatile`，不能替代锁或并发容器。
4. **`Collections.synchronizedList()` 后遍历就安全？**
   不一定。复合操作和遍历仍需手动同步。
5. **所有修改都会增加 `modCount`？**
   通常只有结构性修改会增加。比如 `ArrayList.set()` 不改变大小，一般不算结构性修改；`HashMap` 替换已有值通常也不增加 `modCount`。具体以实现为准。

---

## 7. 总结

- `ConcurrentModificationException`：迭代过程中检测到集合被修改时抛出的运行时异常。
- fail-fast：普通集合迭代器的快速失败机制，尽早暴露不一致。
- modCount：集合的结构性修改计数器；迭代器用 `expectedModCount` 保存创建时的“修改印记”，两者不一致就抛 CME。
- 正确删除：用 `Iterator.remove()`、`removeIf()`，不要 foreach 中直接 `add/remove`。
- 并发场景：使用 `ConcurrentHashMap`、`CopyOnWriteArrayList` 或加锁，不要依赖 fail-fast 保证线程安全。
- 一句话记忆：**集合改，印记变；迭代器对不上，就抛 CME。**
