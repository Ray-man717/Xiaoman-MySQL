# CSS 使用手册（网页的第二层：样式）

> HTML 决定页面**有什么**，CSS 决定**长什么样**：颜色、字体、间距、布局、动画。
> 这一本从选择器和盒模型讲起，把最常用的 Flex 布局、响应式、常见组件样式讲透。

---

## 目录

1. [CSS 是什么](#1-css-是什么)
2. [选择器](#2-选择器)
3. [优先级与层叠](#3-优先级与层叠)
4. [盒模型](#4-盒模型)
5. [颜色与背景](#5-颜色与背景)
6. [文字与字体](#6-文字与字体)
7. [display 与 position](#7-display-与-position)
8. [Flex 布局](#8-flex-布局)
9. [Grid 网格布局](#9-grid-网格布局)
10. [响应式与单位](#10-响应式与单位)
11. [过渡与动画](#11-过渡与动画)
12. [常用组件样式](#12-常用组件样式)
13. [一个完整页面](#13-一个完整页面)
14. [常见问题](#14-常见问题)
15. [速查表](#15-速查表)

---

## 1. CSS 是什么

CSS（Cascading Style Sheets，层叠样式表）描述 HTML 的**呈现样式**。

三种引入方式：

```html
<!-- ① 外部样式表（推荐） -->
<link rel="stylesheet" href="style.css">

<!-- ② 页面内 style 标签 -->
<style>
    h1 { color: red; }
</style>

<!-- ③ 元素内联 style（不推荐大量用） -->
<h1 style="color: red;">标题</h1>
```

基本语法：

```css
选择器 {
    属性: 值;
    属性: 值;
}
```

```css
h1 {
    color: #333;
    font-size: 24px;
    text-align: center;
}
```

---

## 2. 选择器

| 选择器 | 写法 | 选中 |
| --- | --- | --- |
| 标签 | `p` | 所有 `<p>` |
| 类 | `.box` | `class="box"` 的元素 |
| id | `#app` | `id="app"` 的元素 |
| 群组 | `h1, h2` | 多个选择器共用样式 |
| 后代 | `.box p` | `.box` 里的所有 `p` |
| 子元素 | `.box > p` | `.box` 的直接子元素 `p` |
| 相邻兄弟 | `h1 + p` | 紧跟在 `h1` 后的第一个 `p` |
| 属性 | `input[type="text"]` | 指定属性 |
| 伪类 | `a:hover` | 鼠标悬停时的链接 |
| 伪类 | `li:nth-child(2)` | 第 2 个 li |
| 伪元素 | `p::before` | 在 `p` 内容前插入 |
| 通配 | `*` | 所有元素 |

```css
/* 组合使用 */
.card .title {
    font-weight: bold;
}

.card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, .15);
}

input:focus {
    border-color: #2f6fdd;
    outline: none;
}

ul li:first-child {
    color: red;
}

.btn::after {
    content: " →";
}
```

常用伪类：

| 伪类 | 触发时机 |
| --- | --- |
| `:hover` | 鼠标悬停 |
| `:focus` | 获得焦点（输入框） |
| `:active` | 按下时 |
| `:first-child` / `:last-child` | 第一个 / 最后一个 |
| `:nth-child(n)` | 第 n 个（支持 `odd`、`even`） |
| `:not(.x)` | 排除 |

---

## 3. 优先级与层叠

同样能选中一个元素时，谁的样式生效？看**优先级**：

| 写法 | 权重 |
| --- | --- |
| `!important` | 最高（尽量别用） |
| 内联 `style=""` | 1000 |
| `#id` | 100 |
| `.class` / `[attr]` / `:hover` | 10 |
| 标签 / `::before` | 1 |
| `*` | 0 |

规则：

- 权重高的赢；
- 权重相同，**后写的赢**（层叠）；
- `!important` 能压过一切，但会让样式难以维护，只在必要时用。

```css
/* 优先级 10 */
.title { color: blue; }

/* 优先级 100，覆盖上面 */
#main-title { color: red; }
```

继承：文字类属性（`color`、`font-size`）会从父元素继承，`margin`、`padding`、`border` 不会。

调试技巧：浏览器 F12 → Elements，选元素能看到「哪条规则生效、被谁覆盖」。

---

## 4. 盒模型

每个元素都是一个矩形盒子，由内到外四层：

```
┌─────────────────────────────┐
│          margin             │  外边距（盒子之间的空隙）
│  ┌───────────────────────┐  │
│  │        border         │  │  边框
│  │  ┌─────────────────┐  │  │
│  │  │    padding      │  │  │  内边距（内容到边框）
│  │  │  ┌───────────┐  │  │  │
│  │  │  │  content  │  │  │  │  内容区
│  │  │  └───────────┘  │  │  │
│  │  └─────────────────┘  │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

```css
.box {
    width: 200px;           /* 内容宽度 */
    height: 100px;
    padding: 16px;          /* 内边距 */
    border: 1px solid #ccc; /* 边框 */
    margin: 12px;           /* 外边距 */
}
```

**关键**：默认 `box-sizing: content-box`，`width` 只是内容宽度，加上 padding 和 border 实际更宽。加上这一句让宽度包含内边距和边框：

```css
* {
    box-sizing: border-box;
}
```

常用写法：

```css
/* 四边分别设置 */
margin: 10px 20px 10px 20px;   /* 上 右 下 左 */
margin: 10px 20px;              /* 上下 10，左右 20 */
margin: 10px;                   /* 四边都是 10 */
margin: 0 auto;                 /* 块级元素水平居中 */

padding: 8px 16px;
border-radius: 8px;             /* 圆角 */
```

**margin 折叠**：上下相邻的两个块级元素，`margin` 会合并（取较大的那个），不是相加。用 padding 或 flex 的 `gap` 可以避免。

---

## 5. 颜色与背景

### 5.1 颜色写法

```css
color: red;                       /* 颜色名 */
color: #ff0000;                   /* 十六进制 */
color: #f00;                      /* 简写 */
color: rgb(255, 0, 0);            /* RGB */
color: rgba(255, 0, 0, 0.5);      /* 带透明度 */
color: hsl(0, 100%, 50%);         /* 色相/饱和度/亮度 */
```

### 5.2 背景

```css
.box {
    background-color: #f5f7fa;
    background-image: url("bg.png");
    background-repeat: no-repeat;          /* 不重复 */
    background-position: center;           /* 居中 */
    background-size: cover;                /* 铺满，可能裁切 */
    background-size: contain;              /* 完整显示，可能留白 */
}

/* 简写 */
.box {
    background: #f5f7fa url("bg.png") no-repeat center / cover;
}

/* 渐变 */
.gradient {
    background: linear-gradient(135deg, #667eea, #764ba2);
}
```

---

## 6. 文字与字体

```css
body {
    font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif;
    font-size: 15px;
    color: #333;
    line-height: 1.8;                   /* 行高：1.8 倍字号 */
    font-weight: 400;                   /* 400 正常，700 加粗 */
}

h1 {
    font-size: 24px;
    text-align: center;                 /* 居中 */
    letter-spacing: 1px;                /* 字间距 */
}

a {
    color: #2f6fdd;
    text-decoration: none;              /* 去掉下划线 */
}

a:hover {
    text-decoration: underline;
}

p {
    text-indent: 2em;                   /* 首行缩进 2 个字 */
    word-break: break-all;              /* 长英文/网址换行 */
}
```

字体建议：中文优先写「微软雅黑」「PingFang SC」，最后兜底 `sans-serif`，保证没有字体时也不难看。

---

## 7. display 与 position

### 7.1 display

| 值 | 说明 |
| --- | --- |
| `block` | 块级，独占一行，可设宽高 |
| `inline` | 行内，不换行，宽高无效 |
| `inline-block` | 行内但可设宽高 |
| `flex` | 弹性布局容器（第 8 章） |
| `grid` | 网格布局容器（第 9 章） |
| `none` | 隐藏，不占位置 |

`display: none` 和 `visibility: hidden` 的区别：前者不占位置，后者占位置但看不见。

### 7.2 position

| 值 | 说明 |
| --- | --- |
| `static` | 默认，正常排版 |
| `relative` | 相对自身原位置偏移，原位置保留 |
| `absolute` | 相对最近的定位祖先偏移，脱离文档流 |
| `fixed` | 相对浏览器窗口固定（滚动不动） |
| `sticky` | 滚动到阈值时「粘住」 |

```css
/* 绝对定位：父元素必须是 position: relative */
.parent {
    position: relative;
}
.child {
    position: absolute;
    top: 10px;
    right: 10px;
}

/* 固定在右下角的按钮 */
.back-top {
    position: fixed;
    right: 20px;
    bottom: 20px;
}

/* 粘性表头 */
th {
    position: sticky;
    top: 0;
    background: #fff;
}

/* 层叠顺序：数字越大越靠上 */
.modal {
    position: fixed;
    z-index: 1000;
}
```

**最常见的问题**：`absolute` 没有生效（位置不对），是因为它的祖先元素没有 `position: relative`，会一直往上找到 `<html>`。

### 7.3 水平居中三件套

```css
/* 块级元素自身居中 */
.box {
    width: 600px;
    margin: 0 auto;
}

/* 文字/行内元素居中 */
.text {
    text-align: center;
}

/* 子元素在父元素里居中（推荐） */
.parent {
    display: flex;
    justify-content: center;
    align-items: center;
}
```

---

## 8. Flex 布局

Flex 是目前最常用的布局方式，一维（一行或一列）。

```css
.container {
    display: flex;
    flex-direction: row;          /* 横向（默认）：row / column */
    justify-content: center;      /* 主轴对齐 */
    align-items: center;          /* 交叉轴对齐 */
    gap: 12px;                    /* 子元素间距 */
    flex-wrap: wrap;              /* 放不下换行 */
}
```

`justify-content` 的常用值：

| 值 | 效果 |
| --- | --- |
| `flex-start` | 靠左（默认） |
| `flex-end` | 靠右 |
| `center` | 居中 |
| `space-between` | 两端对齐，中间平分 |
| `space-around` | 每个元素两侧平分 |
| `space-evenly` | 间距完全相等 |

`align-items` 的常用值：`flex-start` / `flex-end` / `center` / `stretch`（拉伸填满）。

子元素常用属性：

```css
.item {
    flex: 1;              /* 平分剩余空间 */
    flex: 0 0 200px;      /* 不放大不缩小，固定 200px */
    align-self: flex-end; /* 单独设置对齐 */
}
```

经典布局示例：

```css
/* 导航栏：左 logo，右菜单 */
.navbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 24px;
}

/* 卡片列表自动换行 */
.cards {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
}
.card {
    flex: 1 1 240px;      /* 最小 240px，能伸能缩 */
}

/* 左中右三栏，中间自适应 */
.layout {
    display: flex;
}
.sidebar {
    width: 200px;
}
.content {
    flex: 1;
}
```

---

## 9. Grid 网格布局

Grid 是二维布局（行 + 列），适合做页面整体框架、卡片网格。

```css
.grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);   /* 三等分 */
    gap: 16px;
}

/* 固定 + 自适应 */
.grid2 {
    display: grid;
    grid-template-columns: 200px 1fr;
    gap: 12px;
}

/* 自动填满，每列最小 240px */
.grid3 {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
}
```

跨行跨列：

```css
.item {
    grid-column: span 2;      /* 跨 2 列 */
    grid-row: span 2;         /* 跨 2 行 */
}

/* 或者写起止线 */
.item2 {
    grid-column: 1 / 3;       /* 从第 1 条线到第 3 条线 */
}
```

Flex 和 Grid 怎么选：

| 场景 | 用 |
| --- | --- |
| 一行按钮、导航栏、左右分栏 | Flex |
| 卡片网格、整页布局 | Grid |
| 不确定选哪个 | 先 Flex，不够用再 Grid |

---

## 10. 响应式与单位

### 10.1 单位

| 单位 | 说明 |
| --- | --- |
| `px` | 像素，固定 |
| `%` | 相对父元素 |
| `em` | 相对当前元素字号 |
| `rem` | 相对根元素（`html`）字号，常用 |
| `vw` / `vh` | 视口宽 / 高的 1% |

### 10.2 媒体查询

```css
/* 桌面默认样式 */
.container {
    width: 1200px;
    margin: 0 auto;
}

/* 屏幕宽度 ≤ 768px 时 */
@media (max-width: 768px) {
    .container {
        width: 100%;
        padding: 0 12px;
    }

    .navbar {
        flex-direction: column;
    }
}
```

断点建议（从小屏写起也可以）：

| 断点 | 设备 |
| --- | --- |
| `max-width: 480px` | 手机 |
| `max-width: 768px` | 平板 |
| `max-width: 1024px` | 小笔记本 |
| 大于 1024 | 桌面 |

`head` 里别忘了：

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0">
```

没有它，手机上会按 980px 渲染再缩小，媒体查询也失效。

---

## 11. 过渡与动画

### 11.1 transition（状态切换的过渡）

```css
.btn {
    background: #2f6fdd;
    transition: background 0.3s ease, transform 0.2s ease;
}

.btn:hover {
    background: #1f5fd0;
    transform: translateY(-2px);
}
```

`transition: 属性 时长 缓动 延迟;`

| 缓动 | 效果 |
| --- | --- |
| `ease` | 默认，先快后慢 |
| `linear` | 匀速 |
| `ease-in` | 慢进 |
| `ease-out` | 慢出 |

### 11.2 transform（变形）

```css
.box {
    transform: translate(10px, 20px);   /* 位移 */
    transform: scale(1.1);              /* 放大 */
    transform: rotate(45deg);           /* 旋转 */
    transform: translateX(-50%);        /* 水平位移 */
}
```

### 11.3 animation + keyframes（循环动画）

```css
@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.loading {
    animation: spin 1s linear infinite;
}

@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}

.modal {
    animation: fadeIn 0.3s ease;
}
```

`animation: 名称 时长 缓动 延迟 次数 方向 填充模式;`

---

## 12. 常用组件样式

### 12.1 按钮

```css
.btn {
    display: inline-block;
    padding: 8px 20px;
    border: none;
    border-radius: 6px;
    background: #2f6fdd;
    color: #fff;
    font-size: 14px;
    cursor: pointer;
    transition: background 0.2s;
}

.btn:hover {
    background: #1f5fd0;
}

.btn:disabled {
    background: #ccc;
    cursor: not-allowed;
}
```

### 12.2 卡片

```css
.card {
    background: #fff;
    border-radius: 10px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: box-shadow 0.2s, transform 0.2s;
}

.card:hover {
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
}
```

### 12.3 表格

```css
table {
    width: 100%;
    border-collapse: collapse;      /* 合并边框 */
    font-size: 14px;
}

th, td {
    padding: 8px 12px;
    border-bottom: 1px solid #eee;
    text-align: left;
}

th {
    background: #f5f7fa;
    font-weight: 600;
}

tbody tr:hover {
    background: #fafbfc;
}
```

### 12.4 简单表单

```css
input,
textarea,
select {
    width: 100%;
    padding: 8px 10px;
    border: 1px solid #ccc;
    border-radius: 6px;
    font-size: 14px;
    box-sizing: border-box;
}

input:focus {
    border-color: #2f6fdd;
    outline: none;
    box-shadow: 0 0 0 3px rgba(47, 111, 221, 0.15);
}

label {
    display: block;
    margin-bottom: 4px;
    font-size: 14px;
    color: #333;
}
```

---

## 13. 一个完整页面

给 HTML 手册那个「设备数据展示」页面加样式：

```css
/* 基础重置 */
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
    font-size: 15px;
    line-height: 1.8;
    color: #333;
    background: #f2f5f8;
}

/* 页头 */
header {
    background: #fff;
    padding: 16px 24px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

header h1 {
    font-size: 20px;
    margin-bottom: 6px;
}

nav a {
    margin-right: 16px;
    color: #2f6fdd;
    text-decoration: none;
}

/* 主内容 */
main {
    max-width: 900px;
    margin: 24px auto;
    padding: 0 16px;
}

section {
    background: #fff;
    border-radius: 10px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

section h2 {
    font-size: 17px;
    margin-bottom: 12px;
}

/* 表格 */
table {
    width: 100%;
    border-collapse: collapse;
}

th, td {
    padding: 8px 12px;
    border-bottom: 1px solid #eee;
    text-align: left;
}

th {
    background: #f5f7fa;
}

/* 表单 */
label {
    display: block;
    margin: 10px 0 4px;
}

input {
    width: 100%;
    padding: 8px 10px;
    border: 1px solid #ccc;
    border-radius: 6px;
}

button {
    margin-top: 12px;
    padding: 8px 20px;
    border: none;
    border-radius: 6px;
    background: #2f6fdd;
    color: #fff;
    cursor: pointer;
}

footer {
    text-align: center;
    color: #888;
    font-size: 13px;
    padding: 24px 0;
}

/* 手机适配 */
@media (max-width: 600px) {
    main {
        margin: 12px auto;
    }
    section {
        padding: 14px;
    }
}
```

---

## 14. 常见问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| 样式不生效 | 选择器没选中 / 被更高优先级覆盖 | F12 看生效规则 |
| 写了一遍样式没变化 | 权重相同但被后写的覆盖 | 调顺序或加优先级 |
| `absolute` 位置不对 | 父元素没有 `position: relative` | 给父元素加 |
| 元素宽度超出容器 | 默认 `content-box`，padding 撑大 | `box-sizing: border-box` |
| 两个 div 之间有缝隙 | 换行符产生空白 / margin 折叠 | 用 flex + gap |
| 文字垂直不居中 | 只设了 `text-align` | 用 flex 的 `align-items` |
| 手机上看不清 / 缩放 | 没写 viewport | 加 meta viewport |
| 媒体查询不生效 | 写了 `min-width` 和大范围冲突 | 检查断点顺序（从小屏往大写） |
| 图片拉伸变形 | 只设了宽或高 | 设 `object-fit: cover` |
| 字体不生效 | 字体名不对 / 系统没有 | 用系统自带字体兜底 |

---

## 15. 速查表

### 15.1 选择器

| 写法 | 选中 |
| --- | --- |
| `p` | 标签 |
| `.box` | 类 |
| `#app` | id |
| `.a .b` | 后代 |
| `.a > .b` | 子元素 |
| `a:hover` | 悬停 |
| `input:focus` | 聚焦 |
| `li:nth-child(2)` | 第 2 个 |
| `p::before` | 伪元素 |

### 15.2 布局

| 我想…… | 写法 |
| --- | --- |
| 水平居中 | `margin: 0 auto` 或 flex `justify-content: center` |
| 完全居中 | flex + `justify-content` + `align-items: center` |
| 左右分栏 | `display: flex` + `.right { flex: 1 }` |
| 三等分 | `display: grid; grid-template-columns: repeat(3, 1fr)` |
| 自动换行卡片 | flex + `flex-wrap: wrap` + `flex: 1 1 240px` |
| 固定定位 | `position: fixed` |
| 粘性表头 | `position: sticky; top: 0` |

### 15.3 常用属性

| 属性 | 示例 |
| --- | --- |
| 颜色 | `color: #333` / `background: #fff` |
| 字号 | `font-size: 15px` |
| 字重 | `font-weight: 700` |
| 行高 | `line-height: 1.8` |
| 内边距 | `padding: 16px` |
| 外边距 | `margin: 10px 20px` |
| 圆角 | `border-radius: 8px` |
| 边框 | `border: 1px solid #ccc` |
| 阴影 | `box-shadow: 0 2px 8px rgba(0,0,0,.1)` |
| 隐藏 | `display: none` |
| 过渡 | `transition: all .3s ease` |
