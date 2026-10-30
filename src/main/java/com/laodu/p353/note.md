# ArrayList 底层机制详解

## 一、底层数据结构

ArrayList 本质上就是对 **一个可扩容的 `Object[]` 数组** 的封装：

```java
transient Object[] elementData;  // 真正存数据的数组
private int size;                // 已存元素个数（不是数组长度！）
private static final int DEFAULT_CAPACITY = 10;
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {}; // 无参构造用
private static final Object[] EMPTY_ELEMENTDATA = {};                 // 指定容量0用
protected transient int modCount;  // 结构性修改次数，fail-fast 用
```

关键区分：**`size` 是元素个数，`elementData.length` 是容量**。所有 `get/set` 都基于 `size` 做越界检查。

---

## 二、构造方法与"懒加载"


| 构造方式                      | elementData 初始值                            | 首次 add 时容量  |
| ----------------------------- | --------------------------------------------- | ---------------- |
| `new ArrayList<>()`           | `DEFAULTCAPACITY_EMPTY_ELEMENTDATA`（空数组） | **10**（懒加载） |
| `new ArrayList<>(n)`          | `new Object[n]`                               | n                |
| `new ArrayList<>(collection)` | `Arrays.copyOf(...)` 拷贝而来                 | size             |

无参构造**并不立刻创建长度 10 的数组**，而是共享一个空数组常量；第一次 add 时才真正分配到 10：

```java
// add(E e) 第一步 ensureCapacityInternal(size + 1)
private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity); // max(10, size+1)
    ensureExplicitCapacity(minCapacity);
}
private void ensureExplicitCapacity(int minCapacity) {
    modCount++;
    if (minCapacity - elementData.length > 0)  // 只有真正不够才扩容
        grow(minCapacity);
}
```

---

## 三、扩容的核心公式

### 3.1 源码

```java
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);   // ① 1.5 倍
    if (newCapacity - minCapacity < 0)                    // ② 1.5 倍还不够
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)                 // ③ 溢出保护
        newCapacity = hugeCapacity(minCapacity);
    elementData = Arrays.copyOf(elementData, newCapacity); // ④ 拷贝搬迁
}
```

### 3.2 数学表达

设：

- `C` = 当前容量（`elementData.length`）
- `N` = 当前元素个数（`size`）
- `m` = 本次操作要求的最小容量（add 时 `m = N + 1`，`addAll` 时 `m = N + 待加个数`）

**触发条件：**

$$
m > C \quad\Longrightarrow\quad \text{调用 grow}(m)
$$

**新容量公式（Java 8/11 主体逻辑）：**

$$
C' = \max\!\left(\left\lfloor \frac{3C}{2} \right\rfloor,\; m\right)
$$

其中 $\lfloor 3C/2 \rfloor = C + \lfloor C/2 \rfloor = C + (C \gg 1)$，即**右移一位等价于向下取整除以 2**，因此整体是 **1.5 倍扩容**。

**无参构造的首次扩容**（$C = 0$，但数组是共享空常量）：

$$
C' = \max(10,\; N+1) = 10
$$

**溢出保护（第三层条件）：**

$$
C' =
\begin{cases}
\text{hugeCapacity}(m), & C' > \text{MAX\_ARRAY\_SIZE} = \text{Integer.MAX\_VALUE} - 8 \\[4pt]
\quad = \begin{cases}
\text{Integer.MAX\_VALUE}, & m > \text{MAX\_ARRAY\_SIZE}\ (\text{勉强支持}) \\
\text{MAX\_ARRAY\_SIZE}, & \text{否则}
\end{cases} & \\[4pt]
\text{否则保留 } C' & 
\end{cases}
$$

注意源码里用的是 `newCapacity - minCapacity < 0`、`newCapacity - MAX_ARRAY_SIZE > 0` 这种**减法写法而非直接比较**，是为了防止 `int` 加法溢出成负数导致判断失效。

**JDK 17** 把这段收敛成了 `ArraysSupport.newLength(oldCapacity, minCapacity - oldCapacity, oldCapacity >> 1)`，语义等价：`newLength = old + max(minGrowth, prefGrowth)`，其中 `prefGrowth = old >> 1`。

### 3.3 扩容序列（默认构造）

$$
0 \xrightarrow{\text{首次}} 10 \to 15 \to 22 \to 33 \to 49 \to 73 \to 109 \to \cdots
$$

验算：$15 + \lfloor 15/2 \rfloor = 15+7 = 22$，$22 + 11 = 33$，$33 + 16 = 49$，$49+24 = 73$。因为向下取整，实际比值略小于 1.5，容量按 $\approx 1.5^{k}\cdot 10$ 指数增长。

---

## 四、拷贝成本与均摊复杂度

扩容时 `Arrays.copyOf` → 底层 `System.arraycopy`（native，内存块复制），**必须把旧数组 N 个元素全部搬过去**，单次扩容代价 $O(N)$。

总拷贝量估计：设从 $c_0$ 开始连续扩容到容量 $C_k \approx 1.5^k c_0$，则

$$
S = \sum_{i=0}^{k-1} 1.5^{i} c_0 = c_0 \cdot \frac{1.5^{k}-1}{1.5-1} = 2c_0\left(1.5^{k}-1\right) < 2 C_k
$$

即**插入 N 个元素的总拷贝量是 $O(N)$**，均摊到每次 add 为

$$
\frac{S}{N} = O(1)
$$

这正是 1.5 倍（而非固定 +N）扩容的意义：等比增长保证均摊常数，同时比 2 倍扩容更省内存（2 倍扩容的旧数组永远无法被后续复用）。


| 操作                          | 复杂度                      |
| ----------------------------- | --------------------------- |
| `get(i)` / `set(i,e)`         | $O(1)$                      |
| `add(e)` 尾部                 | 均摊$O(1)$，单次最坏 $O(N)$ |
| `add(i,e)` / `remove(i)` 中间 | $O(N)$（移动元素）          |
| `contains` / `indexOf`        | $O(N)$                      |

---

## 五、其他关键机制

**1. 删除不缩容。** `remove` 只把 `elementData[--size] = null` 并前移元素，容量永远不减。要缩容需显式 `trimToSize()`：

$$
C' = N \quad(\text{trimToSize})
$$

**2. fail-fast 迭代器。** `Itr` 每次 `next()` 检查 `modCount != expectedModCount` 就抛 `ConcurrentModificationException`。`modCount` 在 add/remove/clear 等结构性修改时自增，**`set()` 不改 modCount**。这只是"尽力检测"，不保证并发安全。

**3. 序列化。** `elementData` 是 `transient` 的，`writeObject` 只写 `size` 个有效元素，避免把扩容后的空洞也序列化。

**4. 线程不安全。** 无锁，多线程 add 会丢数据/覆盖，需要 `Collections.synchronizedList` 或 `CopyOnWriteArrayList`。

**5. 使用建议。** 已知规模时用 `new ArrayList<>(expectedSize)` 直接定容，把 $k$ 次扩容变成 0 次，避免 $\sum 1.5^i c_0$ 的拷贝开销——这是最实用的优化点。

---

## 六、一句话总结公式

$$
\boxed{\;C_{\text{new}} = \max\!\left(C + \left\lfloor \frac{C}{2} \right\rfloor,\; \min\text{Capacity}\right),\quad \text{首次} = \max(10,\; N+1)\;}
$$

即"**1.5 倍增长 + 不足则取需求值 + 上限保护**"，靠等比增长把 $O(N)$ 的拷贝摊薄成均摊 $O(1)$。
