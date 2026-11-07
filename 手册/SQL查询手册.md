# SQL 查询手册（MySQL · EMS 项目版）

> 配套本项目的 `EMS` 数据库（环境监测：光照 `ALI`、温度 `Temp`、湿度 `Humidity`、时间 `Time`），
> 语句以 MySQL 5.7 为基准（驱动 `mysql-connector-java-5.1.49`），标注 `8.0+` 的写法在低版本不可用。

---

## 目录

1. [连接与基础命令](#1-连接与基础命令)
2. [SELECT 的执行顺序](#2-select-的执行顺序)
3. [查询字段与计算列](#3-查询字段与计算列)
4. [WHERE 条件过滤](#4-where-条件过滤)
5. [排序与分页](#5-排序与分页)
6. [聚合函数与分组](#6-聚合函数与分组)
7. [时间与日期查询](#7-时间与日期查询)
8. [多表连接 JOIN](#8-多表连接-join)
9. [子查询与 UNION](#9-子查询与-union)
10. [CASE WHEN 与空值处理](#10-case-when-与空值处理)
11. [本项目实战查询](#11-本项目实战查询)
12. [修改数据（INSERT / UPDATE / DELETE）](#12-修改数据insert--update--delete)
13. [索引与执行计划](#13-索引与执行计划)
14. [常见报错速查](#14-常见报错速查)

---

## 1. 连接与基础命令

### 1.1 用命令行连接

```bash
# 本机
mysql -u root -p

# 远程（对应 config.properties 里的 db.ip / db.port）
mysql -h 192.168.2.51 -P 3306 -u root -p

# 直接进入指定库
mysql -h 192.168.2.51 -P 3306 -u root -p EMS
```

### 1.2 库、表的基本查看

```sql
SHOW DATABASES;                 -- 列出所有数据库
USE EMS;                        -- 切换到 EMS 库
SELECT DATABASE();              -- 查看当前所在库

SHOW TABLES;                    -- 列出当前库所有表
DESC ems;                       -- 查看表结构（列名、类型、主键、默认值）
SHOW CREATE TABLE ems;          -- 查看建表语句（含字符集、引擎）
SHOW INDEX FROM ems;            -- 查看索引

SELECT VERSION();               -- 服务器版本，决定能不能用 8.0 语法
SELECT NOW();                   -- 当前时间
```

> `DESC ems;` 是最常用的一条命令：写查询前先确认列名大小写和类型。

---

## 2. SELECT 的执行顺序

书写顺序和执行顺序**不一样**，这决定了别名能不能在 `WHERE` 里用：

```sql
SELECT   ALI, AVG(Temp) AS avg_temp       -- 5. 查出结果并计算别名
FROM     ems                              -- 1. 先定位表
WHERE    Temp > 0                         -- 2. 过滤行
GROUP BY ALI                              -- 3. 分组
HAVING   AVG(Temp) > 20                   -- 4. 过滤分组
ORDER BY avg_temp DESC                    -- 6. 排序（可以用别名）
LIMIT    10;                              -- 7. 截取前 N 行
```

实际执行顺序：`FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT`

由此得到两条硬规则：

- `WHERE` 中**不能**使用 `SELECT` 里定义的别名（那时还没计算）；
- `ORDER BY` 中**可以**使用别名。

---

## 3. 查询字段与计算列

```sql
SELECT * FROM ems;                       -- 查全部列（调试用，正式代码不要用 *）
SELECT id, Temp, Humidity FROM ems;      -- 只查需要的列（更快）

-- 别名：AS 可省略，中文别名要用反引号
SELECT id, Temp AS 温度, Humidity AS 湿度 FROM ems;

-- 计算列
SELECT id, Temp * 9 / 5 + 32 AS temp_f FROM ems;

-- 去重
SELECT DISTINCT ALI FROM ems;                 -- 光照出现过哪些值
SELECT COUNT(DISTINCT ALI) FROM ems;          -- 有多少种不同取值

-- 字符串拼接
SELECT CONCAT('记录', id, '：', Temp, '℃') AS info FROM ems;
SELECT CONCAT_WS(' | ', id, Temp, Humidity) FROM ems;

-- 常量列、限定列名（多表时建议一律写 表名.列名）
SELECT '当前库数据' AS 来源, ems.id, ems.Temp FROM ems;
```

> 列名与关键字冲突（如 `Time`、`user`、`order`）时用反引号包裹：`` `Time` ``、`` `user` ``。

---

## 4. WHERE 条件过滤

### 4.1 比较与逻辑运算

```sql
SELECT * FROM ems WHERE Temp > 30;                    -- 温度高于 30℃
SELECT * FROM ems WHERE Temp >= 20 AND Temp <= 30;    -- 区间
SELECT * FROM ems WHERE Temp > 30 OR Humidity < 40;   -- 满足任一
SELECT * FROM ems WHERE NOT Temp = 0;                 -- 取反
SELECT * FROM ems WHERE id <> 5;                      -- 不等于（也可写 !=）
```

`AND` 优先级高于 `OR`，混用时要加括号：

```sql
-- 错误意图：想查 (温度高 且 湿度低) 或 温度为 0
SELECT * FROM ems WHERE Temp > 30 AND Humidity < 40 OR Temp = 0;

-- 正确写法
SELECT * FROM ems WHERE (Temp > 30 AND Humidity < 40) OR Temp = 0;
```

### 4.2 IN、BETWEEN、LIKE、IS NULL

```sql
-- IN：多个等值之一
SELECT * FROM ems WHERE id IN (1, 2, 3, 5);
SELECT * FROM ems WHERE id NOT IN (1, 2);

-- BETWEEN：闭区间，含两端
SELECT * FROM ems WHERE Temp BETWEEN 20 AND 30;

-- LIKE：模糊匹配（% 任意多字符，_ 单个字符），示例用 user 表的账号名
SELECT * FROM `user` WHERE `User` LIKE '张%';      -- 以「张」开头
SELECT * FROM `user` WHERE `User` LIKE '%张%';     -- 包含「张」
SELECT * FROM `user` WHERE `User` LIKE '张_';      -- 张 + 恰好 1 个字符

-- 匹配字面量 % 或 _，需要转义
SELECT * FROM `user` WHERE `User` LIKE '%\%%' ESCAPE '\\';

-- NULL 只能用 IS NULL / IS NOT NULL 判断，不能用 = NULL
-- （ems 的 ALI 是 NOT NULL DEFAULT 0，这里只演示语法，可换成允许 NULL 的列）
SELECT * FROM ems WHERE ALI IS NULL;
SELECT * FROM ems WHERE ALI IS NOT NULL;
```

> 注意：`LIKE '%张%'` 左侧带 `%` 时索引失效，数据量大要换成全文检索或冗余字段。

### 4.3 优先级速记

| 符号 | 说明 | 示例 |
| --- | --- | --- |
| `()` | 最高，永远用它消歧义 | `WHERE (a OR b) AND c` |
| `NOT` | 一元取反 | `NOT a = 1` |
| `AND` | 逻辑与 | `a = 1 AND b = 2` |
| `OR` | 逻辑或 | `a = 1 OR b = 2` |

---

## 5. 排序与分页

```sql
SELECT * FROM ems ORDER BY Temp DESC;              -- 温度从高到低
SELECT * FROM ems ORDER BY Temp ASC;               -- 从低到高（ASC 可省略）

-- 多列排序：先按湿度降序，湿度相同再按时间降序
SELECT * FROM ems ORDER BY Humidity DESC, `Time` DESC;

-- 排序中手写表达式
SELECT * FROM ems ORDER BY ABS(Temp - 25);

-- 分页：LIMIT 条数 OFFSET 跳过条数（推荐写法）
SELECT * FROM ems ORDER BY id LIMIT 10 OFFSET 0;   -- 第 1 页，每页 10 条
SELECT * FROM ems ORDER BY id LIMIT 10 OFFSET 10;  -- 第 2 页

-- 等价简写：LIMIT 跳过条数, 条数（易读性差，不建议）
SELECT * FROM ems ORDER BY id LIMIT 10, 10;

-- 最新一条记录
SELECT * FROM ems ORDER BY `Time` DESC LIMIT 1;
```

> 分页依赖**稳定排序**：`ORDER BY` 最好带上唯一列（如 `id`），否则翻页可能重复或漏数据。
> 注意：`ORDER BY` 中的 NULL 在 MySQL 里被视为最小值，`ASC` 时排最前。

---

## 6. 聚合函数与分组

### 6.1 五个常用聚合

```sql
SELECT COUNT(*)     AS 记录数 FROM ems;      -- 统计所有行
SELECT COUNT(ALI)   AS 非空光照数 FROM ems;  -- 该列非 NULL 的行数
SELECT SUM(ALI)     FROM ems;                 -- 求和
SELECT AVG(Temp)    FROM ems;                 -- 平均值（忽略 NULL）
SELECT MAX(Temp), MIN(Temp) FROM ems;         -- 最大 / 最小
SELECT ROUND(AVG(Temp), 2) FROM ems;          -- 保留 2 位小数
SELECT ROUND(AVG(Temp), 2) AS avg_temp FROM ems WHERE `Time` >= CURDATE();
```

聚合查询只返回一行，`SELECT` 里除了聚合函数不应再放普通列（见 6.4）。

### 6.2 GROUP BY 分组

```sql
-- 按光照值分组，统计每组的记录数和平均温度
SELECT ALI, COUNT(*) AS cnt, ROUND(AVG(Temp), 2) AS avg_temp
FROM ems
GROUP BY ALI
ORDER BY cnt DESC;

-- 按天分组
SELECT DATE(`Time`) AS day, COUNT(*) AS cnt, ROUND(AVG(Temp), 2) AS avg_temp
FROM ems
GROUP BY DATE(`Time`)
ORDER BY day DESC;
```

### 6.3 HAVING：过滤分组结果

`WHERE` 过滤**行**，`HAVING` 过滤**分组**，且 `HAVING` 可以用聚合函数和别名：

```sql
SELECT ALI, ROUND(AVG(Temp), 2) AS avg_temp
FROM ems
WHERE Temp > 0              -- 先过滤行
GROUP BY ALI
HAVING avg_temp > 25        -- 再过滤分组（可写 AVG(Temp) > 25）
ORDER BY avg_temp DESC;
```

### 6.4 报错 1055：ONLY_FULL_GROUP_BY

MySQL 5.7 默认开启该模式，`SELECT` 中出现的非聚合列必须出现在 `GROUP BY` 中：

```sql
-- 报错 1055：id 没有出现在 GROUP BY 里
SELECT id, ALI, MAX(Temp) FROM ems GROUP BY ALI;

-- 正确思路 1：id 也放进 GROUP BY
SELECT id, ALI, MAX(Temp) FROM ems GROUP BY ALI, id;

-- 正确思路 2：用子查询取每组最大值那条记录（推荐）
SELECT e.*
FROM ems e
JOIN (
    SELECT ALI, MAX(Temp) AS max_temp FROM ems GROUP BY ALI
) m ON e.ALI = m.ALI AND e.Temp = m.max_temp;
```

> 不要为了省事关闭 `sql_mode` 里的 `ONLY_FULL_GROUP_BY`，它拦住的基本都是逻辑错误。

---

## 7. 时间与日期查询

`Time` 列是 `DATETIME`，下面是排查数据、做报表最常用的一组写法。

```sql
-- 当前时间相关
SELECT NOW();                    -- 2026-01-01 12:30:45
SELECT CURDATE();                -- 2026-01-01
SELECT CURTIME();                -- 12:30:45

-- 单个时间字段的拆解
SELECT YEAR(`Time`), MONTH(`Time`), DAY(`Time`), HOUR(`Time`) FROM ems;
SELECT DATE(`Time`) AS d, TIME(`Time`) AS t FROM ems;

-- 格式化输出（做报表习惯用这个）
SELECT DATE_FORMAT(`Time`, '%Y-%m-%d %H:%i:%s') AS 时间文字 FROM ems;
SELECT DATE_FORMAT(`Time`, '%Y-%m-%d %H:00')     AS 小时段 FROM ems;
SELECT DATE_FORMAT(`Time`, '%Y年%m月%d日')        AS 日期中文 FROM ems;

-- 字符串转时间
SELECT STR_TO_DATE('2026-01-01 08:00:00', '%Y-%m-%d %H:%i:%s');
```

### 7.1 常用时间范围条件

```sql
-- 今天（从 0 点开始）
SELECT * FROM ems WHERE `Time` >= CURDATE();

-- 只查某一天
SELECT * FROM ems WHERE DATE(`Time`) = '2026-01-01';

-- 最近 7 天 / 最近 1 小时
SELECT * FROM ems WHERE `Time` >= DATE_SUB(NOW(), INTERVAL 7 DAY);
SELECT * FROM ems WHERE `Time` >= DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- 本周、本月
SELECT * FROM ems WHERE YEARWEEK(`Time`, 1) = YEARWEEK(CURDATE(), 1);
SELECT * FROM ems WHERE DATE_FORMAT(`Time`, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m');

-- 两个时间相差多少天 / 多少小时 / 多少分钟
SELECT DATEDIFF(NOW(), `Time`) AS 天数;
SELECT TIMESTAMPDIFF(HOUR,   `Time`, NOW()) AS 小时数;
SELECT TIMESTAMPDIFF(MINUTE, `Time`, NOW()) AS 分钟数;
```

### 7.2 按时间维度做统计

```sql
-- 每天的最高、最低、平均温度
SELECT
    DATE(`Time`)          AS 日期,
    COUNT(*)              AS 记录数,
    ROUND(MAX(Temp), 1)   AS 最高温,
    ROUND(MIN(Temp), 1)   AS 最低温,
    ROUND(AVG(Temp), 1)   AS 平均温
FROM ems
WHERE `Time` >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(`Time`)
ORDER BY 日期 DESC;

-- 每小时平均湿度（按小时段聚合）
SELECT
    DATE_FORMAT(`Time`, '%Y-%m-%d %H:00') AS 小时段,
    ROUND(AVG(Humidity), 2)               AS 平均湿度
FROM ems
GROUP BY 小时段
ORDER BY 小时段 DESC;

-- 查询某天的第 3 条到第 6 条记录
SELECT * FROM ems
WHERE DATE(`Time`) = '2026-01-01'
ORDER BY `Time`
LIMIT 4 OFFSET 2;   -- 跳过前 2 条，取 4 条，即第 3~6 条
```

> 时间范围条件尽量写成 `Time >= '...' AND Time < '...'` 的形式，
> 对列使用函数（如 `DATE(Time) = ...`）会让该列索引失效。

---

## 8. 多表连接 JOIN

本项目两张表 `ems`（监测数据）和 `user`（账号）目前没有外键关联，下面用它们演示 JOIN 语法
（`u.id = e.id` 仅为示例关联条件）；以后加设备表、加「录入人」字段时，替换表名与条件即可。

### 8.1 四种连接的区别

| 类型 | 结果 |
| --- | --- |
| `INNER JOIN` | 两表都能匹配上的行 |
| `LEFT JOIN` | 左表全部 + 右表匹配行（右表没有则补 NULL） |
| `RIGHT JOIN` | 右表全部 + 左表匹配行 |
| `CROSS JOIN` | 笛卡尔积（少用） |

```sql
-- INNER JOIN：只保留能关联上的
SELECT u.id, u.`User`, e.Temp
FROM `user` u
JOIN ems e ON u.id = e.id;          -- JOIN 默认为 INNER JOIN

-- LEFT JOIN：以左表为主，右表缺失填 NULL
SELECT u.id, u.`User`, e.Temp
FROM `user` u
LEFT JOIN ems e ON u.id = e.id;

-- 过滤条件写在 ON 里 ≠ 写在 WHERE 里
-- LEFT JOIN 时，写在 WHERE 中的右表条件会把「补 NULL」的行过滤掉，等价于 INNER JOIN
SELECT u.id, e.Temp
FROM `user` u
LEFT JOIN ems e ON u.id = e.id AND e.Temp > 30;   -- 保留左表全部
```

### 8.2 写法要点

- 起别名后，字段一律写「别名.列名」，避免 `id` 这种同名列报 `1052 Column 'id' in field list is ambiguous`；
- `ON` 写关联条件，`WHERE` 写过滤条件；
- 三表以上依次 `JOIN`，从左到右理解。

```sql
-- 相关子查询：假设 ems 增加 user_id 列记录「录入人」，查每个用户最近一条记录
SELECT u.id, u.`User`, e.Temp, e.`Time`
FROM `user` u
LEFT JOIN ems e ON e.id = (
    SELECT e2.id
    FROM ems e2
    WHERE e2.user_id = u.id
    ORDER BY e2.`Time` DESC
    LIMIT 1
);
```

---

## 9. 子查询与 UNION

### 9.1 标量子查询（返回一个值）

```sql
-- 查温度等于最高温度的那条记录
SELECT * FROM ems WHERE Temp = (SELECT MAX(Temp) FROM ems);

-- 查高于平均温度的所有记录
SELECT * FROM ems WHERE Temp > (SELECT AVG(Temp) FROM ems);

-- 派生表：把子查询结果当成一张临时表交给外层继续查
SELECT * FROM (
    SELECT id, Temp FROM ems ORDER BY Temp DESC LIMIT 5
) t ORDER BY t.Temp ASC;      -- 取温度前 5 条，再按升序显示
```

### 9.2 IN / EXISTS

```sql
-- IN：子查询返回一列
SELECT * FROM ems WHERE id IN (SELECT id FROM `user`);

-- EXISTS：只判断存在性，通常比 IN 快（大数据量）
SELECT * FROM ems e
WHERE EXISTS (SELECT 1 FROM `user` u WHERE u.id = e.id);

-- NOT EXISTS：找出没有账号对应的记录
SELECT * FROM ems e
WHERE NOT EXISTS (SELECT 1 FROM `user` u WHERE u.id = e.id);
```

### 9.3 UNION 合并结果

```sql
-- 上下拼接：列数和类型要一致；UNION 会去重，UNION ALL 不去重（更快）
SELECT id, Temp FROM ems WHERE Temp > 30
UNION
SELECT id, Temp FROM ems WHERE Humidity > 80;

-- 合并时只保留第一个 SELECT 的列名
SELECT id AS 编号, Temp AS 数值 FROM ems WHERE Temp > 30
UNION ALL
SELECT id, Humidity FROM ems WHERE Humidity > 80;
```

---

## 10. CASE WHEN 与空值处理

```sql
-- 条件分支：给温度打等级
SELECT id, Temp,
    CASE
        WHEN Temp >= 30 THEN '高温'
        WHEN Temp >= 20 THEN '舒适'
        WHEN Temp >= 10 THEN '偏冷'
        ELSE '寒冷'
    END AS 等级
FROM ems;

-- 配合聚合做「条件计数」：高温记录数、舒适记录数
SELECT
    SUM(CASE WHEN Temp >= 30 THEN 1 ELSE 0 END) AS 高温数,
    SUM(CASE WHEN Temp <  30 THEN 1 ELSE 0 END) AS 其他数
FROM ems;

-- 空值处理
SELECT IFNULL(ALI, 0) FROM ems;              -- 为 NULL 时取 0
SELECT COALESCE(ALI, Temp, 0) FROM ems;      -- 返回第一个非 NULL
SELECT IF(Temp > 30, '热', '正常') FROM ems;  -- 三元表达式
```

> `SUM(... THEN 1 ELSE 0)` 是跨数据库都通用的条件统计写法，比多次 `WHERE` 查询省事。

---

## 11. 本项目实战查询

直接可用的 `EMS` 库查询，表结构来自 `SqlUtil.CreateTable`：

```
ems : id INT 主键自增 | ALI DOUBLE 光照 | Temp DOUBLE 温度
      Humidity DOUBLE 湿度 | Time DATETIME 记录时间
user: id INT 主键自增 | User CHAR(255) | Password CHAR(255)
```

### 11.1 日常查看

```sql
-- 最新 10 条监测数据
SELECT id, ALI, Temp, Humidity, `Time`
FROM ems
ORDER BY `Time` DESC
LIMIT 10;

-- 某段时间的数据
SELECT * FROM ems
WHERE `Time` BETWEEN '2026-01-01 00:00:00' AND '2026-01-01 23:59:59'
ORDER BY `Time`;

-- 记录总数与最早、最晚时间
SELECT COUNT(*) AS 总数, MIN(`Time`) AS 最早, MAX(`Time`) AS 最晚 FROM ems;
```

### 11.2 报警类查询

```sql
-- 温度超过 30℃ 的记录
SELECT id, Temp, `Time` FROM ems WHERE Temp > 30 ORDER BY `Time` DESC;

-- 温度或湿度任一超标
SELECT * FROM ems
WHERE Temp > 30 OR Humidity > 80
ORDER BY `Time` DESC;

-- 湿度低于 40 且温度高于 25
SELECT * FROM ems
WHERE Humidity < 40 AND Temp > 25
ORDER BY Temp DESC, Humidity ASC;
```

### 11.3 统计类查询

```sql
-- 全部数据的平均值（保留 2 位）
SELECT
    ROUND(AVG(ALI), 2)       AS 平均光照,
    ROUND(AVG(Temp), 2)      AS 平均温度,
    ROUND(AVG(Humidity), 2)  AS 平均湿度
FROM ems;

-- 今天的最高温度、最低温度
SELECT ROUND(MAX(Temp), 1) AS 最高, ROUND(MIN(Temp), 1) AS 最低
FROM ems WHERE `Time` >= CURDATE();

-- 每天统计一次（最近 7 天）
SELECT DATE(`Time`) AS 日期, COUNT(*) AS 条数, ROUND(AVG(Temp), 1) AS 平均温度
FROM ems
WHERE `Time` >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE(`Time`)
ORDER BY 日期;

-- 温度最高的 5 条
SELECT id, Temp, `Time` FROM ems ORDER BY Temp DESC LIMIT 5;
```

### 11.4 账号表查询

```sql
-- 登录校验：返回 1 条即表示账号密码正确
SELECT id, `User` FROM `user` WHERE `User` = 'admin' AND `Password` = '123456';

-- 用户总数
SELECT COUNT(*) FROM `user`;

-- 最近注册的 5 个账号
SELECT id, `User` FROM `user` ORDER BY id DESC LIMIT 5;
```

> 实际项目里密码应存哈希（如 MD5/BCrypt），不要在 SQL 里明文比对。
> 另外注意 `user` 表名建议始终加反引号，避免与系统表 / 函数名混淆。

---

## 12. 修改数据（INSERT / UPDATE / DELETE）

查询手册也常要顺手改数据，附上最常用的写法，**执行前先 `SELECT` 确认条件**。

```sql
-- 插入：指定列，自增列和带默认值的列可省略
INSERT INTO ems (ALI, Temp, Humidity) VALUES (320.5, 26.4, 55.0);

-- 批量插入
INSERT INTO ems (ALI, Temp, Humidity, `Time`) VALUES
(300, 25.1, 60, '2026-01-01 08:00:00'),
(310, 26.2, 58, '2026-01-01 09:00:00');

-- 显式写时间（默认值自动取当前时间，可不写）
INSERT INTO ems (Temp, `Time`) VALUES (27.0, NOW());

-- 更新：务必带 WHERE，否则全表被改
UPDATE ems SET Temp = 25.0 WHERE id = 10;
UPDATE ems SET Humidity = Humidity + 1 WHERE `Time` >= CURDATE();

-- 删除：务必带 WHERE
DELETE FROM ems WHERE id = 10;

-- 清理 30 天前的历史数据（先 SELECT COUNT(*) 确认条数）
SELECT COUNT(*) FROM ems WHERE `Time` < DATE_SUB(NOW(), INTERVAL 30 DAY);
DELETE FROM ems WHERE `Time` < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 清空表：TRUNCATE 不可回滚、重置自增，比 DELETE 快
TRUNCATE TABLE ems;
```

> 安全习惯：改数据前先 `START TRANSACTION;`，确认无误 `COMMIT;`，出问题 `ROLLBACK;`。

---

## 13. 索引与执行计划

### 13.1 看执行计划

```sql
EXPLAIN SELECT * FROM ems WHERE `Time` >= CURDATE();

EXPLAIN SELECT * FROM ems WHERE Temp > 30 ORDER BY Temp DESC;
```

重点看四列：

| 列 | 关注点 |
| --- | --- |
| `type` | 从好到坏：`const` > `eq_ref` > `ref` > `range` > `index` > `ALL`，出现 `ALL` 即全表扫描 |
| `key` | 实际使用了哪个索引，`NULL` 表示没走索引 |
| `rows` | 预估扫描行数，越小越好 |
| `Extra` | `Using index`（覆盖索引，好）、`Using filesort`（额外排序，慢）、`Using temporary`（用临时表，慢） |

### 13.2 建索引

```sql
CREATE INDEX idx_ems_time ON ems (`Time`);            -- 单列索引
CREATE INDEX idx_ems_temp_hum ON ems (Temp, Humidity); -- 联合索引
ALTER TABLE ems ADD INDEX idx_ems_ali (ALI);
DROP INDEX idx_ems_ali ON ems;                        -- 删除索引
```

### 13.3 索引使用规则

- **最左前缀**：联合索引 `(Temp, Humidity)` 能用于 `WHERE Temp = ?`，不能单独用于 `WHERE Humidity = ?`；
- 对列做运算或函数（`DATE(Time) = ?`、`Temp + 1 > 30`）会导致索引失效；
- `LIKE '%xx'` 左侧模糊不命中索引；
- 索引不是越多越好：每个索引都会增加写入开销与磁盘占用；
- 时间列 + 排序字段通常是这类监测表最值得建的索引。

---

## 14. 常见报错速查

| 错误码 | 提示 | 常见原因与处理 |
| --- | --- | --- |
| `1045` | Access denied for user | 用户名/密码错、或该账号没被授权从当前 IP 登录 |
| `1049` | Unknown database | 库名拼错或库不存在，先 `SHOW DATABASES;` |
| `1054` | Unknown column | 列名拼错或大小写不符（Linux 上表名区分大小写） |
| `1064` | You have an error in your SQL syntax | 多/少逗号、括号不配对、关键字当列名没加反引号 |
| `1055` | expression is not in GROUP BY | `ONLY_FULL_GROUP_BY` 限制，见 6.4 |
| `1062` | Duplicate entry | 主键或唯一键重复，插入前先查是否已存在 |
| `1146` | Table doesn't exist | 表不存在，或没 `USE` 到正确的库 |
| `1052` | Column is ambiguous | 多表连接时同名列未加表别名前缀 |
| `1451` / `1452` | Cannot delete/update a parent row / Cannot add foreign key | 外键约束冲突，先处理子表数据 |
| `1366` | Incorrect string value | 库/连接字符集不支持中文（如 latin1），建议统一 utf8mb4 |
| `2003` | Can't connect to MySQL server | 服务未启动、IP/端口错、防火墙拦截 |
| `10061` | Connection refused | MySQL 未启动或端口没监听（Windows 常见） |

### 排查套路

```sql
SHOW WARNINGS;                                   -- 看最近一条语句的详细信息
SHOW VARIABLES LIKE 'sql_mode';                  -- 确认是否 ONLY_FULL_GROUP_BY
SHOW VARIABLES LIKE 'character_set%';            -- 排查中文乱码
SELECT VERSION();                                -- 确认版本是否支持所用语法
SHOW PROCESSLIST;                                -- 看是否有慢查询/锁等待
```

---

## 附：写查询的 10 条习惯

1. 先 `DESC 表名` 确认字段，再动笔；
2. 生产查询不用 `SELECT *`，只取需要的列；
3. `WHERE` 条件写全，改删数据前先 `SELECT` 验证；
4. `AND` / `OR` 混用加括号；
5. 判断 NULL 只用 `IS NULL`；
6. 时间范围用 `>= ... AND < ...`，尽量不对列套函数；
7. 分组统计注意 `ONLY_FULL_GROUP_BY`；
8. 分页一定配 `ORDER BY`，并带上唯一列；
9. 大表查询先 `EXPLAIN`，看有没有 `type: ALL`；
10. 改数据前 `START TRANSACTION;`，确认后 `COMMIT;`。
