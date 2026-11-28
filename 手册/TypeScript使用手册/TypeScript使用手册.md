# TypeScript 使用手册（给 JavaScript 加类型）

> TypeScript = JavaScript + 类型检查。写错类型时编辑器直接标红，不用等运行才发现。
> 这一本讲清楚：怎么装、怎么写类型、泛型和工具类型怎么用、常见报错怎么改。

---

## 目录

1. [TypeScript 是什么](#1-typescript-是什么)
2. [安装与运行](#2-安装与运行)
3. [基础类型](#3-基础类型)
4. [接口与类型别名](#4-接口与类型别名)
5. [函数类型](#5-函数类型)
6. [类](#6-类)
7. [泛型](#7-泛型)
8. [联合、交叉与类型收窄](#8-联合交叉与类型收窄)
9. [枚举](#9-枚举)
10. [类型断言与类型守卫](#10-类型断言与类型守卫)
11. [工具类型](#11-工具类型)
12. [模块与声明文件](#12-模块与声明文件)
13. [实战：给数据模型加类型](#13-实战给数据模型加类型)
14. [常见编译错误](#14-常见编译错误)
15. [速查表](#15-速查表)

---

## 1. TypeScript 是什么

TypeScript（TS）是 JavaScript 的超集：

- 所有 JS 语法在 TS 里都能用；
- 额外提供**静态类型**，编译时检查；
- 最终会被编译成普通 JS 在浏览器/Node 里运行。

```ts
// JavaScript：运行时才知道错了
function add(a, b) {
    return a + b;
}
add(1, "2");        // "12"，不报错，但结果不对

// TypeScript：写的时候就标红
function add(a: number, b: number): number {
    return a + b;
}
add(1, "2");        // 编译报错：类型 "string" 不能赋给 "number"
```

好处：编辑器有补全、重构安全、错误提前发现、团队协作接口清晰。

代价：多一层编译，需要配置。

---

## 2. 安装与运行

### 2.1 安装

```bash
# 在项目目录初始化（如果没有 package.json）
npm init -y

# 安装 TypeScript（开发依赖）
npm install -D typescript

# 生成配置文件
npx tsc --init
```

### 2.2 tsconfig.json 常用配置

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "strict": true,
    "outDir": "./dist",
    "rootDir": "./src",
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true
  },
  "include": ["src/**/*"]
}
```

| 配置 | 作用 |
| --- | --- |
| `target` | 编译成哪个版本的 JS |
| `module` | 模块格式（ESNext / CommonJS） |
| `strict` | 打开全部严格检查（强烈建议） |
| `outDir` | 编译输出目录 |
| `rootDir` | 源码目录 |
| `esModuleInterop` | 兼容 `import` 引入 CommonJS 包 |

### 2.3 编译与运行

```bash
# 编译一次
npx tsc

# 监听文件变化，自动编译
npx tsc --watch

# 直接运行 .ts（需安装 ts-node 或 tsx）
npm install -D ts-node
npx ts-node src/index.ts
```

前端项目一般用 Vite / webpack，它们内置了 TS 编译，不需要手动 `tsc`。

---

## 3. 基础类型

```ts
let n: number = 42;
let s: string = "文字";
let b: boolean = true;
let big: bigint = 100n;
let sym: symbol = Symbol("id");

let nothing: null = null;
let undef: undefined = undefined;

// 数组
let nums: number[] = [1, 2, 3];
let strs: Array<string> = ["a", "b"];

// 元组：固定长度和类型
let pair: [string, number] = ["ages", 13];

// 任意类型（尽量少用）
let anyValue: any = 123;
anyValue = "也可以";

// 未知类型（比 any 安全：用之前必须收窄）
let unknownValue: unknown = 123;

// 没有返回值
function log(msg: string): void {
    console.log(msg);
}

// 永远不会返回（抛异常、死循环）
function fail(msg: string): never {
    throw new Error(msg);
}
```

`any` 和 `unknown` 的区别：

| | `any` | `unknown` |
| --- | --- | --- |
| 赋值给别的类型 | 可以 | 不行，要先判断 |
| 调用方法 | 可以 | 不行，要先判断 |
| 用途 | 暂时不管类型 | 接收外部数据，安全 |

```ts
const data: unknown = await res.json();
if (typeof data === "object" && data !== null) {
    // 收窄之后才能用
}
```

---

## 4. 接口与类型别名

### 4.1 interface

```ts
interface User {
    id: number;
    name: string;
    age?: number;              // 可选属性
    readonly createdAt: string; // 只读
    tags: string[];
    address?: {
        city: string;
        street: string;
    };
}

const user: User = {
    id: 1,
    name: "小明",
    createdAt: "2026-01-01",
    tags: ["student"]
};
```

### 4.2 type

```ts
type Point = {
    x: number;
    y: number;
};

type Id = number | string;             // 联合类型
type Status = "online" | "offline";    // 字面量类型
```

`interface` 和 `type` 的区别：

| | interface | type |
| --- | --- | --- |
| 扩展 | `interface B extends A` | `type B = A & {...}` |
| 同名合并 | 会自动合并 | 不能重复定义 |
| 适用 | 对象结构 | 联合、元组、函数等 |

日常写对象类型用 `interface`，需要联合/组合时用 `type`。

### 4.3 索引签名与任意属性

```ts
interface Dict {
    [key: string]: number;
}

const counts: Dict = { a: 1, b: 2 };
```

---

## 5. 函数类型

```ts
// 参数和返回值
function add(a: number, b: number): number {
    return a + b;
}

// 函数类型变量
let fn: (a: number, b: number) => number;
fn = (x, y) => x + y;

// 可选参数（必须放最后）
function greet(name: string, title?: string): string {
    return title ? `${title} ${name}` : name;
}

// 默认参数
function greet2(name: string = "同学"): string {
    return `你好，${name}`;
}

// 剩余参数
function sum(...nums: number[]): number {
    return nums.reduce((a, b) => a + b, 0);
}

// 解构参数
function show({ name, age }: { name: string; age: number }) {
    console.log(name, age);
}
```

---

## 6. 类

```ts
class Animal {
    public name: string;              // 公开
    private age: number;              // 只能在类内部访问
    protected legs: number = 4;       // 子类可以访问
    readonly species: string;         // 只读

    constructor(name: string, age: number, species: string) {
        this.name = name;
        this.age = age;
        this.species = species;
    }

    speak(): void {
        console.log(`${this.name} 叫了一声`);
    }
}

// 继承
class Dog extends Animal {
    constructor(name: string, age: number) {
        super(name, age, "犬");
    }

    speak(): void {
        console.log("汪汪");
    }
}

// 实现接口
interface Serializable {
    toJson(): string;
}

class Point implements Serializable {
    constructor(public x: number, public y: number) {}

    toJson(): string {
        return JSON.stringify({ x: this.x, y: this.y });
    }
}
```

构造函数参数加 `public` / `private` / `readonly` 会自动声明并赋值（参数属性）：

```ts
class Point {
    constructor(public x: number, public y: number) {}
}
// 等价于自己写 x、y 字段再在构造里赋值
```

抽象类：

```ts
abstract class Shape {
    abstract area(): number;          // 子类必须实现

    describe(): string {
        return `面积：${this.area()}`;
    }
}

class Circle extends Shape {
    constructor(private r: number) {
        super();
    }

    area(): number {
        return Math.PI * this.r * this.r;
    }
}
```

---

## 7. 泛型

泛型让类型像参数一样传进去，用的时候才确定。

```ts
// 泛型函数
function identity<T>(value: T): T {
    return value;
}

identity<number>(42);
identity("abc");            // 自动推断 T 为 string

// 泛型约束
function getLength<T extends { length: number }>(value: T): number {
    return value.length;
}

getLength("abc");           // 3
getLength([1, 2, 3]);       // 3

// 泛型接口
interface ApiResponse<T> {
    code: number;
    msg: string;
    data: T;
}

const resp: ApiResponse<User> = {
    code: 0,
    msg: "ok",
    data: { id: 1, name: "小明", createdAt: "", tags: [] }
};

// 泛型类
class Box<T> {
    constructor(private value: T) {}

    get(): T {
        return this.value;
    }
}

const box = new Box<number>(42);
```

泛型参数命名习惯：`T`（类型）、`K`（键）、`V`（值）、`E`（元素）。

---

## 8. 联合、交叉与类型收窄

### 8.1 联合类型（或）

```ts
type Id = number | string;

function printId(id: Id) {
    console.log(id);
}
```

### 8.2 交叉类型（且）

```ts
type Name = { name: string };
type Age = { age: number };
type Person = Name & Age;      // 必须同时有 name 和 age
```

### 8.3 字面量类型

```ts
type Status = "online" | "offline" | "unknown";

function setStatus(s: Status) {}
setStatus("online");     // ok
setStatus("run");        // 编译报错
```

### 8.4 类型收窄

处理联合类型时，要先「收窄」才能用具体属性：

```ts
function format(value: string | number) {
    if (typeof value === "string") {
        return value.toUpperCase();     // 这里 TS 知道是 string
    }
    return value.toFixed(1);            // 这里知道是 number
}
```

三种常见收窄方式：

```ts
// typeof：基本类型
if (typeof x === "string") { }

// in：判断属性是否存在
if ("name" in obj) { }

// instanceof：类
if (err instanceof Error) { }
```

---

## 9. 枚举

```ts
enum Status {
    Online = "ONLINE",
    Offline = "OFFLINE",
    Unknown = "UNKNOWN"
}

let s: Status = Status.Online;
console.log(s);                    // "ONLINE"

// 数字枚举（默认从 0 开始）
enum Level {
    Low,        // 0
    Mid,        // 1
    High        // 2
}
```

注意：枚举会编译出额外的 JS 代码；如果只是要一组常量，也可以用字面量联合类型：

```ts
type Status2 = "ONLINE" | "OFFLINE" | "UNKNOWN";
```

---

## 10. 类型断言与类型守卫

### 10.1 类型断言（你比编译器更确定）

```ts
const el = document.querySelector("#app") as HTMLElement;
el.style.color = "red";

// 或者（旧写法，和 as 等价，但在 tsx 里不能用）
const el2 = <HTMLElement>document.querySelector("#app");
```

非空断言 `!`：确定不是 null / undefined。

```ts
const el = document.querySelector("#app")!;
```

断言不是「转换」，只是告诉编译器「相信我」，用错了运行时照样报错。

### 10.2 类型守卫

自定义判断函数，返回 `x is T`：

```ts
interface User {
    name: string;
    age: number;
}

function isUser(value: unknown): value is User {
    return typeof value === "object"
        && value !== null
        && "name" in value
        && "age" in value;
}

const data: unknown = JSON.parse(text);
if (isUser(data)) {
    console.log(data.name);       // 这里 TS 认识 data 是 User
}
```

处理接口返回的数据时，类型守卫很有用。

---

## 11. 工具类型

TypeScript 内置了一批「对类型做变换」的工具。

```ts
interface User {
    id: number;
    name: string;
    password: string;
    email?: string;
}

// 全部变可选
type PartialUser = Partial<User>;

// 全部变必填
type RequiredUser = Required<User>;

// 全部变只读
type ReadonlyUser = Readonly<User>;

// 只取几个字段
type PublicUser = Pick<User, "id" | "name">;

// 去掉几个字段（常用于隐藏密码）
type SafeUser = Omit<User, "password">;

// 键值映射
type CountMap = Record<string, number>;

// 函数返回值类型
type Result = ReturnType<typeof getUser>;
```

`Omit` 用得最多：

```ts
function toSafeUser(user: User): Omit<User, "password"> {
    const { password, ...rest } = user;
    return rest;
}
```

---

## 12. 模块与声明文件

### 12.1 模块

```ts
// math.ts
export function add(a: number, b: number): number {
    return a + b;
}

export const PI = 3.14;

export default class Calculator { }

// main.ts
import { add, PI } from "./math";
import Calculator from "./math";
```

### 12.2 声明文件 .d.ts

给没有类型的 JS 库补类型：

```ts
// types/my-lib.d.ts
declare module "my-lib" {
    export function doSomething(a: string): number;
}

// 全局变量
declare const DEBUG: boolean;
```

一般库自带 `@types/xxx`，装一下就有类型：

```bash
npm install -D @types/node
```

---

## 13. 实战：给数据模型加类型

```ts
// 数据模型
interface DeviceRecord {
    id: number;
    device: string;
    temp: number;
    time: string;
}

// 接口返回格式
interface ApiResponse<T> {
    code: number;
    msg: string;
    data: T;
}

// 请求函数
async function loadRecords(limit: number): Promise<ApiResponse<DeviceRecord[]>> {
    const res = await fetch(`/api/ems?limit=${limit}`);
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
    return res.json() as Promise<ApiResponse<DeviceRecord[]>>;
}

// 渲染表格
function renderTable(rows: DeviceRecord[]): void {
    const tbody = document.querySelector("tbody") as HTMLElement;
    tbody.innerHTML = rows.map((row) => `
        <tr>
            <td>${row.device}</td>
            <td>${row.temp.toFixed(1)}</td>
            <td>${row.time}</td>
        </tr>
    `).join("");
}

// 使用
async function init() {
    try {
        const resp = await loadRecords(10);
        if (resp.code !== 0) {
            console.error(resp.msg);
            return;
        }
        renderTable(resp.data);
    } catch (e) {
        console.error(e);
    }
}

init();
```

这段代码里用到了：接口、泛型接口、`Promise` 泛型、类型断言、`map`、模板字符串。

---

## 14. 常见编译错误

| 错误码 / 提示 | 原因 | 解决 |
| --- | --- | --- |
| `TS2322: Type 'string' is not assignable to type 'number'` | 类型不匹配 | 改值或改类型 |
| `TS2345: Argument of type ... is not assignable` | 函数参数类型错 | 检查参数类型 |
| `TS2339: Property 'x' does not exist on type 'Y'` | 类型上没有这个属性 | 检查接口定义，或加属性 |
| `TS7006: Parameter 'x' implicitly has an 'any' type` | 参数没标类型 | 加类型或给默认值 |
| `TS2531: Object is possibly 'null'` | 可能为 null | 先判空或用 `?.` |
| `TS18048: 'x' is possibly 'undefined'` | 可能未定义 | 判空、默认值、`!` |
| `TS2307: Cannot find module 'x'` | 没装类型或路径错 | 装 `@types/x` 或检查路径 |
| `TS7031: Binding element implicitly has 'any'` | 解构没类型 | 标类型 |
| `TS1183: An implementation cannot be declared in ambient contexts` | 声明文件里写了实现 | 删掉函数体 |

处理「可能为 null」的三种写法：

```ts
// ① 判空
if (el !== null) {
    el.textContent = "x";
}

// ② 可选链
el?.setAttribute("data", "1");

// ③ 非空断言（确定不为空时）
const el2 = document.querySelector("#app")!;
```

---

## 15. 速查表

### 15.1 类型标注

| 我想…… | 写法 |
| --- | --- |
| 数字 | `let n: number = 1` |
| 字符串 | `let s: string = "a"` |
| 布尔 | `let b: boolean = true` |
| 数组 | `let arr: number[] = []` |
| 元组 | `let t: [string, number]` |
| 任意 | `any`（少用） |
| 未知 | `unknown`（安全） |
| 无返回 | `void` |
| 永不返回 | `never` |

### 15.2 结构

| 我想…… | 写法 |
| --- | --- |
| 对象结构 | `interface User { id: number }` |
| 可选属性 | `age?: number` |
| 只读 | `readonly id: number` |
| 联合 | `type Id = number \| string` |
| 交叉 | `type A = B & C` |
| 字面量 | `type S = "on" \| "off"` |
| 函数类型 | `let f: (a: number) => void` |

### 15.3 进阶

| 我想…… | 写法 |
| --- | --- |
| 泛型函数 | `function f<T>(x: T): T` |
| 泛型约束 | `<T extends { length: number }>` |
| 泛型接口 | `interface Resp<T> { data: T }` |
| 断言 | `value as User` |
| 非空 | `value!` |
| 类型守卫 | `function isUser(x: unknown): x is User` |
| 工具类型 | `Partial<T>` / `Pick<T, "id">` / `Omit<T, "password">` |
| 只读数组 | `readonly string[]` |

### 15.4 常用命令

| 命令 | 作用 |
| --- | --- |
| `npx tsc` | 编译 |
| `npx tsc --watch` | 监听编译 |
| `npx tsc --init` | 生成 tsconfig |
| `npx ts-node a.ts` | 直接运行 |
| `npm i -D typescript` | 安装 |
| `npm i -D @types/node` | 装 Node 类型 |
