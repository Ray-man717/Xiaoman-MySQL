# Java 基础语法使用手册

> 从「Hello World」讲到「能看懂一个完整的类」。
> 每个语法点都配可运行的小例子，最后有常见错误与速查表。
> 环境：JDK 17，`javac` 编译、`java` 运行，或 IntelliJ IDEA 里直接 Run。

---

## 目录

1. [运行环境与第一个程序](#1-运行环境与第一个程序)
2. [变量、数据类型与类型转换](#2-变量数据类型与类型转换)
3. [运算符](#3-运算符)
4. [流程控制](#4-流程控制)
5. [数组](#5-数组)
6. [方法](#6-方法)
7. [类与对象](#7-类与对象)
8. [继承与重写](#8-继承与重写)
9. [多态、抽象类与接口](#9-多态抽象类与接口)
10. [static、final 与常量](#10-staticfinal-与常量)
11. [包、访问修饰符与 import](#11-包访问修饰符与-import)
12. [异常处理](#12-异常处理)
13. [常用类](#13-常用类)
14. [集合入门](#14-集合入门)
15. [枚举与注解](#15-枚举与注解)
16. [输入输出基础](#16-输入输出基础)
17. [和本项目代码对照](#17-和本项目代码对照)
18. [常见错误与速查表](#18-常见错误与速查表)

---

## 1. 运行环境与第一个程序

### 1.1 JDK、JRE、JVM

| 名称 | 作用 |
| --- | --- |
| JDK | Java 开发工具包，包含 `javac` 编译器和 JRE |
| JRE | Java 运行时环境，只能运行不能编译 |
| JVM | 虚拟机，执行 `.class` 字节码 |

写代码要 **JDK**，只运行程序有 JRE 就够。

```bash
javac -version      # 查看编译器版本
java -version       # 查看运行时版本
```

### 1.2 第一个程序

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello EMS");
    }
}
```

保存为 `Hello.java`，文件名必须和 public 类名一致。

```bash
javac Hello.java      # 生成 Hello.class
java Hello            # 运行（不带 .class）
```

### 1.3 程序结构

```java
package com.github.rayman717;        // ① 包声明（可选，但项目里必须有）

import java.util.Scanner;            // ② 导入其他包的类

public class Demo {                  // ③ 类：Java 代码的基本单位

    private int count = 0;           // ④ 成员变量

    public static void main(String[] args) {   // ⑤ 入口方法
        System.out.println("开始");            // ⑥ 语句
    }

    public void sayHello() {         // ⑦ 方法
        System.out.println("hello");
    }
}
```

记住四件事：

- 一个 `.java` 文件最多一个 `public` 类，文件名与它同名；
- 入口是 `public static void main(String[] args)`，一个字母都不能错；
- 每条语句以 `;` 结尾；
- 代码块用 `{}` 包起来。

---

## 2. 变量、数据类型与类型转换

### 2.1 八种基本类型

| 类型 | 字节 | 范围 / 说明 | 例子 |
| --- | --- | --- | --- |
| `byte` | 1 | -128 ~ 127 | `byte b = 100;` |
| `short` | 2 | -32768 ~ 32767 | `short s = 1000;` |
| `int` | 4 | ±21 亿 | `int n = 42;` |
| `long` | 8 | 很大，字面量加 `L` | `long t = 1000L;` |
| `float` | 4 | 小数，字面量加 `f` | `float f = 1.5f;` |
| `double` | 8 | 小数，默认类型 | `double d = 26.4;` |
| `char` | 2 | 单个字符，单引号 | `char c = 'A';` |
| `boolean` | 1 | `true` / `false` | `boolean ok = true;` |

变量声明与赋值：

```java
int count = 0;                  // 声明 + 初始化
double temp = 26.4;
String name = "ems-01";         // String 是引用类型，不是基本类型

count = 5;                      // 重新赋值
```

`String` 不属于八种基本类型，但用起来像：

```java
String a = "hello";
String b = "world";
String c = a + " " + b;         // 字符串拼接
```

### 2.2 类型转换

**小转大：自动**

```java
int i = 100;
long l = i;          // int → long，自动
double d = i;        // int → double，自动
```

**大转小：强制，可能丢精度**

```java
double d = 26.9;
int i = (int) d;               // 26，小数部分直接截断
byte b = (byte) 300;           // 溢出，变成 44
```

**字符串与数字互转**

```java
int n = Integer.parseInt("123");
double v = Double.parseDouble("26.4");

String s1 = String.valueOf(42);
String s2 = 42 + "";           // 也能转，但推荐 valueOf
```

`parseInt("abc")` 会抛 `NumberFormatException`，要 try/catch 或先校验。

### 2.3 局部变量 vs 成员变量

```java
public class Demo {
    private int count = 0;      // 成员变量（字段），有默认值 0
    private String name;        // 默认 null

    public void test() {
        int local = 10;         // 局部变量，必须先赋值再用
        System.out.println(local);
    }
}
```

---

## 3. 运算符

### 3.1 算术

```java
int a = 7, b = 2;
System.out.println(a + b);     // 9
System.out.println(a - b);     // 5
System.out.println(a * b);     // 14
System.out.println(a / b);     // 3（整数除法，小数被丢掉）
System.out.println(a % b);     // 1（取余）

double x = 7.0 / 2;            // 3.5，要小数就至少有一个是小数
```

**整数除法的坑**：`7 / 2` 是 `3`，不是 `3.5`。想要小数写 `7.0 / 2` 或 `(double) 7 / 2`。

### 3.2 自增自减

```java
int i = 5;
i++;                // 6
i--;                // 5

int j = i++;        // j = 5，i = 6（先用后加）
int k = ++i;        // i = 7，k = 7（先加后用）
```

### 3.3 比较与逻辑

```java
int a = 5;
System.out.println(a > 3);        // true
System.out.println(a >= 3);       // true
System.out.println(a == 5);       // true，注意是两个等号
System.out.println(a != 3);       // true

boolean ok = a > 3 && a < 10;     // 与
boolean any = a > 10 || a < 10;   // 或
boolean not = !ok;                // 非
```

**字符串比较要用 `equals`**：

```java
String s = "abc";
System.out.println(s == "abc");        // 可能为 false（比较的是引用）
System.out.println(s.equals("abc"));   // true，正确写法
```

### 3.4 三元运算符

```java
int score = 85;
String level = score >= 60 ? "及格" : "不及格";
```

### 3.5 位运算（了解即可）

```java
int a = 5;          // 0101
int b = 3;          // 0011
a & b;              // 0001 = 1，与
a | b;              // 0111 = 7，或
a ^ b;              // 0110 = 6，异或
a << 1;             // 1010 = 10，左移一位等于乘 2
a >> 1;             // 0010 = 2，右移一位等于除 2
```

---

## 4. 流程控制

### 4.1 if / else

```java
double temp = 26.4;

if (temp > 30) {
    System.out.println("高温");
} else if (temp > 20) {
    System.out.println("舒适");
} else {
    System.out.println("偏冷");
}
```

### 4.2 switch

```java
int day = 3;
switch (day) {
    case 1:
        System.out.println("周一");
        break;
    case 2:
        System.out.println("周二");
        break;
    default:
        System.out.println("其他");
        break;
}

// JDK 14+ 的箭头写法，自动 break
String text = switch (day) {
    case 1, 2, 3, 4, 5 -> "工作日";
    case 6, 7 -> "周末";
    default -> "未知";
};
```

忘记 `break` 会「贯穿」到下一个 case，是常见 bug。

### 4.3 for 循环

```java
for (int i = 0; i < 5; i++) {
    System.out.println(i);          // 0 1 2 3 4
}
```

`for-each` 遍历数组或集合：

```java
int[] nums = {1, 2, 3};
for (int n : nums) {
    System.out.println(n);
}

List<String> names = List.of("A", "B");
for (String name : names) {
    System.out.println(name);
}
```

### 4.4 while / do-while

```java
int i = 0;
while (i < 5) {
    System.out.println(i);
    i++;
}

int j = 0;
do {
    System.out.println("至少执行一次");
    j++;
} while (j < 0);
```

### 4.5 break / continue

```java
for (int i = 0; i < 10; i++) {
    if (i == 3) {
        continue;          // 跳过本次，继续下一次
    }
    if (i == 7) {
        break;             // 结束整个循环
    }
    System.out.println(i);
}
```

---

## 5. 数组

### 5.1 声明与初始化

```java
int[] nums = {1, 2, 3, 4};          // 字面量
int[] a = new int[5];               // 默认全是 0
String[] names = new String[3];     // 默认全是 null

int[] b = new int[]{10, 20, 30};
```

### 5.2 访问与遍历

```java
int[] nums = {10, 20, 30};

System.out.println(nums[0]);        // 10
System.out.println(nums.length);    // 3，注意是 length 不是 length()

for (int i = 0; i < nums.length; i++) {
    System.out.println(nums[i]);
}

for (int n : nums) {
    System.out.println(n);
}
```

下标从 `0` 开始，越界会抛 `ArrayIndexOutOfBoundsException`。

### 5.3 二维数组

```java
int[][] grid = new int[2][3];
grid[0][1] = 5;

int[][] matrix = {
        {1, 2, 3},
        {4, 5, 6}
};

for (int[] row : matrix) {
    for (int v : row) {
        System.out.print(v + " ");
    }
    System.out.println();
}
```

### 5.4 常用操作

```java
int[] nums = {3, 1, 2};
Arrays.sort(nums);                       // 排序：1 2 3
int[] copy = Arrays.copyOf(nums, 5);     // 复制（不足补 0）
String s = Arrays.toString(nums);        // "[1, 2, 3]"

// 最大值
int max = nums[0];
for (int n : nums) {
    if (n > max) {
        max = n;
    }
}
```

`Arrays.toString(nums)` 不能省——直接打印数组是 `[I@1b6d3586` 这种地址。

---

## 6. 方法

### 6.1 定义与调用

```java
public class Demo {

    public static void main(String[] args) {
        int sum = add(3, 5);
        System.out.println(sum);          // 8

        sayHello("EMS");
    }

    static int add(int a, int b) {
        return a + b;
    }

    static void sayHello(String name) {
        System.out.println("你好，" + name);
    }
}
```

| 关键字 | 含义 |
| --- | --- |
| `static` | 静态方法，不用 new 对象就能调 |
| `void` | 没有返回值 |
| `return` | 返回值，或提前结束方法 |

### 6.2 参数传递

Java 只有「值传递」：

- 基本类型传的是值的副本，方法里改了不影响外面；
- 引用类型传的是引用的副本，方法里改对象内容会影响外面。

```java
static void change(int n) {
    n = 100;            // 不影响外面
}

static void fill(int[] arr) {
    arr[0] = 100;       // 影响外面，因为改的是数组内容
}
```

### 6.3 方法重载

同名方法，参数列表不同（个数、类型、顺序）：

```java
static int add(int a, int b) { return a + b; }
static double add(double a, double b) { return a + b; }
static int add(int a, int b, int c) { return a + b + c; }
```

返回值不同不算重载。

### 6.4 可变参数

```java
static int sum(int... nums) {
    int total = 0;
    for (int n : nums) {
        total += n;
    }
    return total;
}

sum(1, 2);          // 3
sum(1, 2, 3, 4);    // 10
```

### 6.5 递归

```java
static int factorial(int n) {
    if (n <= 1) {
        return 1;               // 终止条件
    }
    return n * factorial(n - 1);
}
```

递归一定要有终止条件，否则栈溢出（`StackOverflowError`）。

---

## 7. 类与对象

### 7.1 定义类

```java
public class EmsRecord {
    private int id;              // 字段（成员变量）
    private double temp;
    private String time;

    public EmsRecord() {         // 无参构造
    }

    public EmsRecord(int id, double temp, String time) {   // 全参构造
        this.id = id;            // this 区分字段和参数
        this.temp = temp;
        this.time = time;
    }

    public int getId() {         // getter
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
```

### 7.2 创建与使用对象

```java
EmsRecord r = new EmsRecord(1, 26.4, "2026-01-01 08:00:00");
System.out.println(r.getTemp());

EmsRecord r2 = new EmsRecord();
r2.setId(2);
r2.setTemp(27.1);
```

`new` 做的事：分配内存 → 调构造方法 → 返回引用。

### 7.3 封装

- 字段用 `private`；
- 通过 `getter/setter` 访问；
- 好处：可以在 setter 里做校验。

```java
public void setTemp(double temp) {
    if (temp < -100 || temp > 200) {
        throw new IllegalArgumentException("温度超出合理范围：" + temp);
    }
    this.temp = temp;
}
```

### 7.4 对象与引用

```java
EmsRecord a = new EmsRecord(1, 26.4, "08:00");
EmsRecord b = a;              // b 和 a 指向同一个对象
b.setTemp(30.0);              // a 的 temp 也变了

System.out.println(a == b);               // true，比较引用
System.out.println(a.equals(b));          // 默认也是比较引用，除非重写
```

---

## 8. 继承与重写

### 8.1 extends

```java
public class Animal {
    protected String name;

    public void speak() {
        System.out.println("动物叫");
    }
}

public class Dog extends Animal {
    @Override
    public void speak() {
        System.out.println("汪汪");
    }
}
```

```java
Animal a = new Dog();
a.speak();          // 汪汪（多态：看实际对象类型）
```

### 8.2 super

```java
public class Puppy extends Dog {
    @Override
    public void speak() {
        super.speak();              // 调用父类方法
        System.out.println("嘤嘤");
    }
}
```

### 8.3 构造方法链

```java
public class Animal {
    protected String name;

    public Animal(String name) {
        this.name = name;
    }
}

public class Dog extends Animal {
    public Dog(String name) {
        super(name);                // 必须在第一行
    }
}
```

子类构造必须先调父类构造（不写 `super(...)` 就自动调父类无参构造）。

### 8.4 重写（Override）要求

- 方法名、参数、返回值一致；
- 访问权限不能变小；
- 用 `@Override` 注解，写错会编译报错。

### 8.5 final

```java
final class A { }             // 不能被继承
final void method() { }       // 不能被重写
final int x = 10;             // 不能改值
```

---

## 9. 多态、抽象类与接口

### 9.1 多态

同一段代码，对不同对象产生不同行为：

```java
public interface DataSource {
    List<String> load();
}

public class FileSource implements DataSource { ... }
public class DbSource implements DataSource { ... }

public void print(DataSource source) {
    for (String line : source.load()) {
        System.out.println(line);
    }
}

print(new FileSource());       // 读文件
print(new DbSource());         // 读数据库
```

### 9.2 抽象类

```java
public abstract class Shape {
    public abstract double area();     // 没有方法体

    public void print() {
        System.out.println("面积：" + area());
    }
}

public class Circle extends Shape {
    private final double r;

    public Circle(double r) {
        this.r = r;
    }

    @Override
    public double area() {
        return Math.PI * r * r;
    }
}
```

抽象类可以包含抽象方法和普通方法；不能直接 `new`。

### 9.3 接口

```java
public interface Repository<T> {
    List<T> findAll();

    default int count() {
        return findAll().size();       // 默认实现（JDK 8+）
    }
}

public class EmsRepository implements Repository<EmsRecord> {
    @Override
    public List<EmsRecord> findAll() {
        return new ArrayList<>();
    }
}
```

一个类只能继承一个类，但可以实现多个接口。

### 9.4 面向接口编程

```java
// 不好：依赖具体实现
EmsDao dao = new EmsDao();

// 好：依赖接口，方便替换和测试
Repository<EmsRecord> repo = new EmsRepository();
```

---

## 10. static、final 与常量

### 10.1 static

```java
public class ConfigManager {
    private static Properties prop = new Properties();   // 静态字段：全类共享

    public static DbConfig getDbConfig() {               // 静态方法：不用 new
        // ...
    }
}
```

- 静态成员属于类，不属于对象；
- 静态方法里不能直接用 `this`，也不能访问非静态成员；
- 工具类（`SqlUtil`、`LogUtil`）常用静态方法。

### 10.2 final

```java
final int MAX = 100;                       // 常量：不能改
static final double PI = 3.14159;          // 静态常量
```

### 10.3 常量类

```java
public final class EmsConstants {
    public static final int SERVER_PORT = 8888;
    public static final double TEMP_ALARM = 30.0;

    private EmsConstants() {
    }
}
```

### 10.4 静态初始化块

```java
public class DbUtil {
    private static final String URL;

    static {
        URL = "jdbc:mysql://127.0.0.1:3306/EMS";
    }
}
```

---

## 11. 包、访问修饰符与 import

### 11.1 包

```java
package com.github.rayman717.util;
```

- 包名全小写；
- 通常按「域名倒置 + 模块」；
- 一个类的完整名字是「包名.类名」，如 `com.github.rayman717.util.SqlUtil`。

### 11.2 四种访问修饰符

| 修饰符 | 同一个类 | 同一个包 | 子类 | 任何地方 |
| --- | --- | --- | --- | --- |
| `private` | ✓ | ✗ | ✗ | ✗ |
| 默认（不写） | ✓ | ✓ | ✗ | ✗ |
| `protected` | ✓ | ✓ | ✓ | ✗ |
| `public` | ✓ | ✓ | ✓ | ✓ |

### 11.3 import

```java
import java.util.List;              // 单个类
import java.util.*;                 // 整个包（不推荐，容易冲突）
```

`java.lang` 下的类（`String`、`System`、`Math`）不用导入。

---

## 12. 异常处理

### 12.1 try-catch-finally

```java
try {
    int n = Integer.parseInt("abc");
    System.out.println(n);
} catch (NumberFormatException e) {
    System.out.println("不是数字：" + e.getMessage());
} finally {
    System.out.println("无论成败都执行");       // 释放资源、收尾
}
```

### 12.2 多个 catch

```java
try {
    // ...
} catch (NumberFormatException e) {
    // 数字格式错
} catch (NullPointerException e) {
    // 空指针
} catch (Exception e) {
    // 兜底：所有异常
}
```

顺序：子类异常在前，父类在后（否则后面的永远不执行，编译报错）。

### 12.3 throw 与 throws

```java
// 抛异常
public void setTemp(double temp) {
    if (temp > 200) {
        throw new IllegalArgumentException("温度不合法：" + temp);
    }
}

// 声明可能抛异常，交给调用方处理
public static DbConfig load() throws IOException {
    // ...
}
```

### 12.4 受检异常 vs 运行时异常

| 类型 | 举例 | 处理 |
| --- | --- | --- |
| 受检异常（Exception 的子类） | `IOException`、`SQLException` | 必须 try/catch 或 throws |
| 运行时异常（RuntimeException） | `NullPointerException`、`NumberFormatException` | 不强制，但应该避免 |

### 12.5 自定义异常

```java
public class DbException extends RuntimeException {
    public DbException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 12.6 try-with-resources

```java
try (Connection con = DriverManager.getConnection(url, user, pwd);
     Statement st = con.createStatement()) {
    // 用完自动关闭，异常也会关
}
```

凡是实现了 `AutoCloseable` 的资源（流、连接、`Scanner`）都应该这样写。

---

## 13. 常用类

### 13.1 String

```java
String s = "EMS";

s.length();                  // 3
s.isEmpty();                 // false
s.equals("EMS");             // true
s.equalsIgnoreCase("ems");   // true
s.contains("MS");            // true
s.startsWith("E");           // true
s.substring(1);              // "MS"
s.substring(0, 2);           // "EM"
s.indexOf("M");              // 1
s.replace("E", "e");         // "eMS"
s.trim();                    // 去首尾空格
s.split(",");                // 按逗号切分
s.toUpperCase();             // "EMS"
s.charAt(0);                 // 'E'
```

String 不可变，每次修改都会产生新对象。

### 13.2 StringBuilder

```java
StringBuilder sb = new StringBuilder();
sb.append("温度：");
sb.append(26.4);
sb.append("℃");
String result = sb.toString();
```

循环里拼接字符串用 `StringBuilder`，不要用 `+`（性能差）。

### 13.3 包装类

```java
Integer a = 100;              // 自动装箱
int b = a;                    // 自动拆箱

Integer x = Integer.valueOf("123");
int y = Integer.parseInt("123");
```

注意 `Integer` 缓存 -128~127，`Integer(1000) == Integer(1000)` 是 `false`，比较值用 `equals`。

### 13.4 Math

```java
Math.abs(-5);          // 5
Math.max(3, 7);        // 7
Math.min(3, 7);        // 3
Math.pow(2, 3);        // 8.0
Math.sqrt(16);         // 4.0
Math.round(2.6);       // 3
Math.random();         // 0~1 的随机数
```

### 13.5 日期时间（JDK 8+）

```java
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

LocalDate today = LocalDate.now();
LocalDateTime now = LocalDateTime.now();

now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
LocalDate.parse("2026-01-01");
now.plusDays(7);
now.minusHours(2);
```

### 13.6 BigDecimal（精确小数）

```java
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
System.out.println(a.add(b));          // 0.3，不是 0.30000000000000004

// 用字符串构造，不要用 double 构造
```

涉及金额等精确计算时用 `BigDecimal`，`double` 有精度误差。

### 13.7 Random

```java
Random random = new Random();
int n = random.nextInt(100);           // 0~99
double d = random.nextDouble();        // 0~1
```

---

## 14. 集合入门

### 14.1 List（有序、可重复）

```java
import java.util.ArrayList;
import java.util.List;

List<String> names = new ArrayList<>();
names.add("A");
names.add("B");
names.add("A");            // 允许重复

names.size();              // 3
names.get(0);              // "A"
names.contains("B");       // true
names.remove("A");         // 删第一个 "A"
names.isEmpty();           // false

for (String name : names) {
    System.out.println(name);
}
```

`List.of(...)` 创建不可变列表，不能 add。

### 14.2 Set（无序、不重复）

```java
import java.util.HashSet;
import java.util.Set;

Set<String> devices = new HashSet<>();
devices.add("ems-01");
devices.add("ems-01");     // 重复，不会加进去
System.out.println(devices.size());   // 1
```

### 14.3 Map（键值对）

```java
import java.util.HashMap;
import java.util.Map;

Map<String, Double> lastTemp = new HashMap<>();
lastTemp.put("ems-01", 26.4);
lastTemp.put("ems-02", 27.1);

lastTemp.get("ems-01");                 // 26.4
lastTemp.containsKey("ems-03");         // false
lastTemp.getOrDefault("ems-03", 0.0);   // 0.0
lastTemp.remove("ems-02");

for (Map.Entry<String, Double> entry : lastTemp.entrySet()) {
    System.out.println(entry.getKey() + " = " + entry.getValue());
}
```

### 14.4 泛型

```java
List<String> names = new ArrayList<>();     // 只能放 String
Map<String, Integer> counts = new HashMap<>();

List<EmsRecord> records = new ArrayList<>();  // 放自定义对象
```

泛型让编译期就能发现类型错误。

### 14.5 遍历与删除

```java
// 遍历删除要用 Iterator，不能直接 for-each remove
Iterator<String> it = names.iterator();
while (it.hasNext()) {
    if (it.next().startsWith("A")) {
        it.remove();
    }
}
```

---

## 15. 枚举与注解

### 15.1 枚举

```java
public enum DeviceStatus {
    ONLINE, OFFLINE, UNKNOWN;

    public boolean isOnline() {
        return this == ONLINE;
    }
}

DeviceStatus s = DeviceStatus.ONLINE;
if (s == DeviceStatus.ONLINE) {
    System.out.println("在线");
}

// 带字段的枚举
public enum Level {
    LOW(10), MID(50), HIGH(100);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### 15.2 注解

```java
@Override            // 重写父类方法，写错会报错
@Deprecated          // 标记已过时
@SuppressWarnings("unchecked")   // 抑制警告
@FunctionalInterface // 函数式接口

// 自定义注解
public @interface Author {
    String value();
}
```

---

## 16. 输入输出基础

### 16.1 控制台输出

```java
System.out.println("换行输出");
System.out.print("不换行");
System.out.printf("温度：%.1f℃，湿度：%d%%%n", 26.4, 55);
System.err.println("错误信息");
```

### 16.2 Scanner 读键盘

```java
import java.util.Scanner;

Scanner sc = new Scanner(System.in);

System.out.print("请输入温度：");
double temp = sc.nextDouble();

System.out.print("请输入名称：");
String name = sc.next();

String line = sc.nextLine();      // 读一整行（注意和 next 混用的换行问题）
```

### 16.3 常见输入问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| `nextInt()` 后 `nextLine()` 读到空行 | 残留了回车 | 再调一次 `nextLine()` 吃掉 |
| 输入非数字抛 `InputMismatchException` | 没校验 | `hasNextDouble()` 先判断 |

### 16.4 文件读写（详见《IO 使用手册》）

```java
Files.readString(Path.of("config.properties"), StandardCharsets.UTF_8);
Files.writeString(Path.of("out.txt"), "内容", StandardCharsets.UTF_8);
```

---

## 17. 和本项目代码对照

| 项目代码 | 用到的语法 | 在哪一节 |
| --- | --- | --- |
| `Hello.java` 的 `public static void main` | 入口方法 | 第 1 章 |
| `DbConfig` 的字段与构造方法 | 类、构造器、封装 | 第 7 章 |
| `DbConfig` 的 getter/setter | 封装 | 第 7 章 |
| `SqlUtil.getConnection` | 方法、`Class.forName`、`try/catch` | 第 6、12 章 |
| `SqlUtil.CreateTable` 里的字符串拼接 | String | 第 13 章 |
| `ConfigManager` 的 `static` 字段与方法 | static | 第 10 章 |
| `ConfigManager.getDbConfig` 返回 `DbConfig` | 类与对象 | 第 7 章 |
| `LogUtil.info/error` | 静态方法 | 第 10 章 |
| `SwingJFrame extends JFrame` | 继承 | 第 8 章 |
| `MyDialog extends JDialog` | 继承、构造里 `super(...)` | 第 8 章 |
| `new ActionListener() { ... }` | 匿名内部类、接口 | 第 9 章 |
| 数据库查询的 `while (rs.next())` | while、方法调用 | 第 4 章 |
| `catch (SQLException e)` | 异常处理 | 第 12 章 |
| `List<EmsRecord>` | 集合与泛型 | 第 14 章 |

看项目代码时的建议顺序：先看 `DbConfig`（类），再看 `ConfigManager`（static 与文件），
然后 `SqlUtil`（方法、异常、JDBC），最后 `SwingJFrame`（继承、接口、事件）。

---

## 18. 常见错误与速查表

### 18.1 编译期错误

| 报错 | 原因 | 解决 |
| --- | --- | --- |
| `class X is public, should be declared in a file named X.java` | 文件名与类名不一致 | 改文件名 |
| `';' expected` | 少分号 | 补上 |
| `cannot find symbol` | 变量没声明、类没导入、拼写错 | 检查 import 和拼写 |
| `incompatible types` | 类型不匹配 | 强制转换或改类型 |
| `missing return statement` | 有返回值的方法没有 return | 补 return |
| `non-static variable cannot be referenced` | 静态方法里用了非静态成员 | 改成实例方法或加 static |
| `unreachable statement` | return 后面还有代码 | 删掉或调整 |

### 18.2 运行期异常

| 异常 | 常见原因 |
| --- | --- |
| `NullPointerException` | 对象没 new、方法返回 null 没判断 |
| `ArrayIndexOutOfBoundsException` | 下标越界（`<=` 写成 `<`） |
| `NumberFormatException` | `parseInt("abc")` |
| `ArithmeticException: / by zero` | 整数除以 0 |
| `ClassCastException` | 类型强转错误 |
| `ConcurrentModificationException` | 遍历时修改集合 |
| `StackOverflowError` | 递归没有终止条件 |
| `ClassNotFoundException` | 类路径缺 jar |

### 18.3 语法速查

| 想做什么 | 写法 |
| --- | --- |
| 声明变量 | `int n = 0;` |
| 字符串拼接 | `"a" + b` 或 `StringBuilder` |
| 判断相等 | `a == b`（基本类型）/ `a.equals(b)`（对象） |
| 循环 | `for (int i = 0; i < n; i++)` |
| 增强 for | `for (String s : list)` |
| 定义方法 | `public int add(int a, int b) { return a + b; }` |
| 创建对象 | `EmsRecord r = new EmsRecord();` |
| 继承 | `class Dog extends Animal` |
| 实现接口 | `class A implements B` |
| 捕获异常 | `try { } catch (Exception e) { }` |
| 关闭资源 | `try (Connection c = ...) { }` |
| 泛型列表 | `List<EmsRecord> list = new ArrayList<>();` |
| 键值对 | `Map<String, Double> map = new HashMap<>();` |
| 常量 | `public static final int PORT = 8888;` |

### 18.4 学习路线建议

1. 第 1–6 章：能写出「输入三个数、求平均、打印」这类程序；
2. 第 7–12 章：能写出「一个有字段、方法、构造器、异常处理的类」；
3. 第 13–16 章：会用 String、集合、Scanner、文件读写；
4. 第 17 章：能读懂本项目现有代码，并敢动手改。

---

## 附：代码与注释对照表

| 代码 | 作用（注释） | 参数/说明 |
| --- | --- | --- |
| **一、基本类型与变量** | | |
| `byte b = 100;` | 1 字节整数 | 范围 -128 ~ 127 |
| `short s = 1000;` | 2 字节整数 | 范围 -32768 ~ 32767 |
| `int n = 42;` | 4 字节整数 | 默认整数类型 |
| `long l = 1000L;` | 8 字节整数 | 字面量加 `L` |
| `float f = 1.5f;` | 4 字节小数 | 字面量加 `f` |
| `double d = 26.4;` | 8 字节小数 | 默认小数类型 |
| `char c = 'A';` | 单个字符 | 单引号，长度 1 |
| `boolean ok = true;` | 布尔 | 只有 `true` / `false` |
| `String s = "abc";` | 字符串 | 双引号，引用类型 |
| `int i = (int) d;` | 强制类型转换 | 大转小，小数被截断 |
| `double x = 7.0 / 2;` | 小数除法 | 至少一边是小数 |
| `Integer.parseInt("123")` | 字符串转整数 | 不是数字抛异常 |
| `String.valueOf(42)` | 任意类型转字符串 | |
| **二、运算符** | | |
| `a + b` `a - b` `a * b` `a / b` `a % b` | 算术 | `%` 取余；整数除法丢小数 |
| `i++` / `++i` | 自增 | 后用/先用 |
| `a += 1` | 复合赋值 | 等价 `a = a + 1` |
| `a > b` `a <= b` `a == b` `a != b` | 比较 | 对象比较用 `equals` |
| `a && b` `a \|\| b` `!a` | 逻辑与/或/非 | 短路求值 |
| `cond ? a : b` | 三元运算 | 条件真取 a |
| `a & b` `a \| b` `a ^ b` | 位运算 | 按位与/或/异或 |
| `a << 1` / `a >> 1` | 移位 | 左移等于乘 2 |
| **三、流程控制** | | |
| `if (cond) { } else if (cond) { } else { }` | 分支 | 条件必须 `boolean` |
| `switch (x) { case 1: break; default: }` | 多分支 | 别忘 `break` |
| `switch (x) { case 1, 2 -> "工作日"; }` | JDK 14+ 箭头 | 自动 break |
| `for (int i = 0; i < n; i++) { }` | 计数循环 | 初始化/条件/步长 |
| `for (String s : list) { }` | 增强 for | 遍历数组/集合 |
| `while (cond) { }` | 条件循环 | 条件先判断 |
| `do { } while (cond);` | 至少执行一次 | 条件后判断 |
| `break;` / `continue;` | 跳出 / 跳过 | |
| **四、数组** | | |
| `int[] a = new int[5];` | 创建数组 | 默认值 0 |
| `int[] b = {1, 2, 3};` | 字面量初始化 | |
| `a.length` | 长度 | 不是 `length()` |
| `a[0]` | 取元素 | 下标从 0，越界抛异常 |
| `Arrays.sort(a)` | 排序 | 升序 |
| `Arrays.copyOf(a, 10)` | 复制 | 不足补 0 |
| `Arrays.toString(a)` | 转字符串 | 打印用 |
| **五、方法** | | |
| `static int add(int a, int b) { return a + b; }` | 定义方法 | 参数、返回类型 |
| `static void say() { }` | 无返回值 | `void` |
| `static int sum(int... nums)` | 可变参数 | `nums` 是数组 |
| `add(1, 2)` | 调用 | 按顺序传参 |
| `return;` | 提前结束方法 | `void` 方法可用 |
| **六、类与对象** | | |
| `public class EmsRecord { }` | 定义类 | 文件名与 public 类同名 |
| `private int id;` | 字段 | `private` 封装 |
| `public EmsRecord() { }` | 无参构造 | 名字与类名相同 |
| `public EmsRecord(int id) { this.id = id; }` | 全参构造 | `this` 区分字段与参数 |
| `new EmsRecord(1)` | 创建对象 | `new` + 构造参数 |
| `r.getId()` / `r.setId(2)` | getter / setter | 访问私有字段 |
| `r.toString()` | 转字符串 | 可重写 |
| `r.equals(other)` | 比较内容 | 可重写 |
| **七、继承与接口** | | |
| `class Dog extends Animal` | 继承 | 只能继承一个 |
| `@Override` | 重写注解 | 写错会编译报错 |
| `super.method()` | 调父类方法 | |
| `super(name)` | 调父类构造 | 必须第一行 |
| `final class A { }` | 不可继承 | |
| `abstract class Shape { abstract double area(); }` | 抽象类 | 不能 new |
| `class A implements B` | 实现接口 | 可实现多个 |
| `interface B { default void f() { } }` | 接口默认方法 | JDK 8+ |
| **八、static、final、包** | | |
| `static int count;` | 静态字段 | 全类共享 |
| `static void util() { }` | 静态方法 | 不能访问实例成员 |
| `static final int MAX = 100;` | 常量 | 全大写下划线 |
| `package com.x.util;` | 包声明 | 全小写 |
| `import java.util.List;` | 导入类 | |
| `private` / `public` / `protected` | 访问修饰符 | 控制可见范围 |
| **九、异常** | | |
| `try { } catch (Exception e) { }` | 捕获 | `e.getMessage()` 取信息 |
| `try { } finally { }` | 一定执行 | 释放资源 |
| `throw new IllegalArgumentException("x")` | 抛异常 | |
| `void f() throws IOException { }` | 声明异常 | 交给调用方 |
| `class MyException extends RuntimeException` | 自定义异常 | |
| `try (Connection c = ...) { }` | 自动关闭 | `AutoCloseable` |
| **十、常用类** | | |
| `s.length()` | 字符串长度 | |
| `s.equals("x")` | 比较内容 | 不用 `==` |
| `s.contains("x")` | 是否包含 | |
| `s.startsWith("x")` | 是否以 x 开头 | |
| `s.substring(1, 3)` | 截取 | 1 含、3 不含 |
| `s.indexOf("x")` | 位置 | 没有返回 -1 |
| `s.replace("a", "b")` | 替换 | |
| `s.trim()` | 去首尾空格 | |
| `s.split(",")` | 切分 | 返回数组 |
| `s.toUpperCase()` | 转大写 | |
| `new StringBuilder().append(x).toString()` | 拼接 | 循环里用它 |
| `Math.max(a, b)` / `Math.abs(x)` / `Math.round(x)` | 数学 | |
| `LocalDateTime.now()` | 当前时间 | JDK 8+ |
| `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")` | 格式化 | |
| `new BigDecimal("0.1").add(new BigDecimal("0.2"))` | 精确计算 | 用字符串构造 |
| `new Random().nextInt(100)` | 随机数 | 0~99 |
| **十一、集合** | | |
| `List<String> list = new ArrayList<>();` | 列表 | 有序可重复 |
| `list.add("x")` | 添加 | |
| `list.get(0)` | 取 | 下标从 0 |
| `list.size()` | 长度 | |
| `list.contains("x")` | 是否包含 | |
| `Set<String> set = new HashSet<>();` | 集合 | 不重复 |
| `Map<String, Double> map = new HashMap<>();` | 键值对 | |
| `map.put("a", 1.0)` | 放 | 同键覆盖 |
| `map.get("a")` | 取 | 没有返回 null |
| `map.getOrDefault("a", 0.0)` | 取或默认 | |
| `map.containsKey("a")` | 是否含键 | |
| `for (Map.Entry<String, Double> e : map.entrySet())` | 遍历 Map | `e.getKey()`、`e.getValue()` |
| `Iterator<String> it = list.iterator()` | 迭代器 | 遍历时删除用它 |
| `List.of("a", "b")` | 不可变列表 | 不能 add |
| **十二、枚举与注解** | | |
| `enum Status { ONLINE, OFFLINE }` | 枚举 | 值全大写 |
| `Status.ONLINE` | 用枚举 | 用 `==` 比较 |
| `@Override` | 重写 | |
| `@Deprecated` | 已过时 | |
| `@SuppressWarnings("unchecked")` | 抑制警告 | |

