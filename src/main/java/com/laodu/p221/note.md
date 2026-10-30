下面默认已导入：

```java
import java.util.*;
import java.util.stream.*;
```

## 1. 转换 / 展示

### `Arrays.toString(array)`

**参数**：一维数组。
**返回**：数组内容的字符串。
**输入输出示例**：

```java
int[] a = {1, 2, 3};
System.out.println(Arrays.toString(a));
// 输出：[1, 2, 3]
```

### `Arrays.deepToString(Object[])`

**参数**：多维数组或嵌套对象数组。
**返回**：深层内容的字符串。
**输入输出示例**：

```java
int[][] a = {{1, 2}, {3, 4}};
System.out.println(Arrays.deepToString(a));
// 输出：[[1, 2], [3, 4]]
```

### `Arrays.asList(T... a)`

**参数**：可变参数或对象数组。
**返回**：固定大小的 `List`，不能 `add/remove`，但可以 `set`。
**输入输出示例**：

```java
Liststring list = Arrays.asList("a", "b");
System.out.println(list);
// 输出：[a, b]

list.set(0, "x");
System.out.println(list);
// 输出：[x, b]

// list.add("c"); // 抛 UnsupportedOperationException
```

注意：`Arrays.asList(new int[]{1,2,3})` 得到的是 `Listint[]`，不是 `Listinteger`。

### `Arrays.stream(array)`

**参数**：数组，或 `array, start, end`。
**返回**：`Stream` / `IntStream` / `LongStream` / `DoubleStream`。
**输入输出示例**：

```java
int[] a = {1, 2, 3};
System.out.println(Arrays.stream(a).sum());
// 输出：6

int[] b = Arrays.stream(a).map(x -> x * 2).toArray();
System.out.println(Arrays.toString(b));
// 输出：[2, 4, 6]

int[] c = Arrays.stream(new int[]{1, 2, 3, 4}, 1, 3).toArray();
System.out.println(Arrays.toString(c));
// 输出：[2, 3]
```

### `Arrays.spliterator(array)`

**参数**：数组，或 `array, start, end`。
**返回**：`Spliterator`，用于遍历或并行处理。
**输入输出示例**：

```java
Spliterator.OfInt sp = Arrays.spliterator(new int[]{1, 2, 3});
sp.forEachRemaining((int x) -> System.out.print(x + " "));
// 输出：1 2 3
```

---

## 2. 排序

### `Arrays.sort(array)`

**参数**：数组。
**返回**：`void`，原地排序，默认升序。
**输入输出示例**：

```java
int[] a = {3, 1, 2};
Arrays.sort(a);
System.out.println(Arrays.toString(a));
// 输出：[1, 2, 3]
```

### `Arrays.sort(array, from, to)`

**参数**：`fromIndex` 包含，`toIndex` 不包含。
**输入输出示例**：

```java
int[] b = {5, 3, 1, 4, 2};
Arrays.sort(b, 1, 4);
System.out.println(Arrays.toString(b));
// 输出：[5, 1, 3, 4, 2]
```

### `Arrays.sort(T[], Comparator)`

**参数**：对象数组 + 比较器。
**输入输出示例**：

```java
String[] s = {"banana", "apple", "cherry"};
Arrays.sort(s, Comparator.reverseOrder());
System.out.println(Arrays.toString(s));
// 输出：[cherry, banana, apple]
```

### `Arrays.parallelSort(array)`

**参数**：数组。
**返回**：`void`，并行排序，适合大数组。
**输入输出示例**：

```java
int[] c = {5, 2, 8, 1};
Arrays.parallelSort(c);
System.out.println(Arrays.toString(c));
// 输出：[1, 2, 5, 8]
```

### `Arrays.parallelPrefix(array, op)`

**参数**：数组 + 二元操作符。
**作用**：原地做累积运算。
**输入输出示例**：

```java
int[] d = {1, 2, 3, 4};
Arrays.parallelPrefix(d, (x, y) -> x + y);
System.out.println(Arrays.toString(d));
// 输出：[1, 3, 6, 10]
```

---

## 3. 查找

### `Arrays.binarySearch(array, key)`

**参数**：已排序数组 + 要查找的值。
**返回**：找到返回索引；未找到返回 `-(插入点) - 1`。
**输入输出示例**：

```java
int[] a = {1, 3, 5, 7};

System.out.println(Arrays.binarySearch(a, 5));
// 输出：2

System.out.println(Arrays.binarySearch(a, 4));
// 输出：-3，因为 4 应插入到索引 2 的位置
```

### `Arrays.binarySearch(array, from, to, key)`

**参数**：`fromIndex` 包含，`toIndex` 不包含。
**输入输出示例**：

```java
int[] a = {1, 3, 5, 7};
System.out.println(Arrays.binarySearch(a, 1, 4, 5));
// 输出：2
```

---

## 4. 比较 / 哈希

### `Arrays.equals(a, b)`

**参数**：两个一维数组。
**返回**：内容是否相等。
**输入输出示例**：

```java
int[] a = {1, 2};
int[] b = {1, 2};
System.out.println(Arrays.equals(a, b));
// 输出：true
```

### `Arrays.deepEquals(a, b)`

**参数**：两个多维 / 嵌套数组。
**返回**：深层内容是否相等。
**输入输出示例**：

```java
int[][] c = {{1, 2}};
int[][] d = {{1, 2}};

System.out.println(Arrays.equals(c, d));
// 输出：false，因为比较的是内部数组引用

System.out.println(Arrays.deepEquals(c, d));
// 输出：true
```

### `Arrays.compare(a, b)` Java 9+

**参数**：两个数组。
**返回**：字典序比较结果，负数、0、正数。
**输入输出示例**：

```java
System.out.println(Arrays.compare(new int[]{1, 2}, new int[]{1, 3}));
// 输出：-1

System.out.println(Arrays.compare(new int[]{1, 2}, new int[]{1, 2}));
// 输出：0
```

### `Arrays.mismatch(a, b)` Java 9+

**参数**：两个数组。
**返回**：第一个不相等元素的索引；完全相同返回 `-1`。
**输入输出示例**：

```java
System.out.println(Arrays.mismatch(new int[]{1, 2, 3}, new int[]{1, 9, 3}));
// 输出：1

System.out.println(Arrays.mismatch(new int[]{1, 2}, new int[]{1, 2}));
// 输出：-1
```

### `Arrays.hashCode(array)`

**参数**：一维数组。
**返回**：基于内容的哈希值。
**输入输出示例**：

```java
System.out.println(Arrays.hashCode(new int[]{1, 2, 3}));
// 输出：30817
```

### `Arrays.deepHashCode(Object[])`

**参数**：多维 / 嵌套数组。
**返回**：深层内容的哈希值。
**输入输出示例**：

```java
System.out.println(Arrays.deepHashCode(new int[][]{{1, 2}, {3, 4}}));
// 输出：一个整数哈希值
```

---

## 5. 填充 / 生成

### `Arrays.fill(array, val)`

**参数**：数组 + 填充值。
**返回**：`void`，原地填充。
**输入输出示例**：

```java
int[] a = new int[5];
Arrays.fill(a, 7);
System.out.println(Arrays.toString(a));
// 输出：[7, 7, 7, 7, 7]
```

### `Arrays.fill(array, from, to, val)`

**参数**：`fromIndex` 包含，`toIndex` 不包含。
**输入输出示例**：

```java
int[] b = new int[5];
Arrays.fill(b, 1, 3, 9);
System.out.println(Arrays.toString(b));
// 输出：[0, 9, 9, 0, 0]
```

### `Arrays.setAll(array, generator)`

**参数**：数组 + 生成器，通常用索引生成值。
**输入输出示例**：

```java
int[] c = new int[5];
Arrays.setAll(c, i -> i * i);
System.out.println(Arrays.toString(c));
// 输出：[0, 1, 4, 9, 16]
```

### `Arrays.parallelSetAll(array, generator)`

**参数**：数组 + 生成器。
**作用**：并行版 `setAll`。
**输入输出示例**：

```java
int[] d = new int[5];
Arrays.parallelSetAll(d, i -> i + 1);
System.out.println(Arrays.toString(d));
// 输出：[1, 2, 3, 4, 5]
```

---

## 6. 复制 / 截取

### `Arrays.copyOf(array, newLength)`

**参数**：原数组 + 新数组长度。
**返回**：新数组。长度不够补默认值，超出则截断。
**输入输出示例**：

```java
int[] a = {1, 2, 3};

int[] b = Arrays.copyOf(a, 5);
System.out.println(Arrays.toString(b));
// 输出：[1, 2, 3, 0, 0]

int[] c = Arrays.copyOf(a, 2);
System.out.println(Arrays.toString(c));
// 输出：[1, 2]
```

### `Arrays.copyOfRange(array, from, to)`

**参数**：`fromIndex` 包含，`toIndex` 不包含。
**返回**：截取出的新数组。
**输入输出示例**：

```java
int[] a = {1, 2, 3};

int[] d = Arrays.copyOfRange(a, 1, 3);
System.out.println(Arrays.toString(d));
// 输出：[2, 3]

int[] e = Arrays.copyOfRange(a, 1, 5);
System.out.println(Arrays.toString(e));
// 输出：[2, 3, 0, 0]
```

---

## 7. 流 / 迭代

### `Arrays.stream(array)`

**参数**：数组。
**返回**：流。
**输入输出示例**：

```java
int sum = Arrays.stream(new int[]{1, 2, 3}).sum();
System.out.println(sum);
// 输出：6
```

### `Arrays.stream(array, start, end)`

**参数**：`start` 包含，`end` 不包含。
**输入输出示例**：

```java
int[] r = Arrays.stream(new int[]{1, 2, 3, 4}, 1, 3).toArray();
System.out.println(Arrays.toString(r));
// 输出：[2, 3]
```

### `Arrays.spliterator(array)`

**参数**：数组。
**返回**：`Spliterator`。
**输入输出示例**：

```java
Spliterator.OfInt sp = Arrays.spliterator(new int[]{1, 2, 3});
sp.forEachRemaining((int x) -> System.out.print(x + " "));
// 输出：1 2 3
```

---

## 参数规律总结

1. **范围参数**：`fromIndex` 一般包含，`toIndex` 一般不包含。
   例如：`sort(arr, 1, 4)` 只排序索引 `1、2、3`。
2. **`binarySearch` 未找到**：返回 `-(插入点) - 1`。
   例如数组 `[1,3,5,7]` 查找 `4`，插入点是 `2`，返回 `-3`。
3. **`copyOf` 扩容**：不足部分补默认值。
   `int` 补 `0`，`boolean` 补 `false`，对象补 `null`。
4. **`asList` 返回固定大小 List**：可以 `set`，不能 `add/remove`。
5. **多维数组**：

   - 输出用 `deepToString`
   - 比较用 `deepEquals`
   - 哈希用 `deepHashCode`
6. **`compare`、`mismatch`**：Java 9+ 才有。
