# Git 仓库使用手册（E:\UP\MySQL 项目版）

> 本仓库：远程 `origin` = `git@github.com:Ray-man717/Xiaoman-MySQL.git`（SSH 方式），默认分支 `master`。
> 本机 Git 版本 2.53，命令以新版 `switch` / `restore` 写法为主，同时给出老版本兼容写法。

---

## 目录

1. [先建立心智模型：三个区域](#1-先建立心智模型三个区域)
2. [首次配置（换电脑必做）](#2-首次配置换电脑必做)
3. [日常四步循环](#3-日常四步循环)
4. [看懂状态和历史](#4-看懂状态和历史)
5. [撤销、回退与纠错](#5-撤销回退与纠错)
6. [分支与合并](#6-分支与合并)
7. [远程仓库（GitHub）操作](#7-远程仓库github操作)
8. [.gitignore：哪些文件不该进仓库](#8-gitignore哪些文件不该进仓库)
9. [本项目的完整实操流程](#9-本项目的完整实操流程)
10. [中文、换行与编码](#10-中文换行与编码)
11. [常见报错急救](#11-常见报错急救)
12. [场景速查表](#12-场景速查表)

---

## 1. 先建立心智模型：三个区域

Git 在本地有三个存放位置，加一个远程仓库，文件按「工作区 → 暂存区 → 本地仓库 → 远程」单向流动：

```
工作区（你正在改的文件）
   │  git add
   ▼
暂存区（index / 即将被提交的快照）
   │  git commit
   ▼
本地仓库（.git 里的提交历史）
   │  git push
   ▼
远程仓库（GitHub 上的 origin）
```

对应的四条命令：

| 动作 | 命令 | 说明 |
| --- | --- | --- |
| 看现在处于什么状态 | `git status` | 每次操作前后都可以先看一眼 |
| 把改动放进暂存区 | `git add 文件` | 决定「这次提交包含哪些内容」 |
| 把暂存区存成一个版本 | `git commit -m "说明"` | 生成一条不可变的提交记录 |
| 把本地提交传到 GitHub | `git push` | 前三步在本地，这一步才对外 |

反向的两条：`git pull` 把远程的新提交拉下来合并；`git restore 文件` 把工作区的改动丢掉、退回上次提交的样子。

记住一句话：**add 选内容，commit 存版本，push 做同步**。

---

## 2. 首次配置（换电脑必做）

```bash
# 身份信息：每条提交都会记录，写错了历史里也会带着错
git config --global user.name "xiaoman"
git config --global user.email "man_0723_ruilin@outlook.com"

# Windows 上建议的几项
git config --global core.autocrlf true        # 换行符自动转换，避免整个文件被标记为“改了”
git config --global core.quotepath false      # 中文文件名正常显示，而不是 \346\237\245
git config --global core.longpaths true       # 路径过长时报 Filename too long 的解决办法
git config --global init.defaultBranch main   # 新建仓库默认分支名
git config --global pull.rebase false         # pull 时用 merge 方式合并

# 查看结果
git config --global --list
```

本机当前已经配置好：`user.name = xiaoman`、`user.email = man_0723_ruilin@outlook.com`，仓库里还设置了 `core.quotepath = false`。

> 身份信息错了也不致命：`git config --global user.name "新名字"` 改掉，之后的提交就是对的；旧提交里的名字需要改历史，一般不值得折腾。

---

## 3. 日常四步循环

```bash
cd E:\UP\MySQL

git status -s                      # ① 看有哪些改动
git add 文件或目录                  # ② 挑出这次要提交的内容
git commit -m "一句话说明做了什么"   # ③ 存成一个版本
git push                           # ④ 推到 GitHub
```

### 3.1 add 的几种写法

| 写法 | 作用 |
| --- | --- |
| `git add 文件名` | 只添加这一个文件 |
| `git add .` | 添加当前目录下所有新增和修改（不含已删除） |
| `git add -A` | 添加全部变化，包括删除 |
| `git add -u` | 只添加已跟踪文件的修改和删除，不含新文件 |
| `git add "带空格 的文件"` | 路径有空格加引号 |
| `git add *.java` | 按通配符批量添加 |

### 3.2 commit 的几种写法

```bash
git commit -m "修复数据库连接超时"          # 最常用：直接把说明写在 -m 里
git commit -am "修复数据库连接超时"          # 已跟踪文件的修改跳过 add，直接提交
git commit -m "标题" -m "详细说明"          # 第一段当标题，第二段当正文
git commit                                  # 不带 -m 会打开编辑器写多行说明
```

提交信息写清楚「做了什么 + 为什么」，例如「添加 SQL 查询手册（HTML 网页版）」，比「update」「修改」有用得多。仓库历史是给人看的。

### 3.3 只提交一部分改动

```bash
git add -p 文件名      # 逐块（hunk）确认：y 添加 / n 跳过 / s 拆分更小块 / q 退出
```

当一次改了好几处、只想把其中一处提交时用这个，很实用。

---

## 4. 看懂状态和历史

### 4.1 status 的两列标记

```bash
git status        # 完整输出
git status -s     # 简写：左边一列是暂存区，右边一列是工作区
```

| 标记 | 含义 |
| --- | --- |
| `??` | 未跟踪的新文件（Git 不认识它） |
| `A` | 新文件已 add，等待提交 |
| `M` | 已修改（右列 M = 改了还没 add；左列 M = 已 add） |
| `MM` | 改过又 add 后又改了一次，暂存区与工作区版本不同 |
| `D` | 删除 |
| `UU` | 合并冲突，两边都改了同一处 |

### 4.2 log：看历史

```bash
git log --oneline                       # 一行一条，最常用
git log --oneline -5                    # 最近 5 条
git log --oneline --graph --decorate    # 带分支合并图
git log --oneline --all                 # 包括所有分支
git log -p -1                           # 看最后一次提交具体改了什么
git log --stat                          # 看每次提交涉及哪些文件、改了几行
git log --author="xiaoman"              # 按作者筛选
git log --oneline -- 路径               # 只看某个文件/目录的历史
```

### 4.3 diff：看差异

```bash
git diff                # 工作区 vs 暂存区（还没 add 的改动）
git diff --cached       # 暂存区 vs 上次提交（add 了但还没 commit 的内容）
git diff HEAD           # 工作区 vs 上次提交（add 没 add 都算）
git diff 提交1 提交2     # 两个版本之间的差异
git diff -- 文件名       # 只看某个文件
```

### 4.4 show / blame / reflog

```bash
git show 7f78936                 # 看某次提交的内容（不加 hash 就是最新一次）
git show 7f78936:README.md       # 看某次提交里的某个文件
git blame 文件名                  # 每行代码是谁、哪次提交改的
git blame -L 10,20 文件名         # 只看第 10~20 行
git reflog                       # 本地操作流水（包括 reset 掉的提交），救命用
```

---

## 5. 撤销、回退与纠错

这是最容易出错的部分，先看一张表：**「我想做什么 → 用哪条 → 影响哪里」**。

| 我想…… | 命令 | 影响范围 |
| --- | --- | --- |
| 丢掉某个文件没 add 的改动 | `git restore 文件名` | 只影响工作区，改动没了 |
| 把已 add 的文件撤出暂存区 | `git restore --staged 文件名` | 改动还在工作区 |
| 改最后一次提交的说明 | `git commit --amend -m "新说明"` | 改写最后一条提交 |
| 撤销最后一次提交，改动留在工作区 | `git reset --soft HEAD~1` | 提交没了，内容在暂存区 |
| 撤销最后一次提交，改动退出暂存 | `git reset --mixed HEAD~1` | 提交没了，内容在工作区 |
| 彻底回到某个版本（危险） | `git reset --hard 提交号` | 提交和改动一起丢 |
| 撤销已经推送上去的提交 | `git revert 提交号` | 生成一条反向提交，最安全 |
| 临时保存工作现场 | `git stash` / `git stash pop` | 相当于「先藏起来」 |

### 5.1 分开理解 restore 和 reset

- `restore` 只动**文件内容**，不动提交历史：`git restore` / `git restore --staged`；
- `reset` 动的是**指针**：`--soft` 只退提交、`--mixed` 退提交和暂存、`--hard` 连文件一起改回原样。

老版本 Git（2.23 之前）的等价写法：

| 新写法 | 老写法 |
| --- | --- |
| `git restore 文件名` | `git checkout -- 文件名` |
| `git restore --staged 文件名` | `git reset HEAD 文件名` |

### 5.2 reset 的三种模式

```bash
# 场景：刚刚提交错了，想退回去重做
git reset --soft HEAD~1     # 提交撤销，改动全部留在暂存区（可以直接再 commit）
git reset HEAD~1            # 提交撤销，改动留在工作区（--mixed 是默认值）
git reset --hard HEAD~1     # 提交撤销，改动也一并丢弃（危险，确认过再执行）
```

`HEAD~1` 表示「上一个提交」，`HEAD~2` 表示上两个，也可以直接写提交号。

### 5.3 revert：撤销已推送的提交

已经 push 到 GitHub 的提交**不要用 reset**（会把别人的历史也改乱），用 revert 生成一条「反向提交」：

```bash
git revert 7f78936     # 会打开编辑器确认提交信息，保存后生成一条抵消该提交的新提交
git push
```

### 5.4 stash：临时存档

```bash
git stash                 # 把当前未提交的改动藏起来，工作区变干净
git stash -u              # 连未跟踪的新文件一起藏
git stash list            # 看存了哪些
git stash pop             # 取出最近一次并删除记录
git stash apply           # 取出但保留记录
git stash drop            # 丢掉最近一次
```

典型场景：改到一半，来了个紧急修复要切分支，先 `git stash`，处理完切回来 `git stash pop`。

### 5.5 reflog：最后的后悔药

```bash
git reflog                          # 每一项都带提交号
git reset --hard HEAD@{3}           # 回到操作流水里的某一步
git reset --hard 7f78936            # 或者直接回到某个提交号
```

即使 `git reset --hard` 丢掉了提交，30 天内基本都能从 reflog 找回来。

---

## 6. 分支与合并

### 6.1 查看与切换

```bash
git branch                 # 本地分支，* 开头是当前所在
git branch -vv             # 分支 + 最新提交 + 对应的远程分支
git branch -a              # 包括远程分支
git switch master          # 切换分支（新版）
git checkout master        # 切换分支（老版写法）
git switch -c feature-x    # 新建并切换到 feature-x
git checkout -b feature-x  # 老版写法
```

### 6.2 合并

```bash
git switch master          # 先回到要接收改动的分支
git merge feature-x        # 把 feature-x 合并进来
git merge --no-ff feature-x  # 强制生成一个合并提交，历史更清晰
```

### 6.3 解决冲突（最常见的一次卡壳）

```bash
git merge feature-x
# 输出里出现 CONFLICT (content): Merge conflict in 某文件
git status                 # 被标记为 both modified 的就是冲突文件
```

冲突文件里会出现这种标记，把内容改成你最终要的样子，删掉标记行：

```
<<<<<<< HEAD
这是当前分支的内容
=======
这是要合并进来的内容
>>>>>>> feature-x
```

然后：

```bash
git add 冲突文件
git commit                 # 完成这次合并（编辑器里默认的合并信息可以直接用）
```

不想合并了、想回到合并前：

```bash
git merge --abort
```

### 6.4 删除分支

```bash
git branch -d feature-x     # 删除已合并的分支
git branch -D feature-x     # 强制删除没合并的分支（内容可能丢）
git push origin --delete feature-x   # 删除远端分支
```

### 6.5 rebase：把提交“搬”到新基础上

```bash
git switch feature-x
git rebase master
```

rebase 会让历史变成一条直线，但会改写提交号。**已经推送并被别人使用的分支不要 rebase**，否则别人拉取会乱。个人分支、本地整理提交时用它比较合适。

---

## 7. 远程仓库（GitHub）操作

### 7.1 查看与修改远程地址

```bash
git remote -v                                    # 看当前远程
git remote add origin git@github.com:Ray-man717/Xiaoman-MySQL.git   # 添加
git remote set-url origin 新地址                 # 改地址（换 HTTPS / 换仓库时）
git remote remove origin                        # 删除关联
```

### 7.2 fetch 与 pull 的区别

```bash
git fetch origin           # 只把远程的新提交下载到本地，不改你的文件（安全）
git pull                   # fetch + merge，直接把远程改动合并进来
git pull --rebase          # fetch + rebase，历史更直
git pull --ff-only         # 只允许快进合并，有分叉就报错（保守但安全）
```

先 `fetch` 再 `git log origin/master --oneline -5` 看看别人推了什么，确认后再 `merge`，是更稳妥的习惯。

### 7.3 push

```bash
git push                                   # 当前分支已关联远程时，直接推
git push -u origin master                  # 第一次推：建立关联并推送，之后就能直接 git push
git push origin 分支名                     # 推指定分支
git push --tags                            # 推标签
```

本仓库的 `master` 已经关联了 `origin/master`，所以 `git push` 一条命令就够。

### 7.4 把已有的本地仓库推到新 GitHub 仓库

```bash
git remote add origin git@github.com:用户名/仓库名.git
git branch -M main                       # 如果想把主分支改名 main（可选）
git push -u origin master
```

### 7.5 SSH 方式的一次性配置（换电脑要做）

```bash
ssh-keygen -t ed25519 -C "man_0723_ruilin@outlook.com"    # 一路回车，可设密码
cat ~/.ssh/id_ed25519.pub                                  # 复制公钥内容
```

到 GitHub → Settings → SSH and GPG keys → New SSH key，粘贴公钥保存，然后验证：

```bash
ssh -T git@github.com      # 返回 Hi 用户名! You've successfully authenticated... 就通了
```

本机已有 `~/.ssh/id_rsa`，说明之前配过，能正常 push。

---

## 8. .gitignore：哪些文件不该进仓库

### 8.1 本项目的忽略规则

当前 `.gitignore` 已经忽略了这些：

| 规则 | 忽略的内容 | 为什么 |
| --- | --- | --- |
| `SQL.txt` | 数据库服务器账号密码笔记 | 含密码，不能公开 |
| `*.properties` | `config.properties` 等配置 | 含 `db.password`，不能公开 |
| `out/`、`bin/`、`logs/` | 编译输出与日志 | 可重新生成，体积大 |
| `.vscode/`、`.classpath`、`.project`、`.settings/` | IDE 本地配置 | 本机环境相关，别人用不上 |
| `.DS_Store`、`.kotlin` | 系统 / 工具残留文件 | 与本项目无关 |

注意：`.idea/` 和 `*.iml` 目前**还没有**被忽略，所以 `git status` 里会一直出现它们。建议补进 `.gitignore`：

```gitignore
# IntelliJ IDEA 本地配置
.idea/
*.iml

# 依赖包（如果希望仓库轻量，也可以忽略 jar）
*.jar
```

### 8.2 .gitignore 语法

| 写法 | 含义 |
| --- | --- |
| `# 注释` | 注释行 |
| `*.log` | 任意位置的 .log 文件 |
| `/out/` | 只忽略根目录下的 out 目录 |
| `logs/` | 忽略所有名为 logs 的目录 |
| `**/temp` | 任意层级的 temp |
| `!README.md` | 例外：不忽略（放在忽略规则后面才生效） |
| `config.properties` | 忽略指定文件 |

### 8.3 最容易踩的坑：已跟踪的文件，加进 .gitignore 没用

`.gitignore` 只对**未跟踪**文件生效。如果一个文件已经被提交过，再写进 `.gitignore` 依然会被跟踪：

```bash
git rm --cached config.properties     # 从仓库移除但保留本地文件
git rm -r --cached .idea              # 整个目录
git commit -m "移除不该提交的配置文件"
```

检查某个文件是否被忽略、被哪条规则忽略：

```bash
git check-ignore -v config.properties
```

---

## 9. 本项目的完整实操流程

### 9.1 日常改代码的标准动作

```bash
cd E:\UP\MySQL

git status -s                  # 看看改了哪些文件
git add src\                    # 只提交源码改动（也可以 git add . 全加）
git commit -m "优化数据库连接方法"
git push                       # 推到 GitHub
```

### 9.2 换一台电脑接着写

```bash
git clone git@github.com:Ray-man717/Xiaoman-MySQL.git
cd Xiaoman-MySQL                 # 默认用仓库名做目录名（也可以自己指定）
```

clone 之后，`git push` / `git pull` 都不用再配置远程，上游地址已经指好。

### 9.3 两台电脑交替提交的推荐节奏

```bash
# 动手前先同步，减少冲突
git pull

# 干完活
git add .
git commit -m "本次改动说明"
git push
```

如果 `git pull` 提示冲突，按 6.3 的步骤解决；如果提示 `rejected`，说明远程有你先没拉下来的提交，先 `git pull --rebase` 再推。

### 9.4 一次真实提交的示例（本仓库）

```bash
git add "SQL查询手册.md" "Git仓库使用手册.md"
git commit -m "添加 Git 使用手册"
git push
```

---

## 10. 中文、换行与编码

| 问题 | 现象 | 处理 |
| --- | --- | --- |
| 中文文件名显示成乱码 | `"SQL\346\237\245\350\257\242..."` | `git config --global core.quotepath false` |
| 整个文件被标记为已修改 | 明明只改了一行，diff 显示全文件变化 | 换行符问题，`git config --global core.autocrlf true`（Windows） |
| 中文提交信息乱码 | 某些终端里显示问号 | 设置终端为 UTF-8，或 `git config --global i18n.commitEncoding utf-8` |
| 文件名大小写不敏感 | 把 `A.java` 改成 `a.java`，git 不认为有变化 | 仓库 `core.ignorecase = true` 时用 `git mv` 明确改名 |
| 路径太长 | `Filename too long` | `git config --global core.longpaths true` |

---

## 11. 常见报错急救

| 报错 | 原因 | 解决 |
| --- | --- | --- |
| `fatal: not a git repository` | 当前目录不是仓库根目录 | 先 `cd` 到含 `.git` 的目录 |
| `Please tell me who you are` | 没配用户名/邮箱 | `git config --global user.name/user.email` |
| `Permission denied (publickey)` | SSH 公钥没加到 GitHub | 重新 `ssh-keygen` 并粘贴公钥，或改用 HTTPS |
| `! [rejected] ... (non-fast-forward)` | 远程有本地没有的提交 | 先 `git pull --rebase`，解决冲突后再 `git push` |
| `The current branch master has no upstream branch` | 分支没关联远程 | `git push -u origin master` |
| `error: src refspec master does not match any` | 仓库里还没有任何提交 | 先 `git add` + `git commit` |
| `Your local changes would be overwritten by merge` | 有未提交的改动会冲突 | 先 `git stash` 或提交，再 pull |
| `CONFLICT (content)` | 两边改了同一处 | 按 6.3 手工编辑冲突文件后 `git add` + `git commit` |
| `fatal: refusing to merge unrelated histories` | 两个没关系的仓库历史合并 | `git pull origin master --allow-unrelated-histories` |
| 处于 `detached HEAD` 状态 | 检出的是某个提交而不是分支 | `git switch master` 回来，或 `git switch -c 新分支` 保留现场 |
| `fatal: pathspec 'xxx' did not match any files` | 文件名/路径写错 | 用 `git status` 确认准确路径，中文路径加引号 |
| `nothing to commit, working tree clean` | 没有可提交的改动 | 确认是否已经提交过，或改动在别的目录 |
| `RPC failed` / `early EOF`（推送大文件失败） | 体积或网络问题 | 用 SSH 方式；本机已设 `http.postbuffer=524288000` |
| `GH001: Large files detected`（单文件 >100MB） | GitHub 限制 | 移除该文件或改用 Git LFS |
| `error: Your local changes to the following files would be overwritten by checkout` | 切分支会覆盖未提交改动 | 先提交或 `git stash` |

---

## 12. 场景速查表

| 我想…… | 命令 |
| --- | --- |
| 看看当前改了什么 | `git status -s` + `git diff` |
| 只提交其中一部分改动 | `git add -p 文件名` |
| 丢掉某个文件的所有改动 | `git restore 文件名` |
| 把误 add 的文件撤出暂存 | `git restore --staged 文件名` |
| 改刚提交的说明 | `git commit --amend -m "新说明"` |
| 撤销刚提交（还没推） | `git reset --soft HEAD~1` |
| 撤销已推送的提交 | `git revert 提交号` + `git push` |
| 回到某个历史版本看看 | `git switch --detach 提交号`，看完 `git switch master` |
| 找回误删/误 reset 的提交 | `git reflog` 找到提交号，`git reset --hard 提交号` |
| 某行代码是谁改的 | `git blame -L 行号,行号 文件名` |
| 看某次提交改了什么 | `git show 提交号` |
| 临时保存现场去处理别的事 | `git stash` → 回来后 `git stash pop` |
| 同步其他人的提交 | `git pull`（或 `git fetch` 后 `git merge origin/master`） |
| 看远程有哪些分支 | `git branch -r` |
| 新建功能分支并推送 | `git switch -c feature-x` → 提交 → `git push -u origin feature-x` |
| 合并分支并删除 | `git switch master` → `git merge feature-x` → `git branch -d feature-x` |
| 让某个文件不再被跟踪 | `git rm --cached 文件名` + 写进 `.gitignore` |
| 检查文件是否被忽略 | `git check-ignore -v 文件名` |
| 强行让本地和远程一致 | `git fetch` + `git reset --hard origin/master`（会丢弃本地改动） |
| 看所有提交图 | `git log --oneline --graph --all` |

---

## 附：本仓库现状

| 项目 | 当前值 |
| --- | --- |
| 仓库位置 | `E:\UP\MySQL` |
| 当前分支 | `master`（已关联 `origin/master`） |
| 远程地址 | `git@github.com:Ray-man717/Xiaoman-MySQL.git`（SSH） |
| 用户名 / 邮箱 | `xiaoman` / `man_0723_ruilin@outlook.com` |
| 已忽略 | `SQL.txt`、`*.properties`、`out/`、`logs/`、`bin/`、`.vscode/` 等 |
| 建议补进 .gitignore | `.idea/`、`*.iml`（IDE 配置，本机相关） |

常用三条命令记牢即可：`git status` 看状态，`git add` + `git commit` 存版本，`git push` 做同步。
