# HTTP 请求详解（网络通信附加篇）

> 本手册是 [网络通信使用手册](网络通信使用手册.html) 的**附加篇**，专门把 HTTP 请求讲透。
> 基础的 TCP / UDP 部分（Socket、粘包、多客户端并发）请看网络通信手册的第 3–8 章，这里不再重复。
> 环境：JDK 17，主体使用 `java.net.http.HttpClient`，同时保留 JDK 8 的 `HttpURLConnection` 写法。

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
15. [附：和网络通信手册的对应关系](#15-附和网络通信手册的对应关系)

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

---

## 15. 附：和网络通信手册的对应关系

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
| 设备上报服务端（TCP 收数据入库） | 网络通信手册 第 12 章 |

一句话：**设备之间用 TCP/UDP，对平台/云服务用 HTTP。**
