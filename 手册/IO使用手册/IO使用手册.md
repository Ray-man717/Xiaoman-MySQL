# Java I/O 使用手册（EMS 项目版）

> 面向本项目：`EMS` 环境监测系统（MySQL + JDBC + Swing + 网络上报），
> 讲清「配置怎么读」「日志怎么写」「数据怎么导出成 CSV」「大文件怎么不撑爆内存」。
> 环境：JDK 17，`java.io` 与 `java.nio.file` 都是 JDK 自带。

---

## 目录

1. [先分清：字节流与字符流](#1-先分清字节流与字符流)
2. [File 与 Path](#2-file-与-path)
3. [读文件](#3-读文件)
4. [写文件](#4-写文件)
5. [try-with-resources 与关闭](#5-try-with-resources-与关闭)
6. [编码与乱码](#6-编码与乱码)
7. [Files 工具类速查](#7-files-工具类速查)
8. [大文件处理](#8-大文件处理)
9. [属性文件 Properties](#9-属性文件-properties)
10. [CSV 导出与导入](#10-csv-导出与导入)
11. [二进制与对象序列化](#11-二进制与对象序列化)
12. [标准输入输出与重定向](#12-标准输入输出与重定向)
13. [随机读写](#13-随机读写)
14. [网络 I/O](#14-网络-io)
15. [实战：EMS 数据导出与配置读写](#15-实战ems-数据导出与配置读写)
16. [常见坑](#16-常见坑)
17. [速查表](#17-速查表)

---

## 1. 先分清：字节流与字符流

| | 字节流 | 字符流 |
| --- | --- | --- |
| 基类 | `InputStream` / `OutputStream` | `Reader` / `Writer` |
| 单位 | 字节（byte，8 位） | 字符（char，16 位） |
| 处理对象 | 任意文件：图片、jar、压缩包、二进制协议 | 文本：txt、csv、properties、java 源码 |
| 编码 | 不涉及，原样搬运 | 涉及，要指定 UTF-8 等 |
| 常用类 | `FileInputStream`、`FileOutputStream`、`BufferedInputStream` | `FileReader`、`FileWriter`、`BufferedReader`、`BufferedWriter` |

选择原则：

- 内容是**给人看的文字** → 字符流（并且显式指定 UTF-8）；
- 内容是**二进制数据**（图片、压缩包、序列化、网络二进制报文）→ 字节流；
- 不确定 → 字节流最安全。

类体系（简化）：

```
InputStream
├── FileInputStream        读文件
├── ByteArrayInputStream   读内存字节数组
├── BufferedInputStream    带缓冲，读得快
└── DataInputStream        读基本类型（int/double/...）

Reader
├── InputStreamReader      字节流 → 字符流（要指定编码）
├── FileReader             读文本文件（编码用系统默认，不推荐）
├── BufferedReader         带缓冲，按行读
└── StringReader           读字符串
```

---

## 2. File 与 Path

### 2.1 File：老式写法

```java
import java.io.File;

File file = new File("E:\\UP\\MySQL\\logs\\app.log");

System.out.println(file.exists());          // 是否存在
System.out.println(file.isFile());          // 是不是文件
System.out.println(file.isDirectory());     // 是不是目录
System.out.println(file.length());          // 字节数
System.out.println(file.getName());         // app.log
System.out.println(file.getAbsolutePath()); // 绝对路径
System.out.println(file.getParent());       // 上级目录

File dir = new File("E:\\UP\\MySQL\\logs");
dir.mkdirs();                               // 创建目录（含父目录）
```

### 2.2 Path：推荐的新写法

```java
import java.nio.file.Path;
import java.nio.file.Paths;

Path p = Path.of("E:", "UP", "MySQL", "logs", "app.log");   // JDK 11+
Path p2 = Paths.get("E:\\UP\\MySQL\\logs\\app.log");        // JDK 7 写法，也支持

System.out.println(p.toAbsolutePath());
System.out.println(p.getFileName());
System.out.println(p.getParent());
System.out.println(p.resolve("error.log"));   // 拼路径：E:\UP\MySQL\logs\app.log\error.log（注意是拼在后面的）
System.out.println(Path.of("logs", "app.log").toAbsolutePath());
```

跨平台路径拼接：

```java
// 不要写死反斜杠
String bad = "E:\\UP\\MySQL\\logs\\app.log";

// 用 Path 拼，Windows/Linux 都对
Path good = Path.of(System.getProperty("user.dir"), "logs", "app.log");
```

`File` 与 `Path` 互转：

```java
Path path = file.toPath();
File f = path.toFile();
```

### 2.3 遍历目录

```java
import java.nio.file.Files;

try (java.util.stream.Stream<Path> stream = Files.list(Path.of("logs"))) {
    stream.filter(Files::isRegularFile)
          .forEach(p -> System.out.println(p.getFileName()));
}

// 递归遍历（含子目录）
try (java.util.stream.Stream<Path> stream = Files.walk(Path.of("logs"))) {
    stream.filter(Files::isRegularFile)
          .forEach(System.out::println);
}
```

`Files.list` / `Files.walk` 返回的是 `Stream`，**必须用 try-with-resources 关闭**，否则目录句柄不释放。

---

## 3. 读文件

### 3.1 小文件一次读完

```java
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

Path file = Path.of("logs", "app.log");

// 读成字符串（JDK 11+）
String content = Files.readString(file, StandardCharsets.UTF_8);

// 读成行列表
List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

// 读成字节数组
byte[] bytes = Files.readAllBytes(file);
```

适合几 MB 以内的小文件。几十 MB 以上用下面的流式读法。

### 3.2 逐行读（最常用）

```java
try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
    String line;
    while ((line = reader.readLine()) != null) {     // null 表示文件结束
        if (!line.isBlank()) {
            System.out.println(line);
        }
    }
}
```

`readLine()` 会去掉行尾的 `\n` 或 `\r\n`，返回内容本身；文件为空时直接返回 `null`。

### 3.3 大文件流式读（不占内存）

```java
try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
    long count = lines.filter(l -> l.contains("ERROR")).count();
}
```

一行一行处理，内存占用恒定。注意 `Files.lines` 返回的 `Stream` 也要关闭。

### 3.4 老式字节流读法

```java
try (FileInputStream in = new FileInputStream("logs/app.log")) {
    byte[] buffer = new byte[8192];
    int len;
    while ((len = in.read(buffer)) != -1) {          // -1 才是结束
        System.out.write(buffer, 0, len);
    }
}
```

关键：`read` 返回 `-1` 才表示读完，`0` 不是结束。

---

## 4. 写文件

### 4.1 小文件一次写完

```java
// 字符串（JDK 11+，默认覆盖）
Files.writeString(file, "hello\n", StandardCharsets.UTF_8);

// 追加
Files.writeString(file, "new line\n", StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

// 字节数组
Files.write(file, "bytes".getBytes(StandardCharsets.UTF_8));
```

### 4.2 逐行写

```java
try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
    writer.write("第一行");
    writer.newLine();                       // 换行，跨平台
    writer.write("第二行");
    writer.newLine();
}
```

覆盖还是追加，看 `StandardOpenOption`：

| 组合 | 行为 |
| --- | --- |
| 不写（默认） | 覆盖：文件不存在则创建 |
| `CREATE, APPEND` | 追加到末尾 |
| `CREATE, TRUNCATE_EXISTING` | 覆盖（显式写法） |
| `CREATE_NEW` | 只能新建，已存在就报错 |

### 4.3 FileWriter 的坑

```java
// 不推荐：编码用系统默认，换台机器可能乱码
try (FileWriter w = new FileWriter("app.log", true)) { ... }

// 推荐：显式 UTF-8
try (Writer w = new OutputStreamWriter(
        new FileOutputStream("app.log", true), StandardCharsets.UTF_8)) { ... }
```

`new FileWriter(path, true)` 第二个参数 `true` 表示追加，但编码不可控。

---

## 5. try-with-resources 与关闭

不关闭的后果：文件被锁住删不掉、句柄泄漏、数据没写进磁盘。

```java
// 推荐：自动关闭，异常也会关
try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
     BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
    String line;
    while ((line = reader.readLine()) != null) {
        writer.write(line.toUpperCase());
        writer.newLine();
    }
} catch (IOException e) {
    System.out.println("读写失败：" + e.getMessage());
}
```

多资源时关闭顺序是「后声明先关闭」，和 `finally` 里手写顺序相反，符合习惯。

`PrintWriter` / `BufferedWriter` 内部有缓冲区，**不关闭或 `flush()` 就可能没写进去**：

```java
PrintWriter pw = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
pw.println("内容");
pw.flush();          // 不 flush 可能丢失
```

---

## 6. 编码与乱码

### 6.1 三个必须显式指定编码的地方

```java
// 读
Files.newBufferedReader(path, StandardCharsets.UTF_8)
new InputStreamReader(in, StandardCharsets.UTF_8)

// 写
Files.newBufferedWriter(path, StandardCharsets.UTF_8)
new OutputStreamWriter(out, StandardCharsets.UTF_8)

// 转换
new String(bytes, StandardCharsets.UTF_8)
```

不写编码就用平台默认值：中文 Windows 是 GBK，Linux 常是 UTF-8。同一份代码在两台机器上结果不同，就是这里出的问题。

### 6.2 常见乱码对照

| 现象 | 原因 |
| --- | --- |
| 中文变成 `æµ‹è¯•` 之类 | 用 UTF-8 写、用 GBK 读（或相反） |
| 中文变成 `????` | 编码不支持中文（如 ISO-8859-1） |
| Excel 打开 CSV 中文乱码 | 文件没带 UTF-8 BOM |
| 文件开头多了一个看不见的字符 | 读的时候没跳过 BOM |

### 6.3 UTF-8 BOM（给 Excel 用）

```java
try (Writer w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
    w.write('\uFEFF');          // 写一个 BOM，Excel 才会按 UTF-8 打开
    w.write("编号,温度");
    w.newLine();
}
```

读取时要跳过 BOM：

```java
String first = lines.get(0);
if (!first.isEmpty() && first.charAt(0) == '\uFEFF') {
    first = first.substring(1);
}
```

### 6.4 其它编码

国内老系统常见 GBK：

```java
import java.nio.charset.Charset;

Charset gbk = Charset.forName("GBK");
String text = new String(bytes, gbk);
```

优先用 UTF-8，只有对接老系统时再用 GBK。

---

## 7. Files 工具类速查

```java
Path p = Path.of("data", "ems.csv");

Files.exists(p);                     // 存在吗
Files.notExists(p);
Files.isDirectory(p);
Files.isRegularFile(p);
Files.size(p);                       // 字节数
Files.createFile(p);                 // 创建空文件（已存在会抛异常）
Files.createDirectories(p.getParent());  // 创建多级目录，已存在不报错
Files.delete(p);                     // 删除（不存在抛异常）
Files.deleteIfExists(p);             // 删除（不存在返回 false）
Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
Files.createTempFile("ems-", ".tmp");
Files.newInputStream(p);
Files.newOutputStream(p);
```

注意：`Files.delete` 与 `deleteIfExists` 的区别；`Files.createFile` 在文件已存在时抛 `FileAlreadyExistsException`，要创建文件用 `Files.write(p, new byte[0])` 更稳。

---

## 8. 大文件处理

### 8.1 别把大文件一次读进内存

```java
// 危险：几百 MB 的文件会直接 OOM
byte[] all = Files.readAllBytes(bigFile);

// 正确：逐行流式处理
try (BufferedReader reader = Files.newBufferedReader(bigFile, StandardCharsets.UTF_8)) {
    String line;
    while ((line = reader.readLine()) != null) {
        process(line);
    }
}
```

### 8.2 按固定字节块读写（复制文件）

```java
try (InputStream in = Files.newInputStream(src);
     OutputStream out = Files.newOutputStream(dst)) {
    byte[] buffer = new byte[64 * 1024];
    int len;
    while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
    }
}
```

或者直接用 `Files.copy(src, dst, REPLACE_EXISTING)`，底层已经优化过。

### 8.3 统计大文件行数

```java
try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
    long count = lines.count();
}
```

---

## 9. 属性文件 Properties

项目里的 `config.properties` 就是它，配合 `ConfigManager` 使用。自己读写也不难：

```java
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// 读
Properties prop = new Properties();
try (Reader reader = Files.newBufferedReader(Path.of("config.properties"), StandardCharsets.UTF_8)) {
    prop.load(reader);
}
String ip = prop.getProperty("db.ip", "127.0.0.1");
String port = prop.getProperty("db.port", "3306");

// 写（会覆盖整个文件）
try (Writer writer = Files.newBufferedWriter(Path.of("config.properties"), StandardCharsets.UTF_8)) {
    prop.setProperty("db.ip", "192.168.2.51");
    prop.store(writer, "Database Configuration");
}
```

两个注意点：

1. `Properties.store` 会把中文转成 `\u4F60\u597D` 这种转义形式，属于正常现象，`load` 时能还原；
2. 写文件是**整文件覆盖**，要保留原有键值，必须先把文件读进来再改。

---

## 10. CSV 导出与导入

### 10.1 导出（带 BOM，Excel 能直接打开）

```java
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 一行数据：id, ali, temp, humidity, time */
public static void exportCsv(Path target, List<String[]> rows) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
        writer.write('\uFEFF');                     // UTF-8 BOM，Excel 靠它识别编码
        writer.write("id,ali,temp,humidity,time");
        writer.newLine();
        for (String[] row : rows) {
            writer.write(joinCsv(row));
            writer.newLine();
        }
    }
}

/** 简单 CSV 拼接：内容含逗号或引号时用双引号包起来 */
private static String joinCsv(String[] row) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < row.length; i++) {
        if (i > 0) {
            sb.append(',');
        }
        String value = row[i] == null ? "" : row[i];
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            sb.append('"').append(value.replace("\"", "\"\"")).append('"');
        } else {
            sb.append(value);
        }
    }
    return sb.toString();
}
```

### 10.2 导入

```java
public static int importCsv(Path source) throws IOException {
    int count = 0;
    try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
        String line = reader.readLine();
        if (line != null && line.startsWith("\uFEFF")) {
            line = line.substring(1);               // 去掉 BOM
        }
        // 第一行是表头，跳过
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 4) {
                continue;                           // 格式不对，跳过
            }
            // 这里可以做入库：saveToDatabase(parts)
            count++;
        }
    }
    return count;
}
```

> 复杂 CSV（字段里带逗号、换行）需要完整解析器；自己写的简单版本只适合「数据本身不含逗号」的场景。

### 10.3 用 CSV 导出当前表数据（配合 JDBC）

```java
String sql = "SELECT id, ALI, Temp, Humidity, `Time` FROM ems ORDER BY id";
try (Connection con = openConnection();
     Statement st = con.createStatement();
     ResultSet rs = st.executeQuery(sql);
     BufferedWriter writer = Files.newBufferedWriter(Path.of("ems_export.csv"), StandardCharsets.UTF_8)) {

    writer.write('\uFEFF');
    writer.write("id,ALI,Temp,Humidity,Time");
    writer.newLine();
    while (rs.next()) {
        writer.write(rs.getInt("id") + "," + rs.getDouble("ALI") + ","
                + rs.getDouble("Temp") + "," + rs.getDouble("Humidity") + ","
                + rs.getTimestamp("Time"));
        writer.newLine();
    }
}
```

---

## 11. 二进制与对象序列化

### 11.1 基本类型读写

```java
// 写
try (DataOutputStream out = new DataOutputStream(
        new BufferedOutputStream(Files.newOutputStream(Path.of("data.bin"))))) {
    out.writeInt(1001);
    out.writeDouble(26.4);
    out.writeUTF("device-1");
}

// 读（顺序必须与写一致）
try (DataInputStream in = new DataInputStream(
        new BufferedInputStream(Files.newInputStream(Path.of("data.bin"))))) {
    int id = in.readInt();
    double temp = in.readDouble();
    String name = in.readUTF();
}
```

### 11.2 对象序列化

```java
import java.io.Serializable;

public class EmsRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private double temp;
    // getter / setter
}

// 写
try (ObjectOutputStream out = new ObjectOutputStream(
        Files.newOutputStream(Path.of("records.obj")))) {
    out.writeObject(new EmsRecord());
}

// 读
try (ObjectInputStream in = new ObjectInputStream(
        Files.newInputStream(Path.of("records.obj")))) {
    EmsRecord r = (EmsRecord) in.readObject();
}
```

注意：

- 类必须 `implements Serializable`，`serialVersionUID` 建议显式写；
- 序列化格式与类版本绑定，改了字段可能反序列化失败；
- 不要反序列化不可信来源的数据（安全风险）；
- 需要长期存储或跨语言时，用 JSON 而不是 Java 序列化。

---

## 12. 标准输入输出与重定向

```java
// 输出
System.out.println("普通输出");
System.err.println("错误输出");        // 通常也被打印到控制台，但可以单独重定向

// 输入
Scanner scanner = new Scanner(System.in);
String line = scanner.nextLine();
```

读控制台更稳的写法（指定 UTF-8）：

```java
BufferedReader console = new BufferedReader(
        new InputStreamReader(System.in, StandardCharsets.UTF_8));
String line = console.readLine();
```

把日志重定向到文件：

```java
try (PrintStream fileOut = new PrintStream(
        Files.newOutputStream(Path.of("logs", "app.log"), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
        true, StandardCharsets.UTF_8.name())) {
    System.setOut(fileOut);            // 之后 println 都写文件
    System.out.println("程序启动");
}
```

项目里已有 `LogUtil` 写日志，一般不需要重定向；调试控制台程序时可以临时用。

---

## 13. 随机读写

需要「跳到文件中间读/写」时用 `RandomAccessFile`：

```java
import java.io.RandomAccessFile;

try (RandomAccessFile raf = new RandomAccessFile("data.bin", "rw")) {
    raf.seek(100);              // 跳到第 100 字节
    int value = raf.readInt();  // 读 4 字节
    raf.seek(100);
    raf.writeInt(value + 1);    // 原地改写
}
```

模式：`"r"` 只读、`"rw"` 读写、`"rws"` 每次写都同步到磁盘。

超大文件（GB 级）可以用内存映射，读起来接近内存速度：

```java
try (FileChannel channel = FileChannel.open(Path.of("big.bin"), StandardOpenOption.READ)) {
    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    byte b = buffer.get(0);
}
```

日常项目基本用不到，知道有这回事即可。

---

## 14. 网络 I/O

`Socket` 拿到的也是 `InputStream` / `OutputStream`，所以前面所有读写技巧都能直接用：

```java
// 套一层缓冲，按行读
BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
PrintWriter out = new PrintWriter(
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
```

区别只有一个：文件读到末尾返回 `-1`/`null`；网络流在对方不关闭时会**一直阻塞等待**，所以要么约定好结束标志，要么设超时（`socket.setSoTimeout(...)`）。协议设计见《网络通信使用手册》第 5、6 章。

---

## 15. 实战：EMS 数据导出与配置读写

把「查库 → 写 CSV → 读配置 → 追加日志」串起来：

```java
package com.github.rayman717.io;

import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.ConfigManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 把 ems 表导出为 CSV（Excel 可直接打开），并写一行日志。
 */
public class EmsCsvExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        Path csv = Path.of("ems_export.csv");
        Path log = Path.of("logs", "export.log");

        try {
            Files.createDirectories(log.getParent());      // 确保 logs 目录存在
            int rows = export(csv);
            appendLog(log, "导出成功，共 " + rows + " 行 → " + csv.toAbsolutePath());
            System.out.println("导出完成：" + rows + " 行");
        } catch (Exception e) {
            try {
                appendLog(log, "导出失败：" + e.getMessage());
            } catch (IOException ignored) {
                // 日志也写不了就只能打印
            }
            System.out.println("导出失败：" + e.getMessage());
        }
    }

    /** 查询 ems 表并写成 CSV */
    private static int export(Path target) throws Exception {
        DbConfig config = ConfigManager.getDbConfig();
        String url = "jdbc:mysql://" + config.ip + ":3306/" + config.dbName
                + "?useSSL=false&characterEncoding=utf8";
        String sql = "SELECT id, ALI, Temp, Humidity, `Time` FROM ems ORDER BY id";

        int count = 0;
        try (Connection con = DriverManager.getConnection(url, config.user, config.password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql);
             BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {

            writer.write('\uFEFF');                        // BOM，Excel 识别 UTF-8
            writer.write("id,ALI,Temp,Humidity,Time");
            writer.newLine();

            while (rs.next()) {
                writer.write(rs.getInt("id") + ","
                        + rs.getDouble("ALI") + ","
                        + rs.getDouble("Temp") + ","
                        + rs.getDouble("Humidity") + ","
                        + rs.getTimestamp("Time"));
                writer.newLine();
                count++;
            }
        }
        return count;
    }

    /** 追加一行日志 */
    private static void appendLog(Path log, String message) throws IOException {
        String line = LocalDateTime.now().format(FMT) + "  " + message + System.lineSeparator();
        Files.writeString(log, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
```

运行后：

- 项目根目录多出 `ems_export.csv`，用 Excel 打开中文和数字都正常；
- `logs/export.log` 里多一行带时间的记录；
- 数据库连接参数来自 `config.properties`，和项目其它代码一致。

---

## 16. 常见坑

| 坑 | 现象 | 正确处理 |
| --- | --- | --- |
| 忘记关闭流 | 文件被占用、删不掉、句柄泄漏 | 全部用 try-with-resources |
| 用默认编码读写 | 换台机器中文乱码 | 显式 `StandardCharsets.UTF_8` |
| `FileReader` / `FileWriter` | 编码不可控 | 用 `Files.newBufferedReader` 或转换流 |
| 不 `flush` | 写的内容没了 | 关闭或 `flush()` |
| `read` 返回 0 当结束 | 死循环 | 判断 `-1` |
| `readLine` 返回 null 当空行 | 判断错误 | null 是文件结束，空行是 `""` |
| 大文件 `readAllBytes` | OOM | 逐行流式读 |
| `Files.lines` / `Files.list` 不关闭 | 句柄泄漏 | try-with-resources |
| 路径写死 `\` | Linux 上路径错 | `Path.of(...)` 拼接 |
| 追加没加 `APPEND` | 每次覆盖 | `StandardOpenOption.APPEND` |
| `new FileWriter(path)` 覆盖了原文件 | 追加变覆盖 | 第二个参数 `true` 或用 `APPEND` |
| CSV 中文 Excel 乱码 | 没 BOM | 先写 `\uFEFF` |
| `Properties.store` 中文变 `\uXXXX` | 看起来像乱码 | 正常现象，`load` 能还原 |
| 序列化类加字段后反序列化失败 | `InvalidClassException` | 显式 `serialVersionUID`，或改用 JSON |
| 用 `File.delete()` 删不掉 | 文件被流占用 | 先关闭流再删 |
| 相对路径找不到文件 | 工作目录不是项目根目录 | 打印 `Paths.get(".").toAbsolutePath()` 确认 |

排查「文件去哪了」：

```java
System.out.println(Path.of(".").toAbsolutePath());   // 当前工作目录
System.out.println(Path.of("ems_export.csv").toAbsolutePath());
```

---

## 17. 速查表

### 17.1 读

| 我想…… | 代码 |
| --- | --- |
| 一次读成字符串 | `Files.readString(path, UTF_8)` |
| 一次读成行列表 | `Files.readAllLines(path, UTF_8)` |
| 逐行读大文件 | `try (BufferedReader r = Files.newBufferedReader(path, UTF_8)) { while ((l = r.readLine()) != null) ... }` |
| 流式处理行 | `try (Stream<String> s = Files.lines(path, UTF_8)) { ... }` |
| 读字节 | `Files.readAllBytes(path)` |
| 读图片/二进制 | `Files.newInputStream(path)` |

### 17.2 写

| 我想…… | 代码 |
| --- | --- |
| 覆盖写字符串 | `Files.writeString(path, text, UTF_8)` |
| 追加写 | `Files.writeString(path, text, UTF_8, CREATE, APPEND)` |
| 逐行写 | `try (BufferedWriter w = Files.newBufferedWriter(path, UTF_8)) { w.write(...); w.newLine(); }` |
| 写字节 | `Files.write(path, bytes)` |
| 写 CSV 给 Excel | 先 `w.write('\uFEFF')` |

### 17.3 文件与目录

| 我想…… | 代码 |
| --- | --- |
| 判断存在 | `Files.exists(path)` |
| 大小 | `Files.size(path)` |
| 建目录 | `Files.createDirectories(dir)` |
| 删除 | `Files.deleteIfExists(path)` |
| 复制 | `Files.copy(src, dst, REPLACE_EXISTING)` |
| 移动/改名 | `Files.move(src, dst, REPLACE_EXISTING)` |
| 遍历 | `Files.list(dir)` / `Files.walk(dir)` |
| 拼路径 | `Path.of("logs", "app.log")` |

### 17.4 编码对照

| 场景 | 编码 |
| --- | --- |
| 项目内所有文本文件 | UTF-8 |
| 给 Excel 的 CSV | UTF-8 + BOM |
| 对接国内老系统 | GBK（`Charset.forName("GBK")`） |
| 二进制/图片 | 不涉及编码，用字节流 |
