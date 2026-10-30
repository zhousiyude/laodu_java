# Java 枚举实现接口的两种方式

> 枚举 `enum` 不能继承类，但可以实现接口。实现接口后，枚举常量可以当作接口类型使用，从而获得多态能力。

---

## 方式一：枚举类统一实现接口

所有枚举常量共用同一个接口方法实现，通常通过 `name()`、字段或 `switch` 区分不同常量的行为。

### 示例

```java
interface Describable {
    String describe();
}

enum Season implements Describable {
    SPRING("春"),
    SUMMER("夏"),
    AUTUMN("秋"),
    WINTER("冬");

    private final String cnName;

    Season(String cnName) {
        this.cnName = cnName;
    }

    @Override
    public String describe() {
        return name() + "(" + cnName + ")";
    }
}
```

### 使用

```java
public class Demo {
    public static void main(String[] args) {
        Describable d = Season.SPRING;
        System.out.println(d.describe()); // SPRING(春)
    }
}
```

### 特点

- 接口方法只在枚举类中实现一次。
- 所有枚举常量共享这段逻辑。
- 如果需要区分行为，通常要写 `if`、`switch` 或读取枚举字段。
- 适合：各常量行为差异不大、逻辑集中的场景。

---

## 方式二：每个枚举常量单独实现接口

在枚举常量后面加 `{ ... }`，形成“常量特定类体”，让每个常量自己实现接口方法。

### 示例

```java
interface Calculator {
    int calculate(int a, int b);
}

enum Operation implements Calculator {
    ADD {
        @Override
        public int calculate(int a, int b) {
            return a + b;
        }
    },
    SUBTRACT {
        @Override
        public int calculate(int a, int b) {
            return a - b;
        }
    },
    MULTIPLY {
        @Override
        public int calculate(int a, int b) {
            return a * b;
        }
    };
}
```

### 使用

```java
public class Demo {
    public static void main(String[] args) {
        Calculator c = Operation.ADD;
        System.out.println(c.calculate(1, 2)); // 3

        System.out.println(Operation.MULTIPLY.calculate(3, 4)); // 12
    }
}
```

### 特点

- 每个枚举常量相当于一个子类，分别提供自己的实现。
- 天然多态，不需要大量 `switch`。
- 方法实现必须使用 `public`，因为接口方法隐式是 `public`。
- 最后一个枚举常量后要加分号 `;`。
- 适合：各常量行为差异大、希望消除条件判断的场景。

---

## 两种方式对比


| 对比项   | 方式一：枚举类统一实现 | 方式二：每个常量单独实现 |
| -------- | ---------------------- | ------------------------ |
| 实现位置 | 枚举类中               | 每个枚举常量的类体中     |
| 代码复用 | 高                     | 低                       |
| 多态性   | 较弱，常需判断         | 强，直接多态调用         |
| 可读性   | 集中，但可能堆逻辑     | 分散，但每个常量行为清晰 |
| 适用场景 | 行为相似，只是参数不同 | 行为差异大，算法不同     |

---

## 补充：混合方式

也可以让枚举类提供默认实现，个别常量再单独覆盖。

```java
interface Task {
    void run();
}

enum Status implements Task {
    START {
        @Override
        public void run() {
            System.out.println("开始");
        }
    },
    STOP;

    @Override
    public void run() {
        System.out.println("默认行为");
    }
}
```

此时：

- `Status.START.run()` 执行常量自己的实现。
- `Status.STOP.run()` 使用枚举类中的默认实现。

---

## 小结

- **方式一**：枚举类统一实现接口，代码集中，适合行为相似的常量。
- **方式二**：每个枚举常量单独实现接口，多态清晰，适合行为差异大的常量。
- 如果大多数行为相同、少数不同，可以使用“默认实现 + 个别常量覆盖”的混合方式。
