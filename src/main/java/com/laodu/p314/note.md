# Java Date 与 Calendar API 笔记

> 范围：只整理常用、未废弃 API。
> `Date` 中大量字段读写方法已废弃，本笔记不展开。新代码优先考虑 `java.time`。

---

## 一、核心区别


| 对比项   | `Date`            | `Calendar`                 |
| -------- | ----------------- | -------------------------- |
| 本质     | 时间点            | 日历字段 + 时区 + 日期计算 |
| 内部核心 | 一个`long` 毫秒数 | 一组日历字段               |
| 时区     | 本身不处理时区    | 明确带`TimeZone`           |
| 日期计算 | 不擅长            | 擅长，`add`、`roll`、`set` |
| 可变性   | 可变，非线程安全  | 可变，非线程安全           |
| 常见实现 | `java.util.Date`  | `GregorianCalendar`        |

一句话：
**Date 表示时间点，Calendar 表示日历字段和日期计算。**

---

## 二、Date 常用未废弃 API

### 1. 创建

```java
Date now = new Date();
Date date = new Date(epochMillis);
Date fromInstant = Date.from(Instant.now()); // Java 8+
```


| 方法                         | 说明                                  |
| ---------------------------- | ------------------------------------- |
| `new Date()`                 | 当前时间                              |
| `new Date(long date)`        | 从 1970-01-01 00:00:00 UTC 起的毫秒数 |
| `Date.from(Instant instant)` | Java 8+，从`Instant` 创建             |

### 2. 时间点读写


| 方法                      | 说明                   |
| ------------------------- | ---------------------- |
| `long getTime()`          | 获取毫秒时间戳         |
| `void setTime(long time)` | 设置毫秒时间戳         |
| `Instant toInstant()`     | Java 8+，转成`Instant` |

```java
Date date = new Date();
long ms = date.getTime();
date.setTime(ms + 86400000L);
Instant instant = date.toInstant();
```

### 3. 比较


| 方法                              | 说明               |
| --------------------------------- | ------------------ |
| `boolean after(Date when)`        | 是否在指定时间之后 |
| `boolean before(Date when)`       | 是否在指定时间之前 |
| `int compareTo(Date anotherDate)` | 比较时间顺序       |
| `boolean equals(Object obj)`      | 比较毫秒值         |

```java
Date a = new Date();
Date b = new Date(a.getTime() + 1000);

boolean after = b.after(a);
int cmp = a.compareTo(b);
```

### 4. 其他


| 方法                | 说明                         |
| ------------------- | ---------------------------- |
| `Object clone()`    | 克隆                         |
| `int hashCode()`    | 哈希值                       |
| `String toString()` | 默认格式字符串，依赖默认时区 |

---

## 三、Calendar 常用未废弃 API

### 1. 获取实例

```java
Calendar cal = Calendar.getInstance();
Calendar cal2 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
Calendar cal3 = Calendar.getInstance(Locale.CHINA);
Calendar cal4 = Calendar.getInstance(TimeZone.getDefault(), Locale.CHINA);
```

Java 8+ 也可用 `Calendar.Builder`：

```java
Calendar cal = new Calendar.Builder()
        .setDate(2026, Calendar.OCTOBER, 16)
        .setTimeOfDay(12, 30, 0)
        .build();
```

### 2. 常用字段常量


| 常量                                      | 含义                |
| ----------------------------------------- | ------------------- |
| `Calendar.YEAR`                           | 年                  |
| `Calendar.MONTH`                          | 月，**0 表示 1 月** |
| `Calendar.DAY_OF_MONTH` / `Calendar.DATE` | 日                  |
| `Calendar.DAY_OF_YEAR`                    | 一年中的第几天      |
| `Calendar.DAY_OF_WEEK`                    | 星期，`SUNDAY=1`    |
| `Calendar.HOUR_OF_DAY`                    | 24 小时制小时       |
| `Calendar.HOUR`                           | 12 小时制小时       |
| `Calendar.MINUTE`                         | 分                  |
| `Calendar.SECOND`                         | 秒                  |
| `Calendar.MILLISECOND`                    | 毫秒                |
| `Calendar.WEEK_OF_YEAR`                   | 一年中的第几周      |
| `Calendar.WEEK_OF_MONTH`                  | 一月中的第几周      |
| `Calendar.ZONE_OFFSET`                    | 时区偏移            |
| `Calendar.DST_OFFSET`                     | 夏令时偏移          |

### 3. 读取和设置字段


| 方法                                                                             | 说明           |
| -------------------------------------------------------------------------------- | -------------- |
| `int get(int field)`                                                             | 获取字段       |
| `void set(int field, int value)`                                                 | 设置字段       |
| `void set(int year, int month, int date)`                                        | 设置年月日     |
| `void set(int year, int month, int date, int hourOfDay, int minute)`             | 设置到分       |
| `void set(int year, int month, int date, int hourOfDay, int minute, int second)` | 设置到秒       |
| `void clear()`                                                                   | 清除所有字段   |
| `void clear(int field)`                                                          | 清除指定字段   |
| `boolean isSet(int field)`                                                       | 字段是否已设置 |

```java
Calendar cal = Calendar.getInstance();

int year = cal.get(Calendar.YEAR);
int month = cal.get(Calendar.MONTH) + 1; // 注意 +1
int day = cal.get(Calendar.DAY_OF_MONTH);
int hour = cal.get(Calendar.HOUR_OF_DAY);

cal.set(2026, Calendar.OCTOBER, 16, 12, 30, 0);
```

### 4. 日期计算


| 方法                               | 说明                     |
| ---------------------------------- | ------------------------ |
| `void add(int field, int amount)`  | 加减字段，会进位         |
| `void roll(int field, int amount)` | 滚动字段，不影响更大字段 |
| `void roll(int field, boolean up)` | 向上/向下滚动            |

```java
Calendar cal = Calendar.getInstance();

cal.add(Calendar.DAY_OF_MONTH, 7);  // 加 7 天
cal.add(Calendar.MONTH, -1);        // 减 1 个月

cal.roll(Calendar.MONTH, 13);       // 只在月份内滚动，不改变年份
```

### 5. 时间点互转


| 方法                                | 说明                   |
| ----------------------------------- | ---------------------- |
| `Date getTime()`                    | 转成`Date`             |
| `void setTime(Date date)`           | 从`Date` 设置          |
| `long getTimeInMillis()`            | 获取毫秒时间戳         |
| `void setTimeInMillis(long millis)` | 设置毫秒时间戳         |
| `Instant toInstant()`               | Java 8+，转成`Instant` |

```java
Calendar cal = Calendar.getInstance();
Date date = cal.getTime();

cal.setTime(new Date());
long ms = cal.getTimeInMillis();
```

### 6. 时区与宽松模式


| 方法                                        | 说明               |
| ------------------------------------------- | ------------------ |
| `TimeZone getTimeZone()`                    | 获取时区           |
| `void setTimeZone(TimeZone value)`          | 设置时区           |
| `boolean isLenient()`                       | 是否宽松模式       |
| `void setLenient(boolean lenient)`          | 设置宽松模式       |
| `int getFirstDayOfWeek()`                   | 一周第一天         |
| `void setFirstDayOfWeek(int value)`         | 设置一周第一天     |
| `int getMinimalDaysInFirstWeek()`           | 第一周最少天数     |
| `void setMinimalDaysInFirstWeek(int value)` | 设置第一周最少天数 |

```java
cal.setLenient(false); // 严格模式，非法日期会抛异常
cal.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
```

### 7. 最大最小值


| 方法                                | 说明           |
| ----------------------------------- | -------------- |
| `int getActualMaximum(int field)`   | 当前实际最大值 |
| `int getActualMinimum(int field)`   | 当前实际最小值 |
| `int getMaximum(int field)`         | 理论最大值     |
| `int getMinimum(int field)`         | 理论最小值     |
| `int getGreatestMinimum(int field)` | 最大最小值     |
| `int getLeastMaximum(int field)`    | 最小最大值     |

```java
int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH); // 当月最大天数
```

### 8. 比较


| 方法                                      | 说明               |
| ----------------------------------------- | ------------------ |
| `boolean after(Object when)`              | 是否在指定时间之后 |
| `boolean before(Object when)`             | 是否在指定时间之前 |
| `int compareTo(Calendar anotherCalendar)` | 比较               |
| `boolean equals(Object obj)`              | 是否相等           |

```java
Calendar a = Calendar.getInstance();
Calendar b = Calendar.getInstance();
b.add(Calendar.DAY_OF_MONTH, 1);

boolean after = b.after(a);
int cmp = a.compareTo(b);
```

---

## 四、Date 与 Calendar 互转

```java
Date date = new Date();

Calendar cal = Calendar.getInstance();
cal.setTime(date);

Date back = cal.getTime();
```

Java 8+：

```java
Instant instant = date.toInstant();

Calendar cal = Calendar.getInstance();
cal.setTimeInMillis(instant.toEpochMilli());

Instant instant2 = cal.toInstant();
```

---

## 五、常见操作模板

### 获取当前年月日时分秒

```java
Calendar cal = Calendar.getInstance();

int year = cal.get(Calendar.YEAR);
int month = cal.get(Calendar.MONTH) + 1;
int day = cal.get(Calendar.DAY_OF_MONTH);
int hour = cal.get(Calendar.HOUR_OF_DAY);
int minute = cal.get(Calendar.MINUTE);
int second = cal.get(Calendar.SECOND);
```

### 日期加减

```java
Calendar cal = Calendar.getInstance();

cal.add(Calendar.DAY_OF_MONTH, 7);
cal.add(Calendar.MONTH, -1);
cal.add(Calendar.YEAR, 1);
```

### 获取当月最大天数

```java
Calendar cal = Calendar.getInstance();
int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
```

### 设置指定日期

```java
Calendar cal = Calendar.getInstance();
cal.set(2026, Calendar.OCTOBER, 16, 12, 30, 0);
```

---

## 六、易错点

1. **`Calendar.MONTH` 从 0 开始**
   0 = 一月，11 = 十二月。
2. **`Calendar.DAY_OF_WEEK` 从星期日开始**
   `SUNDAY = 1`，`MONDAY = 2`，……，`SATURDAY = 7`。
3. **`add` 和 `roll` 不同**
   `add` 会进位，`roll` 不会影响更大字段。
4. **Calendar 可变、非线程安全**
   多线程环境下不要共享同一个 `Calendar` 实例。
5. **Date 也不适合做复杂日期计算**
   字段读取、月份、年份等操作应交给 `Calendar` 或 `java.time`。
6. **Date 的字段读写方法基本已废弃**
   本笔记不列入，也不建议使用。

---

## 七、Java 8+ 替代建议


| 老 API             | 新 API                            |
| ------------------ | --------------------------------- |
| `Date`             | `Instant`                         |
| `Calendar`         | `ZonedDateTime` / `LocalDateTime` |
| `SimpleDateFormat` | `DateTimeFormatter`               |
| 日期加减           | `plusDays`、`minusMonths` 等      |

```java
Instant instant = Instant.now();
LocalDate date = LocalDate.now();
LocalDateTime dateTime = LocalDateTime.now();
ZonedDateTime zonedDateTime = ZonedDateTime.now();

LocalDate nextWeek = date.plusWeeks(1);
int month = nextWeek.getMonthValue(); // 1~12
```

---

## 八、总结

- `Date`：表示时间点，核心是毫秒时间戳。
- `Calendar`：表示日历字段，支持时区、字段读写、日期加减。
- 两者同时存在主要是历史兼容原因。
- 维护老代码时：`Date` 当时间点用，`Calendar` 做日期计算。
- 新代码优先使用 `java.time`。
