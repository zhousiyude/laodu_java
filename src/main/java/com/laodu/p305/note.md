# StringBuilder 自动扩容机制笔记（JDK 8 经典版与现代版对比）

## 一、 核心结论（一句话总结）

`StringBuilder` 的扩容本质是：**取“翻倍+2”与“实际所需最小容量”两者中的最大值。**

数学表达式：
\[
C_{new} = \max(C_{min}, \ 2 \times C_{old} + 2)
\]

> * \(C_{old}\)：旧容量
> * \(C_{min}\)：本次追加后需要的**最小总容量**（当前长度 + 追加长度）
> * \(C_{new}\)：最终计算出的**新容量**

---

## 二、 触发时机

在 `append()` 等修改操作中，每次都会调用 `ensureCapacityInternal(count + len)` 检查容量。

* `count`：当前已使用长度
* `len`：本次追加的长度
* **触发条件**：`count + len > 当前容量`（即所需最小容量 > 旧容量）

---

## 三、 JDK 8 经典源码剖析

在 JDK 8 中，`StringBuilder` 继承自 `AbstractStringBuilder`，底层使用 `char[] value` 存储字符。

### 1. 入口方法：`append(String str)`

```java
public AbstractStringBuilder append(String str) {
    if (str == null) return appendNull();
    int len = str.length();
    ensureCapacityInternal(count + len);  // 检查容量
    str.getChars(0, len, value, count);   // 拷贝字符
    count += len;
    return this;
}
```

### 2. 容量检查：`ensureCapacityInternal(int minimumCapacity)`

```java
private void ensureCapacityInternal(int minimumCapacity) {
    // overflow-conscious code
    if (minimumCapacity - value.length > 0) { // 所需最小容量 > 当前数组长度
        expandCapacity(minimumCapacity);      // 触发扩容
    }
}
```

### 3. 核心扩容：`expandCapacity(int minimumCapacity)`

这是 JDK 8 最经典的扩容实现，逻辑非常直白：

```java
void expandCapacity(int minimumCapacity) {
    // 1. 默认扩容：旧容量 × 2 + 2
    int newCapacity = value.length * 2 + 2;
  
    // 2. 不足修正：如果翻倍后仍小于所需最小容量，直接取最小容量
    if (newCapacity - minimumCapacity < 0)
        newCapacity = minimumCapacity;
    
    // 3. 溢出保护：如果新容量计算溢出（变成负数）
    if (newCapacity < 0) {
        if (minimumCapacity < 0) // 所需容量也溢出，说明内存不够了
            throw new OutOfMemoryError();
        newCapacity = Integer.MAX_VALUE; // 直接赋值为最大整数
    }
  
    // 4. 执行拷贝：创建新数组并复制原数据
    value = Arrays.copyOf(value, newCapacity);
}
```

**JDK 8 扩容步骤总结：**

1. 先按 `旧容量 × 2 + 2` 计算。
2. 如果不够，直接取 `minimumCapacity`。
3. 如果溢出，尝试取 `Integer.MAX_VALUE`，若还不行则抛 `OutOfMemoryError`。
4. 调用 `Arrays.copyOf` 进行数组拷贝。

---

## 四、 现代版本（JDK 11+ / 17 / 21）源码剖析

从 JDK 9 开始，为了节省内存，引入了**紧凑字符串（Compact Strings）**，底层 `char[]` 变成了 `byte[]` + `byte coder`（Latin1 或 UTF16）。因此，容量计算变为 `value.length >> coder`。扩容逻辑也被重构并抽象到了 `ArraysSupport.newLength` 方法中。

### 1. 容量检查：`ensureCapacityInternal`

```java
private void ensureCapacityInternal(int minimumCapacity) {
    // overflow-conscious code
    int oldCapacity = value.length >> coder;
    if (minimumCapacity - oldCapacity > 0) {
        value = Arrays.copyOf(value,
                newCapacity(minimumCapacity) << coder);
    }
}
```

### 2. 核心扩容：`newCapacity` 与 `hugeCapacity`

```java
private int newCapacity(int minCapacity) {
    int oldCapacity = value.length >> coder;
    // 1. 默认扩容：旧容量 × 2 + 2
    int newCapacity = (oldCapacity << 1) + 2;
  
    // 2. 不足修正
    if (newCapacity - minCapacity < 0) {
        newCapacity = minCapacity;
    }
  
    // 3. 软上限判断（SOFT_MAX_ARRAY_LENGTH 通常为 Integer.MAX_VALUE - 8）
    int SAFE_BOUND = MAX_ARRAY_SIZE >> coder;
    return (newCapacity <= 0 || SAFE_BOUND - newCapacity < 0)
            ? hugeCapacity(minCapacity) : newCapacity;
}

private int hugeCapacity(int minCapacity) {
    int SAFE_BOUND = MAX_ARRAY_SIZE >> coder;
    int UNSAFE_BOUND = Integer.MAX_VALUE >> coder;
  
    if (UNSAFE_BOUND - minCapacity < 0) // 超出极限
        throw new OutOfMemoryError();
    
    return (minCapacity > SAFE_BOUND) ? minCapacity : SAFE_BOUND;
}
```

### 3. 精炼写法（JDK 17+ 的 `ArraysSupport.newLength`）

现代版本将逻辑进一步抽象，用一行代码完成了“翻倍”和“不足修正”：

```java
// 位于 java.util.ArraysSupport
public static int newLength(int oldLength, int minGrowth, int prefGrowth) {
    // 核心公式：旧容量 + max(最小增量, 首选增量)
    int prefLength = oldLength + Math.max(minGrowth, prefGrowth); // might overflow
    if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
        return prefLength;
    } else {
        // put code cold in a separate method
        return hugeLength(oldLength, minGrowth);
    }
}
```

* `minGrowth` = 所需最小容量 - 旧容量
* `prefGrowth` = 旧容量 + 2
* 代入公式推导，完美等价于 \(\max(C_{min}, 2C_{old} + 2)\)。

---

## 五、 JDK 8 与现代版核心差异对比


| 对比维度         | JDK 8                                        | JDK 9+ (现代版)                                                 |
| :--------------- | :------------------------------------------- | :-------------------------------------------------------------- |
| **底层存储**     | `char[] value`                               | `byte[] value` + `byte coder`                                   |
| **容量获取**     | `value.length`                               | `value.length >> coder`                                         |
| **扩容逻辑位置** | `AbstractStringBuilder.expandCapacity`       | `AbstractStringBuilder.newCapacity` + `ArraysSupport.newLength` |
| **扩容公式**     | `旧容量 * 2 + 2`，不足则取 `minimumCapacity` | `oldLength + Math.max(minGrowth, prefGrowth)`                   |
| **溢出与上限**   | 直接判断是否小于 0，取`Integer.MAX_VALUE`    | 引入`SOFT_MAX_ARRAY_LENGTH`，复杂逻辑交由 `hugeLength` 方法     |
| **编码支持**     | 仅 UTF-16                                    | 支持 Latin1 和 UTF-16，根据`coder` 决定扩容上限                 |

---

## 六、 完整扩容流程总结

1. **追加前检查**：计算 `count + len` 是否大于当前容量。
2. **计算新容量**：
   * 默认尝试 `旧容量 × 2 + 2`。
   * 若不够，直接取 `所需最小容量`。
3. **边界与溢出保护**：
   * 判断是否溢出（变负数）。
   * 判断是否超过软上限（`SOFT_MAX_ARRAY_LENGTH`）。
   * 若超过，进入 `hugeCapacity` 尝试分配极限容量或抛出 `OutOfMemoryError`。
4. **数据拷贝**：调用 `Arrays.copyOf` 创建新数组并复制旧数据。
5. **更新引用**：将底层数组指向新数组。

---

## 七、 性能优化建议

* **扩容代价**：扩容本质是 `Arrays.copyOf`，即创建新数组 + 拷贝旧数据，时间复杂度为 O(n)。
* **最佳实践**：**已知最终字符串长度时，务必预设初始容量**。
  ```java
  // 推荐
  StringBuilder sb = new StringBuilder(1024);
  // 不推荐：默认 16，大概率会多次扩容
  StringBuilder sb = new StringBuilder();
  ```

---

## 八、 附：与其他集合扩容对比


| 集合类            | 默认初始容量 | 扩容公式                                 | 特点                     |
| :---------------- | :----------- | :--------------------------------------- | :----------------------- |
| **StringBuilder** | 16           | \(\max(C_{min}, 2C_{old} + 2)\)          | 激进翻倍，减少扩容次数   |
| **ArrayList**     | 10           | \(C_{old} + (C_{old} >> 1)\) (即 1.5 倍) | 折中策略，平衡空间与时间 |
| **HashMap**       | 16           | 2 倍（且必须为 2 的幂）                  | 保证哈希寻址效率         |

---
