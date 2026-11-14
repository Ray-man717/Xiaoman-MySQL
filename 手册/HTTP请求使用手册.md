# HTTP 请求详解（网络通信附加篇）

> 本手册是 [网络通信使用手册](网络通信使用手册.html) 的**附加篇**，专门把 HTTP 讲透：
> 客户端怎么发请求，服务端怎么收请求。
> 基础的 TCP / UDP 部分（Socket、粘包、多客户端并发）请看网络通信手册的第 3–8 章，这里不再重复。
> 环境：JDK 17，客户端主体用 `java.net.http.HttpClient`，服务端主体用 JDK 自带的 `com.sun.net.httpserver`。

---

## 目录

1. [一次请求的完整过程](#1-一次请求的完整过程)
2. [URL 的组成](#2-url-的组成)
3. [请求方法](#3-请求方法)
4. [状态码](#4-状态码)
5. [请求头与响应头](#5-请求头与响应头)
6. [请求体格式](#6-请求体格式)
7. [HttpURLConnection 完整写法](#7-httpurlconnection-完整写法)
8. [HttpClient 完整写法（推荐）](#8-httpclient-完整写法推荐)
9. [下载文件与上传](#9-下载文件与上传)
10. [HTTPS 与证书](#10-https-与证书)
11. [调试 HTTP](#11-调试-http)
12. [实战：EmsApiClient](#12-实战emsapiclient)
13. [常见坑](#13-常见坑)
14. [HTTP 速查](#14-http-速查)
15. [HTTP 服务端怎么写](#15-http-服务端怎么写)
16. [附：和网络通信手册的对应关系](#16-附和网络通信手册的对应关系)

---

## 1. 一次请求的完整过程

HTTP 是「一问一答」：客户端发一个请求，服务端回一个响应，然后连接可能复用也可能关闭。

```
客户端                     服务端
  │  ① 建立 TCP 连接（默认 80/443）  │
  │ ──────────────────────────────► │
  │  ② 发送请求报文                 │
  │ ──────────────────────────────► │
  │  ③ 服务端处理（查库、写数据）    │
  │  ④ 返回响应报文                 │
  │ ◄────────────────────────────── │
  │  ⑤ 连接保持（keep-alive）或关闭  │
```

一个**请求报文**由四部分组成：

```
POST /api/ems/records HTTP/1.1          ← 请求行：方法 + 路径 + 协议版本
Host: 127.0.0.1:8080                    ← 请求头（每行一个 key: value）
Content-Type: application/json; charset=UTF-8
Authorization: Bearer eyJhbGciOi...
Content-Length: 42
Connection: keep-alive
                                        ← 空行：头和体之间的分界
{"ali":300.5,"temp":26.4,"humidity":55} ← 请求体（GET 通常没有）
```

一个**响应报文**长这样：

```
HTTP/1.1 201 Created                    ← 状态行：协议版本 + 状态码 + 短语
Content-Type: application/json; charset=UTF-8
Content-Length: 27
Date: Thu, 01 Jan 2026 08:00:00 GMT
                                        ← 空行
{"id":1001,"status":"ok"}               ← 响应体
```

记住一个对应关系：

| 位置 | 请求 | 响应 |
| --- | --- | --- |
| 第一行 | 方法 + 路径 + 版本 | 状态码 + 短语 |
| 头部 | 我要什么、我是谁 | 我给了什么、怎么给 |
| 主体 | 提交的数据 | 返回的数据 |

## 2. URL 的组成

```
http://127.0.0.1:8080/api/ems/latest?limit=10&device=ems-01#top
└┬─┘   └────┬────┘└┬┘└──────┬──────┘└───┬────┘└─┬─┘
协议      主机    端口    路径          查询参数  片段
```

| 部分 | 说明 | 例子 |
| --- | --- | --- |
| 协议 | `http` 或 `https` | `https` |
| 主机 | 域名或 IP | `api.example.com` |
| 端口 | 不写就用默认 80 / 443 | `:8080` |
| 路径 | 资源位置，区分大小写 | `/api/ems/latest` |
| 查询参数 | `key=value`，多个用 `&` 连接 | `?limit=10&device=ems-01` |
| 片段 | 锚点，客户端用，不发给服务端 | `#top` |

查询参数里有中文、空格、特殊字符时**必须编码**：

```java
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

String keyword = "温度 报警";
String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);   // %E6%B8%A9%E5%BA%A6+%E6%8A%A5%E8%AD%A6
String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);   // 温度 报警

String url = "http://127.0.0.1:8080/api/ems/search?keyword=" + encoded;
```

注意 `URLEncoder` 是把**表单参数**编码成 `application/x-www-form-urlencoded` 格式：空格变 `+`，中文变 `%XX`。拼进路径时如果不能用 `+`，把 `+` 换成 `%20`。

## 3. 请求方法

| 方法 | 用途 | 有请求体 | 安全 | 幂等 |
| --- | --- | --- | --- | --- |
| `GET` | 查询数据 | 一般没有 | 是 | 是 |
| `POST` | 新增数据、提交动作 | 有 | 否 | 否 |
| `PUT` | 整体替换某个资源 | 有 | 否 | 是 |
| `PATCH` | 局部更新 | 有 | 否 | 否 |
| `DELETE` | 删除资源 | 一般没有 | 否 | 是 |
| `HEAD` | 只要响应头，不要体 | 没有 | 是 | 是 |
| `OPTIONS` | 问服务端支持哪些方法（跨域预检也用它） | 没有 | 是 | 是 |

两个概念：

- **安全**：方法不改变服务端数据（GET 不会改数据）；
- **幂等**：执行一次和执行 N 次结果一样（`PUT` 设成同一个值，重复调结果相同）。

POST 不幂等，所以**用 POST 做「失败自动重试」要小心重复提交**：要么服务端按业务唯一键去重，要么每次带一个幂等键（requestId）。

选择建议：

| 想做的事 | 方法 |
| --- | --- |
| 查最新 10 条数据 | `GET /api/ems/latest?limit=10` |
| 新增一条监测记录 | `POST /api/ems` |
| 修改某条记录的温度 | `PUT /api/ems/1001` 或 `PATCH` |
| 删除某条记录 | `DELETE /api/ems/1001` |

## 4. 状态码

状态码第一位决定大类：

| 范围 | 含义 | 谁的问题 |
| --- | --- | --- |
| `1xx` | 信息，继续处理（少见） | 协议层 |
| `2xx` | 成功 | 没问题 |
| `3xx` | 重定向，资源在别处 | 需要跟随 |
| `4xx` | 请求有问题 | **客户端**（你写错了） |
| `5xx` | 服务端出错 | **服务端**（对方的问题） |

常见状态码含义：

| 码 | 含义 | 常见原因 |
| --- | --- | --- |
| `200` | OK | 正常返回 |
| `201` | Created | 新增成功，通常返回新资源的 id |
| `204` | No Content | 成功但没有响应体（DELETE 常用） |
| `301` / `302` | 永久 / 临时重定向 | 地址变了，需要跟随 |
| `304` | Not Modified | 缓存没过期，没有新内容 |
| `400` | Bad Request | 参数格式错、JSON 语法错 |
| `401` | Unauthorized | 没带 Token、Token 过期或无效 |
| `403` | Forbidden | 认证过了但没权限 |
| `404` | Not Found | 路径写错、资源不存在 |
| `405` | Method Not Allowed | 用 GET 调了只支持 POST 的接口 |
| `409` | Conflict | 重复创建、版本冲突 |
| `413` | Payload Too Large | 请求体超限 |
| `415` | Unsupported Media Type | Content-Type 不对（比如发 JSON 却写 `text/plain`） |
| `422` | Unprocessable Entity | 格式对但业务校验不通过 |
| `429` | Too Many Requests | 限流了，等一会儿再试 |
| `500` | Internal Server Error | 服务端代码抛异常了 |
| `502` / `504` | Bad Gateway / Gateway Timeout | 网关后面的服务挂了或超时 |
| `503` | Service Unavailable | 服务端维护中或过载 |

客户端处理原则：

```java
int code = resp.statusCode();

if (code >= 200 && code < 300) {
    // 成功：读 body
} else if (code >= 300 && code < 400) {
    // 重定向：HttpClient 默认会跟，HttpURLConnection 默认也跟
} else if (code >= 400 && code < 500) {
    // 我的问题：检查 URL、参数、Content-Type、Token
    // 401/403 尤其要先看鉴权
} else if (code >= 500) {
    // 对方的问题：可以重试（但注意幂等）
}
```

## 5. 请求头与响应头

**常用请求头**（客户端发给服务端）：

| 头 | 作用 | 例子 |
| --- | --- | --- |
| `Host` | 目标主机 | `127.0.0.1:8080` |
| `Content-Type` | 请求体的格式 | `application/json; charset=UTF-8` |
| `Content-Length` | 请求体字节数（自动算） | `42` |
| `Accept` | 我能接受什么响应格式 | `application/json` |
| `Authorization` | 身份凭证 | `Bearer eyJ...` |
| `User-Agent` | 客户端标识 | `EMS-Client/1.0` |
| `Connection` | 连接是否复用 | `keep-alive` |
| `Accept-Encoding` | 能接受的压缩方式 | `gzip` |
| `Cookie` | 会话信息 | `SESSION=abc123` |

**常用响应头**（服务端返回）：

| 头 | 作用 |
| --- | --- |
| `Content-Type` | 响应体格式，如 `application/json` |
| `Content-Length` | 响应体字节数 |
| `Set-Cookie` | 服务端设置的 Cookie |
| `Cache-Control` | 缓存策略 |
| `Location` | 重定向目标地址 |
| `WWW-Authenticate` | 401 时提示如何认证 |
| `Retry-After` | 429/503 时建议多久后重试 |

`Content-Type` 的值决定服务端怎么解析请求体：

| 值 | 请求体长什么样 | 用在哪 |
| --- | --- | --- |
| `application/json` | `{"key":"value"}` | 现代 API 最常见 |
| `application/x-www-form-urlencoded` | `key1=value1&key2=value2` | 传统表单提交、登录接口 |
| `multipart/form-data; boundary=xxx` | 分段二进制 | 文件上传 |
| `text/plain` | 纯文本 | 简单上报 |
| `application/octet-stream` | 任意二进制 | 上传文件流 |

鉴权头三种常见写法：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...   ← JWT / OAuth2 令牌，最常见
Authorization: Basic cm9vdDoxMjM0NTY=            ← 用户名:密码 的 Base64
X-API-Key: 8f14e45fceea167a5a36dedd4bea2543     ← 自定义 API Key（不少厂商这么用）
```

Basic 的生成方式：

```java
String auth = "root:123456";
String basic = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
```

## 6. 请求体格式

**JSON（最常用）**：

```java
String json = "{\"ali\":300.5,\"temp\":26.4,\"humidity\":55.0}";
// 或者用 String.format 拼
String json2 = String.format("{\"ali\":%.1f,\"temp\":%.1f,\"humidity\":%.1f}", 300.5, 26.4, 55.0);
```

手工拼 JSON 要转义双引号；数据里本身有引号、反斜杠时容易出错，复杂结构请用 Gson。

**表单（`application/x-www-form-urlencoded`）**：

```java
String form = "user=" + URLEncoder.encode("admin", StandardCharsets.UTF_8)
        + "&password=" + URLEncoder.encode("123456", StandardCharsets.UTF_8);
```

**multipart 文件上传**（手工构造，了解结构即可）：

```
POST /api/upload HTTP/1.1
Content-Type: multipart/form-data; boundary=----EmsBoundary123

------EmsBoundary123
Content-Disposition: form-data; name="file"; filename="data.csv"
Content-Type: text/csv

<文件二进制内容>
------EmsBoundary123--
```

```java
String boundary = "----EmsBoundary" + System.currentTimeMillis();
byte[] fileBytes = Files.readAllBytes(Path.of("ems_export.csv"));

StringBuilder head = new StringBuilder();
head.append("--").append(boundary).append("\r\n");
head.append("Content-Disposition: form-data; name=\"file\"; filename=\"ems_export.csv\"\r\n");
head.append("Content-Type: text/csv\r\n\r\n");
String tail = "\r\n--" + boundary + "--\r\n";

ByteArrayOutputStream body = new ByteArrayOutputStream();
body.write(head.toString().getBytes(StandardCharsets.UTF_8));
body.write(fileBytes);
body.write(tail.getBytes(StandardCharsets.UTF_8));
```

实际项目里上传文件建议用 OkHttp、Apache HttpClient 这类库，它们把 multipart 封装好了；JDK 自带的需要自己拼。

**空体**：GET、DELETE 一般没有请求体，Java 里用 `BodyPublishers.noBody()`。

## 7. HttpURLConnection 完整写法

JDK 8 就有的老写法，兼容性最好，但代码偏啰嗦。

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** GET 请求，返回响应体文本；非 2xx 时抛出带错误信息的异常 */
public static String httpGet(String url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(3000);          // 连接超时
    conn.setReadTimeout(5000);             // 读取超时
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestProperty("User-Agent", "EMS-Client/1.0");

    int code = conn.getResponseCode();
    boolean ok = code >= 200 && code < 300;
    // 4xx/5xx 要用 getErrorStream，getInputStream 会抛 IOException
    InputStream in = ok ? conn.getInputStream() : conn.getErrorStream();

    StringBuilder sb = new StringBuilder();
    if (in != null) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
    }
    conn.disconnect();                     // 释放连接

    if (!ok) {
        throw new IOException("HTTP " + code + "：" + sb);
    }
    return sb.toString();
}
```

POST JSON：

```java
public static String httpPostJson(String url, String json) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);                // 允许写请求体
    conn.setConnectTimeout(3000);
    conn.setReadTimeout(5000);
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    conn.setRequestProperty("Accept", "application/json");

    try (OutputStream out = conn.getOutputStream()) {
        out.write(json.getBytes(StandardCharsets.UTF_8));
    }

    int code = conn.getResponseCode();
    InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();

    StringBuilder sb = new StringBuilder();
    if (in != null) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
    }
    conn.disconnect();

    if (code >= 400) {
        throw new IOException("HTTP " + code + "：" + sb);
    }
    return sb.toString();
}
```

HttpURLConnection 的注意点：

| 注意 | 说明 |
| --- | --- |
| 不设超时 | 默认超时是「永久」，网络异常时线程会一直挂着 |
| 不读流就断开 | 连接不会真正释放，高并发时句柄耗尽 |
| 4xx/5xx 用 `getInputStream` | 抛 `FileNotFoundException`，应该用 `getErrorStream` |
| 默认跟随重定向 | `setInstanceFollowRedirects(false)` 可关闭 |
| `setDoOutput(true)` 会改变方法 | 如果没有显式 `setRequestMethod`，会被当成 POST |
| 连接复用 | 读完响应并 `disconnect()`（或读完流）后才能复用 |

## 8. HttpClient 完整写法（推荐）

JDK 11+ 的 `java.net.http.HttpClient`，代码短、支持异步和 HTTP/2，**线程安全可以复用**。

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class HttpDemo {
    public static void main(String[] args) throws Exception {
        // 客户端：连接超时、跟随重定向、强制 HTTP/1.1（兼容老服务端）
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // GET
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8080/api/ems/latest?limit=10"))
                .timeout(Duration.ofSeconds(5))         // 单次请求超时
                .header("Accept", "application/json")
                .header("Authorization", "Bearer your-token")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(get,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        System.out.println("状态码：" + resp.statusCode());
        System.out.println("响应头：" + resp.headers().map());
        System.out.println("响应体：" + resp.body());
    }
}
```

POST JSON：

```java
String json = "{\"ali\":300.5,\"temp\":26.4,\"humidity\":55.0}";

HttpRequest post = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:8080/api/ems"))
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Authorization", "Bearer your-token")
        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .build();

HttpResponse<String> resp = client.send(post,
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
```

PUT / PATCH / DELETE：

```java
HttpRequest put = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:8080/api/ems/1001"))
        .header("Content-Type", "application/json; charset=UTF-8")
        .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
        .build();

HttpRequest delete = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:8080/api/ems/1001"))
        .DELETE()
        .build();
```

异步（不阻塞当前线程）：

```java
client.sendAsync(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
      .thenAccept(r -> System.out.println("异步结果：" + r.statusCode()))
      .exceptionally(ex -> {
          System.out.println("异步失败：" + ex.getCause());
          return null;
      });
```

两种写法的关键差异：

| | `HttpURLConnection` | `HttpClient` |
| --- | --- | --- |
| JDK 版本 | 8+ | 11+ |
| 4xx/5xx 行为 | `getInputStream` 抛异常，要读 `getErrorStream` | **不抛异常**，正常返回状态码，自己判断 |
| 超时 | 连接超时 + 读超时，分开设 | 连接超时（builder）+ 请求超时（request） |
| 异步 | 要自己开线程 | `sendAsync` |
| HTTP/2 | 不支持 | 支持 |
| 线程安全 | 每个请求一个连接对象 | `HttpClient` 可复用，线程安全 |
| 推荐度 | 老项目兼容 | 新项目首选 |

**最容易被忽略的差异**：`HttpClient` 收到 404、500 不会抛异常，一定要自己判断 `statusCode()`。

## 9. 下载文件与上传

**下载到文件**：

```java
HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:8080/api/ems/export"))
        .build();

Path target = Path.of("ems_export.csv");
Files.deleteIfExists(target);      // ofFile 要求目标不存在，否则抛 FileAlreadyExistsException
HttpResponse<Path> resp = client.send(req, HttpResponse.BodyHandlers.ofFile(target));
System.out.println("下载完成：" + resp.body().toAbsolutePath() + "，大小 " + Files.size(target));
```

**下载到内存**（小文件）：`HttpResponse.BodyHandlers.ofByteArray()`。

**上传文件（原始字节）**：

```java
HttpRequest upload = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:8080/api/upload"))
        .header("Content-Type", "application/octet-stream")
        .POST(HttpRequest.BodyPublishers.ofFile(Path.of("ems_export.csv")))
        .build();
```

**上传文件（multipart 表单）**：JDK 没有现成的 multipart 支持，按 9.6 节手工拼 body，然后：

```java
.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
```

## 10. HTTPS 与证书

`https://` 的 URL 用同样的代码即可，JDK 会自动做 TLS 握手。前提是服务端证书能被 JDK 信任。

常见报错：

```
javax.net.ssl.SSLHandshakeException: PKIX path building failed:
sun.security.provider.certpath.SunCertPathBuilderException:
unable to find valid certification path to requested target
```

含义：证书链不受信任（自签证书、企业内网证书、证书过期）。处理方式：

1. 生产环境：用正规 CA 证书（Let's Encrypt 等免费）；
2. 内网自签：把服务端证书导入 JDK 的信任库：

```bash
keytool -importcert -alias ems-server -file server.crt -keystore "%JAVA_HOME%\lib\security\cacerts"
```

3. 测试环境临时跳过校验：**只用于本地调试**，不要写进正式代码：

```java
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;

SSLContext ctx = SSLContext.getInstance("TLS");
ctx.init(null, new TrustManager[]{ new X509TrustManager() {
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
} }, new SecureRandom());

HttpClient client = HttpClient.newBuilder().sslContext(ctx).build();
```

另外证书过期、域名不匹配（`No name matching ... found`）也会报 `SSLHandshakeException`，先检查证书本身。

## 11. 调试 HTTP

写之前先用命令行把接口跑通，比在 Java 里调快得多。

Windows 下 `curl` 是 `Invoke-WebRequest` 的别名，要用真 curl 得写 `curl.exe`：

```bash
# GET，显示请求和响应头
curl.exe -v "http://127.0.0.1:8080/api/ems/latest?limit=10"

# 带 Token
curl.exe -H "Authorization: Bearer your-token" "http://127.0.0.1:8080/api/ems/latest"

# POST JSON
curl.exe -X POST "http://127.0.0.1:8080/api/ems" ^
  -H "Content-Type: application/json; charset=UTF-8" ^
  -d "{\"ali\":300.5,\"temp\":26.4,\"humidity\":55.0}"

# 只看状态码
curl.exe -o NUL -s -w "%{http_code}" "http://127.0.0.1:8080/api/ems/latest"

# 表格化输出（方便看 JSON）
curl.exe -s "http://127.0.0.1:8080/api/ems/latest" | python -m json.tool
```

PowerShell 原生写法（不用 curl）：

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/ems/latest?limit=10" -Method Get |
    Format-Table

Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/ems" -Method Post `
    -ContentType "application/json; charset=UTF-8" `
    -Body '{"ali":300.5,"temp":26.4,"humidity":55.0}'
```

浏览器里按 F12 → Network 面板，能直接看到每个请求的方法、状态码、请求头、请求体、响应内容，是排查「参数发错了还是服务端处理错了」的最快途径。

## 12. 实战：EmsApiClient

把上面的内容封装成一个能直接用的客户端：带 Token、超时、重试、状态码判断、错误信息完整。

```java
package com.github.rayman717.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * EMS 云平台 HTTP 客户端。
 * <p>
 * 统一处理：鉴权头、超时、JSON 编码、状态码判断、失败重试。
 */
public class EmsApiClient {

    private final HttpClient client;
    private final String baseUrl;
    private final String token;

    public EmsApiClient(String baseUrl, String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /** 上报一条监测数据，返回服务端响应（通常是带 id 的 JSON） */
    public String report(double ali, double temp, double humidity) throws IOException {
        String json = String.format("{\"ali\":%.1f,\"temp\":%.1f,\"humidity\":%.1f}",
                ali, temp, humidity);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ems"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return execute(request, 3);
    }

    /** 查询最新数据 */
    public String latest(int limit) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ems/latest?limit=" + limit))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return execute(request, 2);
    }

    /** 统一发送：2xx 返回 body，4xx 直接报错，5xx 和网络异常重试 */
    private String execute(HttpRequest request, int maxAttempts) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> resp = client.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = resp.statusCode();

                if (code >= 200 && code < 300) {
                    return resp.body();
                }
                if (code >= 400 && code < 500) {
                    // 客户端错误重试也没用，直接抛
                    throw new IOException("请求被拒绝 HTTP " + code + "：" + resp.body());
                }
                // 5xx 可以重试
                throw new IOException("服务端错误 HTTP " + code + "：" + resp.body());
            } catch (IOException e) {
                last = e;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(500L * attempt);   // 退避：0.5s、1s、1.5s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("请求被中断", e);
            }
        }
        throw last == null ? new IOException("请求失败") : last;
    }
}
```

使用：

```java
EmsApiClient api = new EmsApiClient("http://127.0.0.1:8080", "your-token");
String result = api.report(300.5, 26.4, 55.0);
System.out.println("上报成功：" + result);
```

配合《Swing 使用手册》：网络请求放在 `SwingWorker` 的 `doInBackground()` 里，`done()` 里更新界面，避免卡死。

## 13. 常见坑

| 坑 | 现象 | 解决 |
| --- | --- | --- |
| 没设超时 | 网络异常时线程永久阻塞 | 连接超时 + 请求超时都要设 |
| HttpClient 不判断状态码 | 404 也当成功了 | 显式判断 `statusCode() / 100 != 2` |
| HttpURLConnection 读 4xx 用 `getInputStream` | 抛 `FileNotFoundException` | 用 `getErrorStream()` |
| Content-Type 写错 | 服务端 415 或解析失败 | JSON 用 `application/json`，表单用 `x-www-form-urlencoded` |
| JSON 手拼漏转义 | 400 Bad Request | 用 `String.format` 或 Gson |
| 中文乱码 | 响应体里有 `????` | 读写都用 UTF-8，别依赖平台默认 |
| URL 参数没编码 | 400 或参数值不对 | `URLEncoder.encode(..., UTF_8)` |
| 忘记 `disconnect` 或读流 | 连接泄漏、句柄耗尽 | 读完后 `disconnect()`，或 try-with-resources |
| 重定向不跟随 | 拿到 301/302 当失败 | 默认跟随；要手动处理看 `Location` 头 |
| 用 POST 重试导致重复数据 | 库里多出重复记录 | 服务端去重或带幂等键 |
| 代理/防火墙 | 连接超时 | 检查系统代理、`-Dhttp.proxyHost` 参数 |
| HTTPS 证书不受信任 | `SSLHandshakeException` | 用正规证书，或把证书导入 JDK 信任库 |
| 响应体太大一次读进内存 | OOM | 用 `BodyHandlers.ofFile` 流式落盘 |
| 请求头里有中文 | 报错或被截断 | 头里只放 ASCII，中文放 body |

## 14. HTTP 速查

| 我想…… | 写法 |
| --- | --- |
| 发 GET | `.GET()` 或 `HttpRequest.newBuilder(uri).GET().build()` |
| 发 POST JSON | `.header("Content-Type","application/json").POST(BodyPublishers.ofString(json, UTF_8))` |
| 发 PUT | `.PUT(BodyPublishers.ofString(json, UTF_8))` |
| 发 DELETE | `.DELETE()` |
| 带 Token | `.header("Authorization", "Bearer " + token)` |
| 取状态码 | `resp.statusCode()` |
| 取响应体 | `resp.body()` |
| 取响应头 | `resp.headers().firstValue("Content-Type").orElse("")` |
| 下载到文件 | `BodyHandlers.ofFile(path)` |
| 上传文件 | `BodyPublishers.ofFile(path)` |
| 设超时 | builder 里 `connectTimeout`，request 里 `timeout` |
| 判断成功 | `resp.statusCode() / 100 == 2` |
| 401 怎么查 | 看 Token 是否过期、格式是否 `Bearer ` 开头 |
| 415 怎么查 | 看 Content-Type 是否与 body 格式匹配 |
## 15. HTTP 服务端怎么写

前面的章节都是客户端视角：**我发请求，别人回**。这一章反过来：**别人发请求，我来回**。

设备通过 HTTP 上报数据、Swing 客户端通过 HTTP 拉数据，都需要一个 HTTP 服务端。

### 15.1 三种做法

| 做法 | 依赖 | 代码量 | 适合 |
| --- | --- | --- | --- |
| JDK 自带 `com.sun.net.httpserver.HttpServer` | 无，JDK 自带 | 少 | 学习、课程项目、内网小工具 |
| 手写 Socket 解析 HTTP 报文 | 无 | 多且容易出错 | 理解协议原理，不推荐生产 |
| Spring Boot / Jetty / Netty | 要 Maven 依赖 | 最少 | 正式项目 |

本项目的现状是**纯 javac + jar**、没有 Maven，所以这一章以 JDK 自带的 `HttpServer` 为主，最后再给出 Spring Boot 的对照写法。

### 15.2 最小可运行服务端

```java
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class MinimalServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/ping", exchange -> {
            byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);   // 状态码 + 响应体长度
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
            exchange.close();                                 // 一定要关
        });

        // 默认是单线程串行处理，必须换成线程池
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("已启动：http://127.0.0.1:8080/api/ping");
    }
}
```

测试：

```bash
curl.exe "http://127.0.0.1:8080/api/ping"
# {"status":"ok"}
```

> 编译时如果报 `找不到 com.sun.net.httpserver`，加参数 `--add-modules jdk.httpserver`。

### 15.3 处理一个请求的四步

每个 handler 拿到的是 `HttpExchange`，它同时代表「请求」和「响应」，要按固定顺序操作：

```java
private static void handle(HttpExchange exchange) throws IOException {
    try {
        String method = exchange.getRequestMethod();          // ① 方法：GET / POST
        String path = exchange.getRequestURI().getPath();     // ② 路径：/api/ems
        String query = exchange.getRequestURI().getRawQuery();//    查询串：limit=10

        String body = new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);                      // ③ 读请求体（可能为空）

        byte[] out = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, out.length);        // ④ 先发状态码和长度
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);                                    //    再写响应体
        }
    } finally {
        exchange.close();                                     // 无论成功失败都要关闭
    }
}
```

四步的顺序不能乱：

| 步骤 | 方法 | 注意 |
| --- | --- | --- |
| ① 看方法 | `getRequestMethod()` | 不支持的用 405 |
| ② 看路径 | `getRequestURI().getPath()` | 路径区分大小写 |
| ③ 读请求体 | `getRequestBody()` | 只读一次，读完就没了 |
| ④ 写响应 | `sendResponseHeaders` + `getResponseBody()` | **必须先发头再写体** |

### 15.4 读查询参数与请求体

查询参数（`?limit=10&device=ems-01`）需要自己解析：

```java
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

private static Map<String, String> parseQuery(String rawQuery) {
    Map<String, String> params = new HashMap<>();
    if (rawQuery == null || rawQuery.isEmpty()) {
        return params;
    }
    for (String pair : rawQuery.split("&")) {
        int eq = pair.indexOf('=');
        if (eq < 0) {
            params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
        } else {
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
    }
    return params;
}
```

读 JSON 请求体：要么引 Gson，要么按第 10 章（网络通信手册）里的 `jsonValue` 简单取值。请求体只能读一次，读完要自己保存。

### 15.5 写响应的统一格式

建议所有接口都返回同一种结构，客户端好处理：

```json
{"code":0,"msg":"ok","data":{...}}
```

两个工具方法：

```java
/** 成功响应：code=0 */
private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream os = exchange.getResponseBody()) {
        os.write(body);
    }
}

/** 失败响应：统一 {"code":非0,"msg":"..."} */
private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
    String safe = message == null ? "" : message.replace("\"", "'");
    sendJson(exchange, status, "{\"code\":" + status + ",\"msg\":\"" + safe + "\"}");
}
```

状态码用法要一致：

| 情况 | 状态码 |
| --- | --- |
| 查询成功 | `200` |
| 新增成功 | `201` |
| 删除成功、无返回内容 | `204`（或 200 + 空 JSON） |
| 参数缺失、格式错 | `400` |
| 没带 Token / Token 错 | `401` |
| 方法不对（GET 调 POST 接口） | `405` |
| 路径不存在 | `404` |
| 服务端异常 | `500` |

### 15.6 完整示例：EmsHttpServer

需求：

- `POST /api/ems`：接收 `{"ali":300.5,"temp":26.4,"humidity":55.0}`，写入 `ems` 表，返回新记录的 id；
- `GET /api/ems/latest?limit=10`：返回最近 N 条数据（JSON 数组）；
- `GET /api/ping`：健康检查；
- 简单 Token 校验：请求头 `Authorization: Bearer ems-token` 不对就返回 401。

```java
package com.github.rayman717.server;

import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.ConfigManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.Executors;

/**
 * EMS 的 HTTP 服务端。
 * <p>
 * 设备用 POST 上报数据，界面用 GET 拉数据，统一走 /api 下的接口。
 */
public class EmsHttpServer {

    private static final int PORT = 8080;
    private static final String TOKEN = "ems-token";     // 生产环境应从配置读
    private static final int MAX_LIMIT = 100;

    public static void main(String[] args) throws IOException {
        ConfigManager.initConfig();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", EmsHttpServer::handleApi);   // 统一入口，内部路由
        server.setExecutor(Executors.newFixedThreadPool(8));       // 不设就是单线程串行
        server.start();
        System.out.println("EMS HTTP 服务已启动：http://127.0.0.1:" + PORT + "/api/ping");
    }

    /** 路由分发 */
    private static void handleApi(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/api/ping".equals(path) && "GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, "{\"status\":\"ok\"}");
                return;
            }
            if ("/api/ems".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(exchange);
                return;
            }
            if ("/api/ems/latest".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleLatest(exchange);
                return;
            }
            sendError(exchange, 404, "接口不存在");
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "数字格式错误");
        } catch (Exception e) {
            sendError(exchange, 500, "服务端错误：" + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    /** POST /api/ems */
    private static void handleCreate(HttpExchange exchange) throws Exception {
        if (!hasValidToken(exchange)) {
            sendError(exchange, 401, "Token 无效");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        String aliText = jsonValue(body, "ali");
        String tempText = jsonValue(body, "temp");
        String humText = jsonValue(body, "humidity");
        if (aliText == null || tempText == null || humText == null) {
            sendError(exchange, 400, "缺少 ali / temp / humidity 字段");
            return;
        }

        double ali = Double.parseDouble(aliText);
        double temp = Double.parseDouble(tempText);
        double humidity = Double.parseDouble(humText);

        int id = insertRecord(ali, temp, humidity);
        sendJson(exchange, 201, "{\"code\":0,\"id\":" + id + ",\"msg\":\"ok\"}");
    }

    /** GET /api/ems/latest?limit=10 */
    private static void handleLatest(HttpExchange exchange) throws Exception {
        if (!hasValidToken(exchange)) {
            sendError(exchange, 401, "Token 无效");
            return;
        }

        int limit = 10;
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery != null) {
            for (String pair : rawQuery.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "limit".equals(kv[0])) {
                    limit = Math.max(1, Math.min(MAX_LIMIT, Integer.parseInt(kv[1])));
                }
            }
        }

        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, ALI, Temp, Humidity, `Time` FROM ems ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    sb.append("{\"id\":").append(rs.getInt("id"))
                      .append(",\"ali\":").append(rs.getDouble("ALI"))
                      .append(",\"temp\":").append(rs.getDouble("Temp"))
                      .append(",\"humidity\":").append(rs.getDouble("Humidity"))
                      .append(",\"time\":\"").append(rs.getTimestamp("Time")).append("\"}");
                }
            }
        }
        sb.append(']');
        sendJson(exchange, 200, sb.toString());
    }

    /** 写库，返回自增主键 */
    private static int insertRecord(double ali, double temp, double humidity) throws Exception {
        String sql = "INSERT INTO ems (ALI, Temp, Humidity) VALUES (?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, ali);
            ps.setDouble(2, temp);
            ps.setDouble(3, humidity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private static Connection openConnection() throws Exception {
        DbConfig config = ConfigManager.getDbConfig();
        String url = "jdbc:mysql://" + config.ip + ":3306/" + config.dbName
                + "?useSSL=false&characterEncoding=utf8";
        return DriverManager.getConnection(url, config.user, config.password);
    }

    /** 简单 Token 校验：Authorization: Bearer ems-token */
    private static boolean hasValidToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        return auth != null && auth.equals("Bearer " + TOKEN);
    }

    /** 从平铺 JSON 里取一个 key 的值 */
    private static String jsonValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) {
            return null;
        }
        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) {
            return null;
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String safe = message == null ? "" : message.replace("\"", "'");
        sendJson(exchange, status, "{\"code\":" + status + ",\"msg\":\"" + safe + "\"}");
    }
}
```

测试：

```bash
# 健康检查
curl.exe "http://127.0.0.1:8080/api/ping"

# 上报一条
curl.exe -X POST "http://127.0.0.1:8080/api/ems" ^
  -H "Authorization: Bearer ems-token" ^
  -H "Content-Type: application/json" ^
  -d "{\"ali\":300.5,\"temp\":26.4,\"humidity\":55.0}"

# 查最新 5 条
curl.exe "http://127.0.0.1:8080/api/ems/latest?limit=5" -H "Authorization: Bearer ems-token"
```

### 15.7 线程、超时与连接

| 事项 | 说明 |
| --- | --- |
| 默认单线程 | `HttpServer` 不设 executor 时是串行处理，一个慢请求会堵住所有请求 |
| 线程池大小 | 8–16 个足够小项目，取决于数据库能承受多少并发 |
| 关闭 | `server.stop(0)`；线程池要自己 `shutdown()` |
| 数据库连接 | 每个请求开一个连接，教学够用；并发高了要换连接池 |
| 请求大小 | 默认没有严格限制，但别让客户端一次 POST 几百 MB |
| 慢请求 | 自己控制业务超时（比如 JDBC 查询设 `setQueryTimeout`） |

优雅关闭的骨架：

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    server.stop(0);
    executor.shutdown();
    System.out.println("服务已停止");
}));
```

### 15.8 HTTPS 服务端

需要 `HttpsServer` + 一个 keystore（证书 + 私钥）：

```bash
keytool -genkeypair -alias ems -keyalg RSA -keysize 2048 -validity 3650 ^
        -keystore ems.p12 -storetype PKCS12 -storepass changeit ^
        -dname "CN=127.0.0.1"
```

```java
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

KeyStore ks = KeyStore.getInstance("PKCS12");
try (FileInputStream in = new FileInputStream("ems.p12")) {
    ks.load(in, "changeit".toCharArray());
}
KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
kmf.init(ks, "changeit".toCharArray());

SSLContext ctx = SSLContext.getInstance("TLS");
ctx.init(kmf.getKeyManagers(), null, null);

HttpsServer https = HttpsServer.create(new InetSocketAddress(8443), 0);
https.setHttpsConfigurator(new HttpsConfigurator(ctx));
https.createContext("/api/", EmsHttpServer::handleApi);   // 复用同一个 handler
https.start();
```

内网自签证书时，客户端会报 `PKIX path building failed`，处理方式见第 10 章。

### 15.9 生产环境：Spring Boot

JDK 自带的 `HttpServer` 适合理解和跑通流程，正式项目一般用框架，因为它自带路由、参数绑定、校验、日志、监控、连接池。

`pom.xml` 加依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

一个接口只要几行：

```java
@RestController
@RequestMapping("/api/ems")
public class EmsController {

    @PostMapping
    public Map<String, Object> create(@RequestBody EmsRecord record) {
        int id = emsService.save(record);          // 业务逻辑
        return Map.of("code", 0, "id", id);
    }

    @GetMapping("/latest")
    public List<EmsRecord> latest(@RequestParam(defaultValue = "10") int limit) {
        return emsService.latest(limit);
    }
}
```

对照：

| 能力 | JDK `HttpServer` | Spring Boot |
| --- | --- | --- |
| 依赖 | 无 | Maven/Gradle |
| 路由/参数绑定 | 手写 | 注解 |
| JSON 序列化 | 手写 | 自动 |
| 校验 | 手写 | `@Valid` |
| 日志/监控 | 手写 | 自带 |
| 学习成本 | 低 | 中 |

本项目目前没有 Maven，所以先用第 15.6 节的 `EmsHttpServer` 跑通「设备上报 → 入库 → 界面拉取」的闭环；等以后引入 Spring Boot 再迁移。

### 15.10 常见坑

| 坑 | 现象 | 解决 |
| --- | --- | --- |
| 没调 `sendResponseHeaders` | 客户端一直等，超时 | 先发状态码和长度，再写体 |
| 长度写错 | 响应被截断或一直挂起 | 用 `body.length`，或传 `-1` 走 chunked |
| 忘了 `exchange.close()` | 连接泄漏、端口耗尽 | `finally` 里关闭 |
| 没设 executor | 并发请求排队，一个慢请求堵全站 | `setExecutor(线程池)` |
| 读请求体两次 | 第二次读到空 | 只读一次，先存 String |
| 405 处理缺失 | 用错方法也返回 200 | 判断方法并返回 405 |
| 路径大小写 | `/API/ems` 匹配不到 `/api/ems` | 统一小写，或自己忽略大小写 |
| 中文乱码 | 响应中文变问号 | `Content-Type: application/json; charset=UTF-8` |
| 端口占用 | `BindException: Address already in use` | 换端口或关掉占用的进程 |
| 跨域请求失败 | 浏览器报 CORS 错误 | 加 `Access-Control-Allow-Origin` 头 |
| 异常没捕获 | 客户端收到空响应 | handler 里 try/catch，统一返回 500 |
| 返回 `null` 消息体 | 客户端解析失败 | 至少返回 `{}` |
| 用 `com.sun.net.httpserver` 编译报错 | 找不到类 | 加 `--add-modules jdk.httpserver` |
| Token 写死在代码里 | 换 token 要重新编译 | 从 `config.properties` 读 |

---


## 16. 附：和网络通信手册的对应关系

| 主题 | 看哪本 |
| --- | --- |
| TCP 连接、`Socket` 收发 | 网络通信手册 第 3–5 章 |
| 粘包拆包、长度前缀 | 网络通信手册 第 6 章 |
| 多客户端并发（线程池） | 网络通信手册 第 7 章 |
| UDP | 网络通信手册 第 8 章 |
| **HTTP 报文、方法、状态码、请求头** | **本手册 第 1–6 章** |
| **HttpURLConnection / HttpClient 写法** | **本手册 第 7–8 章** |
| 下载、上传、HTTPS、调试 | 本手册 第 9–11 章 |
| 可直接复用的 HTTP 客户端 | 本手册 第 12 章 |
| **HTTP 服务端（接收上报、返回数据）** | **本手册 第 15 章** |
| 设备上报服务端（TCP 收数据入库） | 网络通信手册 第 12 章 |

一句话：**设备之间用 TCP/UDP，对平台/云服务用 HTTP；自己对外提供接口，用第 15 章的 HTTP 服务端。**
