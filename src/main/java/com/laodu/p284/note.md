# Java 自动装箱、拆箱与 Integer 缓存笔记（从 0 开始）

> 本文根据我原来的笔记和疑问整理：
>
> 1. 这篇笔记对吗？
> 2. 和 String 常量池行为上有差异吗？
> 3. 当为 `valueOf`、`new` 的时候依然也是直接从整数常量池中去取吗？

---

## 0. 先给结论

我原来的笔记里，**代码运行结果基本是对的**，但有两个关键概念错误：

1. `Integer integer = 1;` 自动装箱底层**不是** `new Integer(1)`，而是 `Integer.valueOf(1)`。
2. “Integer 类加载时创建 128 个 Integer 对象”不准确。
   更准确的说法是：`IntegerCache` 这个静态内部类初始化时，默认缓存 **-128 到 127，共 256 个** `Integer` 对象。

关于疑问：

- `Integer.valueOf(x)`：如果 `x` 在缓存范围内，从 `IntegerCache` 取；否则新建对象。
- `new Integer(x)`：**永远新建对象，永远不查缓存**。
- 自动装箱等价于 `Integer.valueOf(x)`，不是 `new Integer(x)`。
- `Integer` 缓存和 `String` 常量池**机制不同**，不能混为一谈。

---

## 1. 从基本类型和包装类说起

Java 有 8 种基本类型：


| 基本类型 | 包装类    |
| -------- | --------- |
| byte     | Byte      |
| short    | Short     |
| int      | Integer   |
| long     | Long      |
| float    | Float     |
| double   | Double    |
| char     | Character |
| boolean  | Boolean   |

为什么需要包装类？

- 泛型不能使用基本类型，例如 `Listinteger`。
- 集合类只能存对象。
- 包装类可以表示 `null`。
- 包装类提供了一些方法，例如 `Integer.parseInt()`、`Integer.valueOf()`。

基本类型和包装类之间可以互相转换：

```java
int a = 1;
Integer b = Integer.valueOf(a); // 基本类型 -> 包装类，手动装箱

Integer c = 1;                  // 自动装箱
int d = c;                      // 自动拆箱
```

---

## 2. 自动装箱和自动拆箱

### 2.1 什么是自动装箱

自动装箱：基本类型自动转成包装类。

```java
Integer integer = 1;
```

编译器会把它改写成：

```java
Integer integer = Integer.valueOf(1);
```

注意：**不是** `new Integer(1)`。

### 2.2 什么是自动拆箱

自动拆箱：包装类自动转成基本类型。

```java
Integer integer = 1;
int intValue = integer;
```

编译器会把它改写成：

```java
int intValue = integer.intValue();
```

### 2.3 用 javap 验证

```bash
javap -c AutoPackageClass
```

可以看到类似：

```text
invokestatic  java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
invokevirtual java/lang/Integer.intValue:()I
```

所以：

- 自动装箱 -> `Integer.valueOf()`
- 自动拆箱 -> `Integer.intValue()`

### 2.4 自动拆箱可能 NPE

```java
Integer integerNull = null;
System.out.println(integerNull + 1);
```

这里会报：

```text
java.lang.NullPointerException:
Cannot invoke "java.lang.Integer.intValue()" because "integerNull" is null
```

原因：`integerNull + 1` 会先把 `integerNull` 拆箱，等价于：

```java
integerNull.intValue() + 1
```

而 `integerNull` 是 `null`，调用 `intValue()` 就 NPE。

但下面不会 NPE：

```java
Integer integerNull = null;
System.out.println("integerNull = " + integerNull);
```

因为字符串拼接不会先拆箱，而是把 `null` 转成 `"null"`。

---

## 3. Integer.valueOf 和 IntegerCache

### 3.1 valueOf 源码逻辑

JDK 中 `Integer.valueOf(int i)` 的核心逻辑类似：

```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

意思是：

- 如果 `i` 在缓存范围内，直接返回缓存中的 `Integer` 对象。
- 如果不在缓存范围内，新建一个 `Integer` 对象。

### 3.2 默认缓存范围

`IntegerCache` 默认缓存范围是：

```text
-128 ~ 127
```

数量是：

```text
127 - (-128) + 1 = 256
```

所以不是 128 个，而是 **256 个** `Integer` 对象。

### 3.3 为什么下面结果是 true / false

```java
Integer integer1 = 1;
Integer integer2 = 1;
System.out.println(integer1 == integer2); // true
```

因为 `1` 在 `-128 ~ 127` 范围内，自动装箱走 `Integer.valueOf(1)`，返回的是同一个缓存对象。

```java
Integer integer3 = 1000;
Integer integer4 = 1000;
System.out.println(integer3 == integer4); // false
```

因为 `1000` 默认不在缓存范围内，`Integer.valueOf(1000)` 会新建两个不同对象，所以 `==` 比较引用地址为 `false`。

### 3.4 缓存是什么时候创建的？

我原来的笔记写：

> 在 Integer 类中，在类加载的时候会创建 128 个 Integer 对象。

这句话不够准确。

更准确的说法：

- `IntegerCache` 是 `Integer` 的静态内部类。
- `IntegerCache` 初始化时创建缓存数组。
- 第一次调用 `Integer.valueOf()` 并访问 `IntegerCache` 时，会触发 `IntegerCache` 初始化。
- 默认创建 `-128 ~ 127` 共 256 个 `Integer` 对象。

另外，“整数型常量池”这个叫法也不正式。Java 规范里没有官方术语叫“整数型常量池”。建议叫：

- `IntegerCache`
- Integer 整数缓存
- Integer 缓存池

不要和“字符串常量池”混为一谈。

### 3.5 缓存范围可以调整

默认上限是 127，但可以通过 JVM 参数调整。

HotSpot 常用：

```bash
java -XX:AutoBoxCacheMax=1000 AutoPackageClass
```

也可以设置系统属性：

```bash
-Djava.lang.Integer.IntegerCache.high=1000
```

如果调大缓存上限，那么：

```java
Integer integer3 = 1000;
Integer integer4 = 1000;
System.out.println(integer3 == integer4);
```

可能变成 `true`。

所以不要依赖 `==` 判断两个 `Integer` 是否相等。

---

## 4. new Integer 和 valueOf 的区别

这是核心问题之一。


| 写法                    | 是否走缓存 | 说明                                    |
| ----------------------- | ---------- | --------------------------------------- |
| `Integer.valueOf(1)`    | 是         | 1 在缓存范围，返回缓存对象              |
| `Integer.valueOf(1000)` | 默认否     | 1000 超出默认缓存，新建对象             |
| `new Integer(1)`        | 否         | 永远新建对象，不查缓存                  |
| `new Integer(1000)`     | 否         | 永远新建对象                            |
| `Integer i = 1;`        | 是         | 自动装箱，等价于`Integer.valueOf(1)`    |
| `Integer i = 1000;`     | 默认否     | 自动装箱，等价于`Integer.valueOf(1000)` |

示例：

```java
Integer a = 1;                  // Integer.valueOf(1)，走缓存
Integer b = Integer.valueOf(1); // 走缓存，和 a 是同一个对象
Integer c = new Integer(1);     // 新对象，不走缓存

System.out.println(a == b);      // true
System.out.println(a == c);      // false
System.out.println(a.equals(c)); // true
```

所以回答疑问：

> 当为 `valueOf`、`new` 的时候依然也是直接从整数常量池中去取吗？

- `Integer.valueOf(x)`：在缓存范围内，从 `IntegerCache` 取；否则新建。
- `new Integer(x)`：永远不查缓存，永远新建。
- 自动装箱：等价于 `Integer.valueOf(x)`。

---

## 5. 我原来的笔记逐条修正

原代码：

```java
// 自动装箱,实际底层做的是Integer integer = new Integer(1);
Integer integer = 1;
```

修正：

```java
// 自动装箱，底层实际是 Integer integer = Integer.valueOf(1);
Integer integer = 1;
```

原代码：

```java
// 自动拆箱,实际底层做的是int int_value = integer.intValue();
int int_value = integer;
```

修正：这个是对的。

```java
// 自动拆箱，底层是 int int_value = integer.intValue();
int int_value = integer;
```

原代码：

```java
Integer Integer_null = null;
System.out.println(Integer_null + 1);
```

修正：会 NPE，原因是对的。变量名不建议大写开头。

```java
Integer integerNull = null;
// System.out.println(integerNull + 1); // NPE: integerNull.intValue()
```

原注释：

```java
// 在Integer类中,在类加载的时候会创建128个Integer对象,缓存起来,范围是-128~127
```

修正：

```java
// IntegerCache 初始化时，默认创建 -128~127 共 256 个 Integer 对象。
// 自动装箱/Integer.valueOf 在缓存范围内返回缓存对象，否则新建对象。
// new Integer(x) 永远新建对象，不走缓存。
```

---

## 6. 和 String 常量池的差异

### 6.1 String 常量池示例

```java
String s1 = "abc";
String s2 = "abc";
System.out.println(s1 == s2); // true

String s3 = new String("abc");
System.out.println(s1 == s3); // false

String s4 = s3.intern();
System.out.println(s1 == s4); // true
```

说明：

- `"abc"` 字面量会进入字符串常量池。
- 相同字面量会复用同一个字符串对象。
- `new String("abc")` 会在堆中新建对象。
- `intern()` 可以返回字符串常量池中的引用。

### 6.2 Integer 和 String 对比


| 维度             | Integer                   | String                                        |
| ---------------- | ------------------------- | --------------------------------------------- |
| 机制             | `IntegerCache` 类库缓存   | 字符串常量池，JVM 层面机制                    |
| 范围             | 默认 -128~127，上限可调   | 字符串字面量、常量表达式、`intern()` 的字符串 |
| 是否所有值都缓存 | 不是，只有小范围整数      | 不是所有字符串都自动入池，但字面量会          |
| `new` 行为       | `new Integer(x)` 永远新建 | `new String("x")` 永远在堆中新建              |
| 手动入池         | 没有`Integer.intern()`    | 有`String.intern()`                           |
| `==`             | 比较引用地址              | 比较引用地址                                  |
| `equals`         | 比较 int 值               | 比较字符串内容                                |

### 6.3 核心区别

`Integer` 的缓存：

- 是 Java 类库自己实现的。
- 默认只缓存 `-128 ~ 127`。
- 不能像 String 一样把任意整数动态加入缓存。
- 没有 `intern()` 方法。

`String` 的常量池：

- 是 JVM 层面的机制。
- 字符串字面量会自动进入常量池。
- 可以通过 `intern()` 手动入池。
- 常量池中的字符串可以被复用。

所以：

> Integer 缓存和 String 常量池行为有相似之处，但本质不同。

相似点：

- 都可能导致 `==` 为 `true`。
- `new` 出来的对象都不走复用。

不同点：

- 机制不同。
- 范围不同。
- 是否可手动入池不同。
- 一个是类库缓存，一个是 JVM 字符串常量池。

---

## 7. 修正后的完整代码

```java
package com.laodu.p284;

public class AutoPackageClass {
    public static void main(String[] args) {

        // 自动装箱：编译后等价于 Integer integer = Integer.valueOf(1);
        Integer integer = 1;

        // 自动拆箱：编译后等价于 int intValue = integer.intValue();
        int intValue = integer;
        System.out.println("intValue: " + intValue);

        // 包装类为 null 时，参与数值运算会自动拆箱，导致 NPE
        Integer integerNull = null;
        // System.out.println(integerNull + 1); // NPE: integerNull.intValue()
        System.out.println("integerNull = " + integerNull); // 不拆箱，输出 null

        // IntegerCache 默认缓存 -128~127，共 256 个 Integer 对象
        Integer integer1 = 1;
        Integer integer2 = 1;
        System.out.println("integer1 == integer2: " + (integer1 == integer2)); // true

        Integer integer3 = 1000;
        Integer integer4 = 1000;
        System.out.println("integer3 == integer4: " + (integer3 == integer4)); // false

        // valueOf 在缓存范围内返回缓存对象，否则新建
        Integer integer5 = Integer.valueOf(1);
        System.out.println("integer1 == integer5: " + (integer1 == integer5)); // true

        // new Integer 永远新建对象，不走缓存。构造器已废弃，不推荐使用。
        Integer integer6 = new Integer(1);
        System.out.println("integer1 == integer6: " + (integer1 == integer6)); // false
        System.out.println("integer1.equals(integer6): " + integer1.equals(integer6)); // true
    }
}
```

---

## 8. 其他包装类的缓存

不只是 `Integer` 有缓存，其他包装类也有类似设计。


| 包装类    | 缓存范围             |
| --------- | -------------------- |
| Byte      | -128 ~ 127，全部缓存 |
| Short     | -128 ~ 127           |
| Integer   | -128 ~ 127，上限可调 |
| Long      | -128 ~ 127           |
| Character | 0 ~ 127              |
| Boolean   | TRUE / FALSE         |
| Float     | 没有缓存             |
| Double    | 没有缓存             |

所以：

```java
Long a = 1L;
Long b = 1L;
System.out.println(a == b); // true

Long c = 1000L;
Long d = 1000L;
System.out.println(c == d); // false
```

---

## 9. 常见易错点

### 9.1 用 == 比较两个包装类

```java
Integer a = 1000;
Integer b = 1000;
System.out.println(a == b); // false
```

正确做法：

```java
System.out.println(a.equals(b)); // true
```

或者先拆箱：

```java
System.out.println(a.intValue() == b.intValue()); // true
```

### 9.2 包装类和基本类型比较

```java
Integer a = 1000;
int b = 1000;
System.out.println(a == b); // true
```

因为一边是基本类型，`a` 会自动拆箱，变成数值比较。

### 9.3 null 拆箱

```java
Integer a = null;
// System.out.println(a + 1); // NPE
```

### 9.4 字符串拼接不会拆箱

```java
Integer a = null;
System.out.println("a = " + a); // a = null
```

### 9.5 不要依赖缓存范围

```java
Integer a = 1000;
Integer b = 1000;
System.out.println(a == b); // 默认 false
```

但如果 JVM 参数调整了缓存上限，可能变成 `true`。
所以永远不要用 `==` 比较两个包装类的值。

---

## 10. 总结

1. 自动装箱底层是 `Integer.valueOf(x)`，不是 `new Integer(x)`。
2. 自动拆箱底层是 `Integer.intValue()`。
3. 包装类为 `null` 时自动拆箱会 NPE。
4. `IntegerCache` 默认缓存 `-128 ~ 127`，共 256 个 `Integer` 对象。
5. 缓存创建更准确地说是在 `IntegerCache` 初始化时，不是简单说“Integer 类加载时”。
6. `Integer.valueOf(x)`：缓存范围内取缓存，否则新建。
7. `new Integer(x)`：永远新建，不走缓存。
8. `Integer` 缓存和 `String` 常量池机制不同：
   - `Integer` 是类库缓存。
   - `String` 是 JVM 字符串常量池。
   - `String` 有 `intern()`，`Integer` 没有。
9. 比较包装类的值，用 `equals()`，不要用 `==`。
10. “整数型常量池”这个叫法不正式，建议叫 `IntegerCache` 或 Integer 整数缓存。

---

## 11. 我的疑问集中回答

### 疑问 1：这篇笔记对吗？

大部分现象是对的，但有两个关键错误：

- 自动装箱底层不是 `new Integer(1)`，而是 `Integer.valueOf(1)`。
- 不是缓存 128 个，而是默认缓存 -128 到 127，共 256 个。

### 疑问 2：和 String 常量池行为上有差异吗？

有差异。

- `Integer` 用的是 `IntegerCache`，是 Java 类库缓存。
- `String` 用的是字符串常量池，是 JVM 层面机制。
- `String` 有 `intern()`，`Integer` 没有。
- `Integer` 默认只缓存小范围整数，`String` 字面量可以进入常量池。
- `new String` 和 `new Integer` 都不走复用。

### 疑问 3：当为 valueOf、new 的时候依然也是直接从整数常量池中去取吗？

- `Integer.valueOf(x)`：在缓存范围内，从 `IntegerCache` 取；否则新建。
- `new Integer(x)`：永远不查缓存，永远新建。
- 自动装箱：等价于 `Integer.valueOf(x)`，不是 `new Integer(x)`。

所以：

```java
Integer a = 1;                  // 走缓存
Integer b = Integer.valueOf(1); // 走缓存
Integer c = new Integer(1);     // 不走缓存，新对象

System.out.println(a == b); // true
System.out.println(a == c); // false
System.out.println(a.equals(c)); // true
```
