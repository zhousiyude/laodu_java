# HashMap.put 从 0 到 1 学习笔记

> 版本基准：JDK 8（JDK 7 的头插法/死循环问题会单独提一句）
> 目标：搞懂 put 全过程 + 彻底理解 `equals` 与 `hashCode` 的契约，附带一个「只重写 equals」的血案现场

---

## 0. 先给结论（背下来）

```
┌─────────────────────────────────────────────────────────────┐
│ ① HashMap = 数组 + 链表 + 红黑树                              │
│ ② put 三步：算 hash → 算下标 → 空就放/冲突就比较/相同就覆盖    │
│ ③ 定位靠 hashCode，判等靠 equals —— 两道关卡，缺一不可        │
│ ④ equals 相等 ⇒ hashCode 必须相等（否则第二道关卡永远到不了）  │
│ ⑤ hashCode 相等 ⇏ equals 相等（这就是哈希冲突，允许，但变慢）  │
└─────────────────────────────────────────────────────────────┘
```

---

## 1. 底层长什么样

HashMap 的肉身上就一个字段最重要：

```java
transient Node<K,V>[] table;   // 桶数组，俗称 "table"
```

每个 `Node` 是链表节点（JDK 8 里 `TreeNode` 继承自它）：

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;      // ★ 缓存了 key 的 hash 值！
    final K key;
    V value;
    Node<K,V> next;      // 链表指针
}
```

注意 `final int hash` —— **hash 是被缓存进节点的**，后续比较不需要重算 `key.hashCode()`。这个细节是后面反例的关键。

```
        HashMap 全景图（默认容量 16）

  index  table[]
  ┌────┬──────────────────────────────────────────────┐
  │ 0  │ null                                          │
  │ 1  │ ●──► [hash|"A"|1] ──► [hash|"Q"|2] ──► null   │  ← 链表（哈希冲突）
  │ 2  │ null                                          │
  │ 3  │ ●──► [hash|"B"|3]                             │
  │ 4  │ null                                          │
  │ …  │ …                                             │
  │ 7  │ ●──► 🌳 红黑树 TreeNode（链表 ≥8 且容量 ≥64）    │
  │ …  │ …                                             │
  │15  │ null                                          │
  └────┴──────────────────────────────────────────────┘
```

**为什么是数组？** 下标随机访问 O(1)。
**为什么还要链表？** 因为 `hash & (n-1)` 只有 n 种结果，多个 key 会撞到同一个桶，叫**哈希冲突**。

---

## 2. put 源码逐行拆解

### 2.1 入口

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}
```

一行就干两件事：先算 hash，再进 `putVal`。

### 2.2 第一步：hash 扰动函数

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

`key == null` 时 hash 恒为 0，所以 **HashMap 允许一个 null key**，且它永远落在 index 0。

那个 `^ (h >>> 16)` 是干嘛的？看下图：

```
h        = 1010 0110 1101 0011 | 0000 0000 0000 0000
h >>> 16 = 0000 0000 0000 0000 | 1010 0110 1101 0011
h ^ h>>>16=1010 0110 1101 0011 | 1010 0110 1101 0011
                                ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
                          高 16 位被"折叠"进低 16 位
```

因为后面算下标只用到低位（`& (n-1)`，n 一般很小），高 16 位的信息本来全浪费了。**扰动 = 让高位也参与运算，降低冲突概率**。这是"用 1 次异或换哈希均匀"的性价比操作。

### 2.3 第二步：算下标

```java
i = (n - 1) & hash      // n = table.length，永远是 2 的幂
```

```
n = 16,  n - 1 = 15 = 0000 1111

hash    = 1010 0110 1101 0011
n - 1   = 0000 0000 0000 1111
          ─────────────────── &（按位与）
index   = 0000 0000 0000 0011  = 3
```

**为什么容量必须是 2 的幂？**
因为 `n - 1` 的二进制是 `00...0111` 这种"低位全 1"的形状，此时 `hash & (n-1)` 恰好等价于 `hash % n`，但**位运算比取模快一个数量级**。如果 n 不是 2 的幂，`&` 就不是取模了，分布会烂掉。

> 顺带：`tableSizeFor()` 会把构造时传的容量向上取整到 2 的幂（比如传 10 → 16）。

### 2.4 第三步：putVal 主干

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;

    // ① 懒初始化：第一次 put 才真正 new 数组（默认 16）
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;

    // ② 该桶是空的 → 直接 new 一个节点放进去，完事
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);

    else {
        // ③ 桶里有人了，开始"找是不是同一个 key"
        Node<K,V> e; K k;

        // ③-a 先看头节点
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;

        // ③-b 是红黑树 → 交给树去插
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);

        // ③-c 遍历链表
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {          // 走到尾巴都没找到
                    p.next = newNode(hash, key, value, null);  // 尾插
                    if (binCount >= TREEIFY_THRESHOLD - 1)     // 7 → 第8个
                        treeifyBin(tab, hash);                 // 尝试树化
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;                            // 找到相同 key
                p = e;
            }
        }

        // ④ 找到了相同 key → 覆盖 value，返回旧值
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }

    ++modCount;
    // ⑤ size 超过阈值(容量×0.75) → 扩容
    if (++size > threshold) resize();
    afterNodeInsertion(evict);
    return null;   // 新增 key 时返回 null
}
```

### 2.5 put 全流程图

```
                      put(key, value)
                             │
                             ▼
        ┌────────────────────────────────────────┐
        │ ① hash = key.hashCode() ^ (h >>> 16)   │  null key → hash = 0
        └────────────────────────────────────────┘
                             │
                             ▼
        ┌────────────────────────────────────────┐
        │ ② i = (n - 1) & hash                   │  等价于 hash % n
        └────────────────────────────────────────┘
                             │
                             ▼
                 ┌───────────────────────┐
                 │ tab[i] == null ?      │
                 └───────────────────────┘
                    │是                │否
                    ▼                  ▼
           ┌──────────────┐   ┌──────────────────────────────────┐
           │ 直接 new Node │   │ ③ 比较头节点：                    │
           │ 放进去 ✅     │   │    p.hash == hash                │
           └──────────────┘   │    && (p.key == key              │
                    │         │        || key.equals(p.key))    │
                    │         └──────────────────────────────────┘
                    │              │相同            │不同
                    │              ▼                ▼
                    │      ┌──────────────┐  ┌──────────────────┐
                    │      │ ④ 覆盖 value │  │ 是 TreeNode ?     │
                    │      │ 返回 oldValue│  └──────────────────┘
                    │      └──────────────┘     │是        │否
                    │                           ▼          ▼
                    │                    ┌──────────┐ ┌────────────────┐
                    │                    │ 红黑树插入│ │ 遍历链表        │
                    │                    └──────────┘ │ 找到 → 覆盖     │
                    │                                 │ 没找到 → 尾插   │
                    │                                 │ 长度≥8 → 树化   │
                    │                                 └────────────────┘
                    ▼
        ┌────────────────────────────┐
        │ ⑤ ++size > threshold ?     │  threshold = capacity × 0.75
        │    是 → resize() 扩容       │  默认 16 × 0.75 = 12
        └────────────────────────────┘
```

### 2.6 关键细节：比较顺序为什么是 `hash` 在前？

```java
if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
//  └── 关卡1 ──┘     └── 关卡2a ──┘   └────── 关卡2b ────────┘
//   int 比较          引用比较            equals 逐字段比较
//   纳秒级，极快      几乎免费            可能很慢（比如比较 10 个字段）
```

**这就是"两道关卡"**：

```
        ┌─────────────────────────────────────────────┐
        │  关卡 1：p.hash == hash ？                    │  ← 靠 hashCode
        │     ✗ 不同 → 直接跳过，equals 根本不调用      │
        │     ✓ 相同 → 进入关卡 2                       │
        └─────────────────────────────────────────────┘
                            │
                            ▼
        ┌─────────────────────────────────────────────┐
        │  关卡 2：== 或 equals ？                      │  ← 靠 equals
        │     ✓ → 认定是同一个 key，覆盖 value          │
        │     ✗ → 不是同一个 key，继续找/挂链表         │
        └─────────────────────────────────────────────┘
```

**记住这张图，第 7 节的血案就是"关卡 1 就把你拦死了"。**

### 2.7 顺带：树化的双阈值

```java
static final int TREEIFY_THRESHOLD   = 8;   // 链表 ≥ 8 考虑树化
static final int UNTREEIFY_THRESHOLD = 6;   // 树 ≤ 6 退化成链表（留 2 的缓冲防抖动）
static final int MIN_TREEIFY_CAPACITY = 64; // ★ 数组 < 64 时优先扩容，不树化
```

`treeifyBin()` 里第一件事是：

```java
if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
    resize();   // 数组太小，扩容比树化更划算
```

**意思就是：小表先扩容散列，大表才上红黑树。**

### 2.8 扩容 resize（简版）

阈值：`threshold = capacity * loadFactor = 16 * 0.75 = 12`，**第 13 个元素**触发扩容到 32。

JDK 8 扩容有个很妙的优化：**不需要重新算 hash**，因为新容量是旧容量 ×2，元素只有两种去处：

```
oldCap = 16 = 0001 0000
                  ↑
              就看这新增的一位

hash = xxxx ? xxxx
            │
            ├── 该位 = 0 → 留在原索引 j
            └── 该位 = 1 → 挪到 j + oldCap（即 j + 16）
```

源码里体现为 `loHead/loTail` 和 `hiHead/hiTail` 两条链，一次遍历拆成两队，`& oldCap` 判断走哪队。

> JDK 7 是头插法 + 每个元素重算下标，多线程扩容会成环死循环；JDK 8 改尾插 + 高低位拆分，好很多（但 HashMap 依然不是线程安全的，并发用 ConcurrentHashMap）。

---

## 3. equals 与 hashCode 的契约

这是 `java.lang.Object` 定下的规矩，**不是 HashMap 发明的**，HashMap 只是这个规矩的"执行者"和"受害者"。

```
┌──────────────────────────────────────────────────────────────────┐
│                         Object 规范契约                            │
├──────────────────────────────────────────────────────────────────┤
│  C1. 自反：x.equals(x) 恒为 true                                   │
│  C2. 对称：x.equals(y) == y.equals(x)                              │
│  C3. 传递：x.equals(y) && y.equals(z) ⇒ x.equals(z)                │
│  C4. 一致：只要参与比较的字段没变，多次调用结果不变                  │
│  C5. ★ 若 x.equals(y) 为 true，则 x.hashCode() == y.hashCode()     │
│  C6. ★ 若 x.equals(y) 为 false，hashCode 可以相同也可以不同（允许冲突）│
└──────────────────────────────────────────────────────────────────┘
```

用集合论画出来就是：

```
      ┌───────────────────────────────────────────────┐
      │  所有对象                                       │
      │   ┌─────────────────────────────────────┐     │
      │   │ hashCode 相同的一堆（同一个桶）        │     │
      │   │    ┌───────────────┐                │     │
      │   │    │ equals 相同    │ ← 必须全在      │     │
      │   │    │ （同一个 key） │   同一堆里！     │     │
      │   │    └───────────────┘                │     │
      │   │    ○ ○ ○  ← equals 不同但 hashCode 相同│     │
      │   │            （哈希冲突，合法）          │     │
      │   └─────────────────────────────────────┘     │
      └───────────────────────────────────────────────┘
```

一句话：

> **hashCode 决定"去哪个桶找"，equals 决定"桶里哪个才是它"。**
> 你俩 equals 说是一家人，hashCode 却把你们分到两个桶，那 HashMap 连找你的机会都没有。

---

## 4. 血案现场：只重写 equals，不重写 hashCode

### 4.1 代码

```java
import java.util.*;

public class BadKeyDemo {

    // ❌ 只重写 equals，不重写 hashCode
    static class BadKey {
        private final String name;
        BadKey(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BadKey other = (BadKey) o;
            return Objects.equals(name, other.name);
        }
        // 故意不写 hashCode()
    }

    // ✅ 两个都重写
    static class GoodKey {
        private final String name;
        GoodKey(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GoodKey other = (GoodKey) o;
            return Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);   // 用相同的字段算 hash
        }
    }

    public static void main(String[] args) {
        // ===== 反例 =====
        Map<BadKey, String> bad = new HashMap<>();
        BadKey k1 = new BadKey("A");
        bad.put(k1, "张三");

        BadKey k2 = new BadKey("A");            // 逻辑上"相等"的新对象
        System.out.println("k1.equals(k2)       = " + k1.equals(k2));   // true
        System.out.println("bad.get(k2)         = " + bad.get(k2));     // null ❗️
        System.out.println("bad.get(k1)         = " + bad.get(k1));     // 张三（同一对象）

        // ===== 正例 =====
        Map<GoodKey, String> good = new HashMap<>();
        good.put(new GoodKey("A"), "张三");
        System.out.println("good.get(new GoodKey(\"A\")) = "
                + good.get(new GoodKey("A")));  // 张三 ✅
    }
}
```

### 4.2 运行结果

```
k1.equals(k2)       = true
bad.get(k2)         = null      ❗️
bad.get(k1)         = 张三
good.get(new GoodKey("A")) = 张三
```

`equals` 都说 true 了，`get` 却返回 null。诡异吧？画图。

### 4.3 为什么？看图

`BadKey` 没重写 `hashCode`，用的是 `Object.hashCode()`，**它基于对象的内存地址（准确说是 JVM 实现相关的对象头信息），每个 new 出来的对象几乎都不一样。**

```
【put 阶段】  bad.put(new BadKey("A") /* 记为 @1a2b */, "张三")

   @1a2b.hashCode() = 0x1a2b
        │
        ▼
   hash 扰动 → 假设 index = 3

   table:
   ┌────┬──────────────────────────────────────┐
   │ 0  │ null                                  │
   │ 1  │ null                                  │
   │ 2  │ null                                  │
   │ 3  │ ● [hash=H₁ | key=BadKey@1a2b | "张三"] │ ← 存这儿了
   │ 4  │ null                                  │
   │ …  │ …                                     │
   └────┴──────────────────────────────────────┘

【get 阶段】  bad.get(new BadKey("A") /* 记为 @7f3c */)

   ① 算 hash：@7f3c.hashCode() = 0x7f3c  ← 和 0x1a2b 完全不同！
                     │
                     ▼
   ② 算下标：hash 扰动 → index = 11

   table:
   ┌────┬──────────────────────────────────────┐
   │ 3  │ ● [hash=H₁ | key=BadKey@1a2b | "张三"] │ ← 你要的东西在这儿
   │ …  │ …                                     │
   │ 11 │ null   ← ★ 你查的是这儿！              │
   └────┴──────────────────────────────────────┘

   ③ tab[11] == null  →  直接 return null
   ✗ equals 一次都没被调用！
```

**关键就在关卡 1：**

```java
if (p.hash == hash && ... key.equals(k))
//      ↑
//   H₁ vs H₂，根本不等，短路了
//   就算 p 就在眼前，equals 也没机会执行
```

而 `bad.get(k1)` 能取到，是因为 `k1` 是**同一个对象**，`hashCode()` 每次都返回同一个值（Object.hashCode 对同一对象稳定），自然落回 index 3。

### 4.4 正确姿势

```
┌───────────────────────────────────────────────────────────┐
│  规则：重写 equals ⇒ 必须重写 hashCode                    │
│  并且：参与 equals 比较的字段，必须全部参与 hashCode 计算   │
└───────────────────────────────────────────────────────────┘
```

```java
@Override
public int hashCode() {
    return Objects.hash(name);         // 和 equals 用同一组字段
}
// 或者手写（性能略好，无数组分配）：
// return 31 * name.hashCode();
```

> 实际项目里用 Lombok 的 `@Data` / `@EqualsAndHashCode` 会自动生成，但**手写实体类时最容易漏**。经典踩坑场景：ORM 实体类、DTO 作为 Map 的 key 或放进 HashSet、缓存 key。

---

## 5. 反过来问：hashCode 相同但 equals 不同，会怎样？

**合法，但会退化成链表。**

```
   BadHashKey 的 hashCode 被写成恒返回 1（比如为了"绝对不出错" 😅）

   index 1:
   ●──► [k1] ──► [k2] ──► [k3] ──► … ──► [k1000] ──► null
        每次 get 都要走完 1000 个节点比 equals
        查询 O(1) → O(n)  💀
```

这就是为什么 `hashCode()` 要写得**散列均匀**。极端的反例是 `return 1;`，功能没问题，性能爆炸。

---

## 6. 顺手复习：String 的 hashCode 长啥样

```java
public int hashCode() {
    int h = hash;                       // 缓存
    if (h == 0 && value.length > 0) {
        char val[] = value;
        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];        // 用 31 是因为编译器能把 ×31 优化成 (h<<5)-h
        }
        hash = h;
    }
    return h;
}
```

`String` 是**不可变类且重写了 equals+hashCode**，所以它可以安全地做 Map 的 key。这就是为什么"字符串当 key 总是好使"。

---

## 7. 总结卡片

```
┌─────────────────────────── put 一图流 ───────────────────────────┐
│                                                                   │
│  put(k,v)                                                         │
│     │                                                             │
│     ├─ hash(k) = hashCode ^ (h>>>16)      ← 扰动，高位折进低位      │
│     │                                                             │
│     ├─ i = (n-1) & hash                   ← 2 的幂才等价于 %       │
│     │                                                             │
│     ├─ tab[i] == null ?  → new Node 直接放                        │
│     │                                                             │
│     └─ 有冲突：                                                     │
│          ├─ 关卡1：p.hash == hash ?       ← 靠 hashCode，快        │
│          └─ 关卡2：== 或 equals ?         ← 靠 equals，慢          │
│               相同 → 覆盖 value，返回 oldValue                     │
│               不同 → 树？走树 : 尾插链表（≥8 且容量≥64 树化）        │
│                                                                   │
│  size > capacity*0.75  →  resize()（高低位拆分，不重算 hash）       │
└───────────────────────────────────────────────────────────────────┘

【一句话记忆点】
  hashCode 负责"送到哪个桶"，equals 负责"桶里哪个是你"。
  equals 相等但 hashCode 不等 = 你被送到别的桶了 = 永远找不到。
```

## 8. 自检清单 ✅

- [ ]  能默写 `hash()` 和 `(n-1) & hash` 这两行
- [ ]  能说出为什么容量必须是 2 的幂
- [ ]  能画出 put 的完整分支图（空桶/头节点/树/链表）
- [ ]  能解释 `p.hash == hash &&` 为什么放在 `equals` 前面
- [ ]  能举例说明只重写 equals 会导致 `get` 返回 null，并画出两个桶
- [ ]  知道树化需要 `链表≥8` **且** `数组≥64`
- [ ]  知道阈值是 `容量 × 0.75`

## 9. 可能被追问的题


| 追问                                  | 一句话答                                                                         |
| ------------------------------------- | -------------------------------------------------------------------------------- |
| HashMap 为什么线程不安全？            | 并发 put 可能丢数据/覆盖，JDK 8 不会再成环但依然不安全，用 ConcurrentHashMap     |
| JDK 7 和 8 的 put 有啥区别？          | 7 头插 + 重新算下标（会死循环），8 尾插 + 高低位拆分                             |
| 为什么链表长度到 8 才树化？           | 泊松分布下 8 的概率约千万分之六，树化是为了兜底极端情况；配合 6 的退化阈值防抖动 |
| null key 怎么处理？                   | hash 恒为 0，永远放 index 0                                                      |
| 为什么重写 equals 就要重写 hashCode？ | 否则 equals 相等的两个对象 hashCode 可能不等，落到不同桶，集合类全部失效         |
