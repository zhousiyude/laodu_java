# Java 泛型笔记：类级泛型 vs 方法级泛型

> 核心结论：**类级泛型参数只属于实例，静态成员无法使用；静态方法要用泛型，必须在方法上自己声明。**

---

## 一、类级泛型：只服务于"实例"

### 1.1 什么是类级泛型

```java
class Boxt {          // T 声明在类上
    private T value;    // ✅ 实例字段可用
    public Box(T v) {   // ✅ 构造方法可用
        this.value = v;
    }
    public T get() {    // ✅ 实例方法可用
        return value;
    }
}
```

### 1.2 静态成员不能用类级泛型

```java
class Boxt {
    static T staticValue;      // ❌ 编译错误
    static T getStatic() {     // ❌ 编译错误
        return null;
    }
}
```

**为什么不行？**

因为 `T` 是**每个实例各自确定**的：

```java
Boxstring  a = new Box<>("hi");   // 这个实例的 T = String
Boxinteger b = new Box<>(1);      // 这个实例的 T = Integer
```

而静态成员是**整个类共享一份**的，JVM 加载类时就存在了，此时根本不知道 `T` 到底是 `String` 还是 `Integer`，所以编译器直接禁止。

> 换句话说：**静态成员没有"实例上下文"，而类级泛型参数恰恰依赖实例上下文才能确定。**

---

## 二、"指定泛型"到底在哪指定？

常见说法"通过构造方法指定"不够准确，实际是在 **`new` 的类名处**指定或推断：

```java
// 方式一：钻石运算符，由左边变量声明推断
Boxstring b1 = new Box<>("hi");

// 方式二：显式指定（老写法，仍有效）
Boxinteger b2 = new Boxinteger(1);
```

注意：**构造方法本身也可以声明自己的类型参数**，与类的 `T` 无关：

```java
class Boxt {
    private T value;

    // U 是构造方法自己的类型参数
    public 
<u> Box(U u, T t) {
        this.value = t;
    }
}
```

所以更严谨的说法是：

> **泛型实参是在 `new` 表达式处（或变量声明处）指定的，不是在构造方法签名处。**

---

## 三、方法级泛型：静态方法用泛型的唯一正路

### 3.1 静态方法必须自己声明类型参数

```java
class Utils {
    // static T identity(T t) { }        ❌ 类上没有 T 可用

    static <t> T identity(T t) {          // ✅ 方法自己声明 <t>
        return t;
    }

    static <t extends="" comparable<t="">> T max(T a, T b) {  // ✅ 带边界
        return a.compareTo(b) >= 0 ? a : b;
    }
}
```

### 3.2 调用时类型如何确定

```java
String s = Utils.identity("hello");        // 由实参推断 T = String
Integer i = Utils.<integer>identity(42);   // 显式指定（少见）
```

### 3.3 关键：静态方法里看不到类的 T

```java
class Foo<t> {
    static void bar() {
        // T x;   ❌ 编译错误：这里的 T 不存在
    }
}
```

即使类声明了 `<t>`，静态方法体内引用 `T` 照样报错——**静态方法没有实例，拿不到类级 T**。

---

## 四、对比总结表


| 场景     | 能否使用泛型         | 说明                 |
| -------- | -------------------- | -------------------- |
| 实例字段 | ✅ 可用类级 T        | 属于某个具体实例     |
| 构造方法 | ✅ 可用类级 T        | 构造时有实例上下文   |
| 实例方法 | ✅ 可用类级 T        | 调用时有实例上下文   |
| 静态字段 | ❌ 不能用类级 T      | 全类共享，无法确定 T |
| 静态方法 | ❌ 不能用类级 T      | 同上                 |
| 静态方法 | ✅ 可声明方法级`<t>` | 由实参/显式指定推断  |
| 构造方法 | ✅ 可声明方法级`<u>` | 与类级 T 独立        |

---

## 五、背后原理：为什么类级泛型"属于实例"

Java 泛型是**编译期类型检查 + 运行时擦除**：

1. **擦除**：`Box<string>` 和 `Box<integer>` 运行时都是 `Box`，`T` 被擦成 `Object`（或边界类型）。
2. **类型信息只存在于编译期**，由编译器在**每次使用点**（`new Box<string>`、变量声明处）做检查。
3. **静态成员在类加载时就初始化**，早于任何实例创建，此时没有"某次使用点"的上下文，编译器无从得知 `T`，所以禁止。

> 一句话：**类级 T 的"值"是随实例走的，静态成员不随实例走，所以对不上。**

---

## 六、实践建议

### ✅ 推荐做法

```java
// 1. 工具类静态方法 → 方法级泛型
public final class CollectionUtils {
    public static <t> List<t> of(T... items) {
        return Arrays.asList(items);
    }
}

// 2. 需要持有类型 → 泛型类 + 实例方法
public class Holder<t> {
    private final T value;
    public Holder(T v) { this.value = v; }
    public T get() { return value; }
}
```

### ❌ 避免的做法

```java
// 想用类级 T 写静态方法——做不到，别硬凑
class Bad<t> {
    static T get() { }   // 编译不过
}

// 用原始类型绕过——丢失类型安全，不推荐
class Bad2<t> {
    static Object get() { return null; }  // 能用但没类型检查
}
```

---

## 七、一句话记忆

> **类级泛型 = 实例的私有参数，静态成员碰不到；**
> **方法级泛型 = 方法自己的参数，静态实例都能用；**
> **`new` 时指定（或推断）泛型实参，不是构造方法指定。**

---

## 八、自测题

1. `class A<t> { static T f() {} }` 能编译吗？为什么？
2. `class A<t> { static <u> U f(U u) { return u; } }` 能编译吗？
3. `A<string> a = new A<>();` 中 `<string>` 是在哪里"传"给 A 的？
4. 静态方法里能写 `T x;`（类声明了 `<t>`）吗？

**答案**：

1. 不能，静态成员无实例上下文，无法确定 T。
2. 能，`<u>` 是方法自己声明的。
3. 在 `new` 表达式的类名处（钻石运算符推断）。
4. 不能，静态方法体内类级 `T` 不可见。

---

*笔记完 · 建议配合 IDE 实际敲一遍编译错误，印象更深*</u></t></string></string></u></t></t></t></t></t></t></t></string></integer></string></u></t></t></t></integer></t></t></t></u>
