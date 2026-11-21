# Swing 使用手册（Java 桌面界面 · EMS 项目版）

> 面向本项目：`EMS` 环境监测（MySQL + JDBC + `com.github.rayman717` 包结构），
> 从「弹出一个空窗口」讲到「能查数据、能刷新、能插入记录的监控界面」。
> 环境：JDK 17（`misc.xml` 里 languageLevel=JDK_17），Swing 属于 JDK 自带的 `javax.swing`，不需要额外依赖。

---

## 目录

1. [先懂三件事：组件、容器、EDT](#1-先懂三件事组件容器edt)
2. [第一个窗口](#2-第一个窗口)
3. [布局管理器](#3-布局管理器)
4. [常用组件](#4-常用组件)
5. [事件处理](#5-事件处理)
6. [对话框与文件选择](#6-对话框与文件选择)
7. [表格 JTable](#7-表格-jtable)
8. [菜单、工具栏与右键菜单](#8-菜单工具栏与右键菜单)
9. [外观、字体与图标](#9-外观字体与图标)
10. [多线程：别让界面卡死](#10-多线程别让界面卡死)
11. [编译、运行与打包](#11-编译运行与打包)
12. [实战：EMS 数据监控窗口](#12-实战ems-数据监控窗口)
13. [常见错误与坑](#13-常见错误与坑)
14. [表格进阶](#14-表格进阶)
15. [树与列表进阶](#15-树与列表进阶)
16. [输入框与文档模型](#16-输入框与文档模型)
17. [自定义绘制与动画](#17-自定义绘制与动画)
18. [窗口进阶](#18-窗口进阶)
19. [应用结构与 MVC](#19-应用结构与-mvc)
20. [调试与性能](#20-调试与性能)
21. [组件速查补充](#21-组件速查补充)
22. [速查表](#22-速查表)

---

## 1. 先懂三件事：组件、容器、EDT

写 Swing 只有三个概念需要先记住：

**① 组件（Component）**：窗口里看得到的每一个东西。`JButton`、`JLabel`、`JTextField`、`JTable` 都是组件。

**② 容器（Container）**：能装别的组件的组件。`JFrame`（窗口）、`JPanel`（面板）、`JScrollPane`（滚动面板）都是容器。

**③ EDT（事件分发线程）**：Swing 所有绘制和事件回调都在这一条线程里执行。规则只有一条：**界面更新放 EDT，耗时操作放后台**。

一个典型界面的组件树：

```
JFrame（窗口，默认 BorderLayout）
├── JPanel（North，放工具栏）
│   ├── JButton「刷新」
│   └── JButton「添加记录」
├── JScrollPane（Center，包住表格）
│   └── JTable
└── JPanel（South，状态栏）
    └── JLabel「共 50 条记录」
```

Java 代码里对应的写法就是「先建容器、定布局、加组件、显示」：

```java
JFrame frame = new JFrame("EMS");
JPanel top = new JPanel();
top.add(new JButton("刷新"));
frame.add(top, BorderLayout.NORTH);       // add(组件, 位置)
frame.setVisible(true);                   // 最后一步才显示
```

> Swing 组件是「轻量级」的：由 Java 自己绘制，不依赖操作系统控件，所以不同系统上外观一致，但启动比 AWT 稍慢。

---

## 2. 第一个窗口

### 2.1 最小可运行窗口

```java
import javax.swing.SwingUtilities;
import javax.swing.JFrame;

public class HelloSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {          // 在 EDT 上创建界面
            JFrame frame = new JFrame("EMS 环境监测");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);      // 窗口居中
            frame.setVisible(true);                 // 放在最后
        });
    }
}
```

保存为 `HelloSwing.java`，编译运行就能看到一个空窗口。

### 2.2 每一行在做什么

| 代码 | 作用 |
| --- | --- |
| `SwingUtilities.invokeLater(...)` | 把创建界面的代码交给 EDT 执行，避免线程问题 |
| `new JFrame("标题")` | 创建窗口并设置标题 |
| `setDefaultCloseOperation(...)` | 点右上角 × 时做什么，见下表 |
| `setSize(600, 400)` | 窗口宽高（像素） |
| `setLocationRelativeTo(null)` | 屏幕居中；传 `null` 表示相对屏幕 |
| `setVisible(true)` | 真正显示窗口，**漏了它界面永远不出现** |
| `dispose()` | 关闭并释放窗口资源（对话框常用） |

关闭动作的三种常见值：

| 常量 | 点 × 后 |
| --- | --- |
| `JFrame.EXIT_ON_CLOSE` | 退出程序，进程结束 |
| `JFrame.HIDE_ON_CLOSE` | 只隐藏窗口，程序还在后台（默认值） |
| `JFrame.DISPOSE_ON_CLOSE` | 关闭当前窗口，释放资源，其他窗口不受影响 |
| `JFrame.DO_NOTHING_ON_CLOSE` | 什么都不做，配合 `WindowListener` 自定义 |

### 2.3 窗口和内容面板

`JFrame.add(...)` 实际是加到了它的内容面板（`getContentPane()`），默认布局是 `BorderLayout`。要改整体布局：

```java
frame.setLayout(new BorderLayout());   // 等价于 frame.getContentPane().setLayout(...)
JPanel panel = new JPanel();
frame.add(panel, BorderLayout.CENTER);
```

### 2.4 窗口大小和最小尺寸

```java
frame.setSize(800, 600);
frame.setMinimumSize(new Dimension(600, 400));   // 用户拖动缩小时的下限
frame.pack();                                    // 或者按内容自动算大小（对话框常用）
```

---

## 3. 布局管理器

Swing 不用手写坐标，靠布局管理器自动排。**界面乱、组件重叠、组件不显示，八成是布局用错了。**

| 布局 | 特点 | 适合 |
| --- | --- | --- |
| `BorderLayout` | 分东南西北中五区，中区会拉伸 | 窗口整体框架 |
| `FlowLayout` | 从左到右排，放不下换行，默认居中 | 工具栏、按钮组 |
| `GridLayout` | 等分成网格 | 表单、九宫格按钮 |
| `GridBagLayout` | 最灵活也最啰嗦，可设权重和跨行列 | 复杂表单 |
| `BoxLayout` | 单方向排列，可加弹簧和间距 | 侧边栏、竖排按钮 |
| `null`（绝对布局） | 自己 `setBounds` 指定坐标 | 不推荐 |

### 3.1 BorderLayout

```java
JFrame frame = new JFrame("BorderLayout");
frame.setLayout(new BorderLayout(8, 8));   // 第二、三参数是水平/垂直间距
frame.add(new JButton("北"), BorderLayout.NORTH);
frame.add(new JButton("南"), BorderLayout.SOUTH);
frame.add(new JButton("西"), BorderLayout.WEST);
frame.add(new JButton("东"), BorderLayout.EAST);
frame.add(new JButton("中"), BorderLayout.CENTER);   // 只有中区会跟着窗口伸缩
```

注意：同一个区域重复 `add` 会覆盖前一个，只保留最后一个。

### 3.2 FlowLayout

```java
JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));   // 左对齐，水平间距 10，垂直 8
panel.add(new JButton("刷新"));
panel.add(new JButton("添加"));
panel.add(new JButton("删除"));
```

`JPanel` 的默认布局就是 `FlowLayout`，所以 `new JPanel()` 不加布局参数时就是「一行按钮」。

### 3.3 GridLayout

```java
JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));   // 3 行 2 列，间距 8
form.add(new JLabel("光照 ALI："));
form.add(new JTextField());
form.add(new JLabel("温度 Temp："));
form.add(new JTextField());
form.add(new JLabel("湿度 Humidity："));
form.add(new JTextField());
```

每个格子大小相同。组件数超过格子数时会自动增加行。

### 3.4 GridBagLayout（登录表单示例）

```java
JPanel panel = new JPanel(new GridBagLayout());
GridBagConstraints c = new GridBagConstraints();
c.insets = new Insets(6, 6, 6, 6);   // 四周留白
c.fill = GridBagConstraints.HORIZONTAL;
c.weightx = 1;                        // 横向拉伸权重

c.gridx = 0; c.gridy = 0;
panel.add(new JLabel("用户名："), c);

c.gridx = 1;
panel.add(new JTextField(15), c);

c.gridx = 0; c.gridy = 1;
panel.add(new JLabel("密　码："), c);

c.gridx = 1;
panel.add(new JPasswordField(15), c);

c.gridx = 0; c.gridy = 2; c.gridwidth = 2;   // 占两列
panel.add(new JButton("登录"), c);
```

关键参数：`gridx/gridy` 位置、`gridwidth/gridheight` 跨几格、`weightx/weighty` 拉伸权重、`fill` 填充方式、`anchor` 对齐方向。

### 3.5 BoxLayout

```java
JPanel side = new JPanel();
side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));   // 竖向排列
side.add(new JButton("刷新"));
side.add(Box.createVerticalStrut(10));                   // 固定 10 像素空隙
side.add(new JButton("导出"));
side.add(Box.createVerticalGlue());                      // 弹簧，把上面的组件顶上去
side.add(new JButton("退出"));
```

横向用 `BoxLayout.X_AXIS`，配合 `Box.createHorizontalStrut(n)` 和 `createHorizontalGlue()`。

### 3.6 绝对布局（为什么不推荐）

```java
panel.setLayout(null);              // 关闭布局管理器
button.setBounds(20, 20, 120, 32);  // x, y, 宽, 高
panel.add(button);
```

窗口一放大组件位置就错、换字体就重叠，除非做「固定尺寸的小工具窗口」，否则别用。

### 3.7 组合布局的思路

实际界面几乎都是嵌套：外层 `BorderLayout` 定框架，中间放 `JPanel`（`GridLayout` 做表单、`FlowLayout` 放按钮），底部状态栏 `BorderLayout` 或者单独 `JPanel`。

```java
JFrame frame = new JFrame("组合布局");
frame.setLayout(new BorderLayout(6, 6));

JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
toolbar.add(new JButton("刷新"));
toolbar.add(new JButton("添加"));
frame.add(toolbar, BorderLayout.NORTH);

JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
form.add(new JLabel("温度："));
form.add(new JTextField(10));
form.add(new JLabel("湿度："));
form.add(new JTextField(10));
frame.add(form, BorderLayout.CENTER);

frame.add(new JLabel("就绪"), BorderLayout.SOUTH);
```

---

## 4. 常用组件

### 4.1 全部组件一页速查

| 想做什么 | 用哪个组件 | 关键方法 |
| --- | --- | --- |
| 显示文字 | `JLabel` | `setText`、`setHorizontalAlignment` |
| 点一下执行动作 | `JButton` | `addActionListener`、`setEnabled` |
| 单行输入 | `JTextField` | `getText`、`setText`、`setColumns` |
| 密码输入 | `JPasswordField` | `getPassword()` 返回 `char[]` |
| 多行文本 | `JTextArea` | `append`、`setLineWrap`，配 `JScrollPane` |
| 勾选 | `JCheckBox` | `isSelected`、`setSelected` |
| 单选（互斥） | `JRadioButton` + `ButtonGroup` | `isSelected` |
| 下拉选择 | `JComboBox` | `addItem`、`getSelectedItem` |
| 列表 | `JList` + `DefaultListModel` | `addElement`、`getSelectedValue` |
| 表格 | `JTable` + `DefaultTableModel` | 见第 7 章 |
| 数字/日期调节 | `JSpinner` | `getValue`、`SpinnerNumberModel` |
| 拖动取值 | `JSlider` | `getValue`、`addChangeListener` |
| 进度 | `JProgressBar` | `setValue`、`setStringPainted(true)` |
| 分页 | `JTabbedPane` | `addTab` |
| 左右分栏 | `JSplitPane` | `HORIZONTAL_SPLIT`、`VERTICAL_SPLIT` |
| 滚动 | `JScrollPane` | 把组件包起来 |
| 弹窗提示 | `JOptionPane` | 见第 6 章 |
| 选文件 | `JFileChooser` | 见第 6 章 |
| 菜单 | `JMenuBar` + `JMenu` + `JMenuItem` | 见第 8 章 |

### 4.2 文本输入

```java
JLabel label = new JLabel("温度：");
JTextField tempField = new JTextField(10);          // 10 列宽（约 10 个字符）
tempField.setText("25.0");
String text = tempField.getText().trim();           // 取值，务必 trim

JPasswordField pwdField = new JPasswordField(10);
char[] pwd = pwdField.getPassword();                // 不返回 String，是出于安全考虑
String pwdStr = new String(pwd);

JTextArea logArea = new JTextArea(8, 30);           // 8 行 30 列
logArea.setLineWrap(true);                          // 自动换行
logArea.setWrapStyleWord(true);                     // 按单词边界换行
logArea.append("开始读取数据…" + System.lineSeparator());
JScrollPane logScroll = new JScrollPane(logArea);   // 必须包滚动面板
```

`JTextField` 没有原生占位符（placeholder），想实现「请输入温度」的灰字提示，用 `FocusListener` 在获得/失去焦点时切换文字和颜色，或者干脆用 `JLabel` 提示。

### 4.3 勾选与单选

```java
JCheckBox autoRefresh = new JCheckBox("自动刷新", true);   // 第二个参数是默认是否选中
boolean on = autoRefresh.isSelected();

JRadioButton c1 = new JRadioButton("按天", true);
JRadioButton c2 = new JRadioButton("按小时");
ButtonGroup group = new ButtonGroup();   // 放进同一个组才会互斥
group.add(c1);
group.add(c2);
```

注意：`JRadioButton` 不加进 `ButtonGroup` 就不会互斥，多个能同时选中。

### 4.4 下拉框

```java
JComboBox<String> combo = new JComboBox<>(new String[]{"最近 10 条", "最近 50 条", "全部"});
combo.setSelectedIndex(0);
combo.addActionListener(e -> {
    String choice = (String) combo.getSelectedItem();
    System.out.println("选了：" + choice);
});
```

`getSelectedItem()` 返回 `Object`，泛型写法可以强转成 `String`。

### 4.5 列表

```java
DefaultListModel<String> listModel = new DefaultListModel<>();
listModel.addElement("记录 A");
listModel.addElement("记录 B");

JList<String> list = new JList<>(listModel);
list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
list.addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting()) {                 // 拖动选择时会触发多次，过滤一下
        System.out.println(list.getSelectedValue());
    }
});

JScrollPane listScroll = new JScrollPane(list);
```

直接让 `JList` 长在窗口里不放 `JScrollPane`，内容一多就显示不全。

### 4.6 微调器、滑块、进度条

```java
JSpinner spinner = new JSpinner(new SpinnerNumberModel(25.0, -20.0, 60.0, 0.5)); // 初值/最小/最大/步长
double value = ((Number) spinner.getValue()).doubleValue();

JSlider slider = new JSlider(0, 100, 40);
slider.setPaintTicks(true);
slider.setMajorTickSpacing(20);
slider.setPaintLabels(true);

JProgressBar bar = new JProgressBar(0, 100);
bar.setValue(35);
bar.setStringPainted(true);   // 显示「35%」
```

### 4.7 分页与分栏

```java
JTabbedPane tabs = new JTabbedPane();
tabs.addTab("实时监控", monitorPanel);
tabs.addTab("历史查询", historyPanel);
tabs.addTab("设置", settingPanel);

JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
split.setDividerLocation(220);          // 分隔条位置
split.setResizeWeight(0.2);             // 拖动窗口时左栏占 20% 的伸缩量
```

### 4.8 滚动面板

```java
JScrollPane scroll = new JScrollPane(table);
scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
```

规则很简单：`JTable`、`JList`、`JTextArea` 这类内容会变长的组件，一律包一层 `JScrollPane`。

---

## 5. 事件处理

### 5.1 按钮点击的三种写法

```java
JButton btn = new JButton("刷新");

// ① lambda（最推荐，ActionListener 是函数式接口）
btn.addActionListener(e -> loadData());

// ② 匿名内部类（需要访问旧版 Java 或写多行时）
btn.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        loadData();
    }
});

// ③ 方法引用
btn.addActionListener(this::loadData);   // loadData(ActionEvent e) 时可用

// ④ 只写一次、处处复用的按钮监听（实现 ActionListener 的类）
```

### 5.2 常用监听器

| 监听器 | 什么时候触发 | 常用场景 |
| --- | --- | --- |
| `ActionListener` | 按钮点击、回车、菜单项 | 按钮、菜单 |
| `ItemListener` | 勾选/取消勾选 | `JCheckBox` |
| `ChangeListener` | 值变化 | `JSlider`、`JSpinner`、`JTabbedPane` |
| `DocumentListener` | 文本框内容变化 | 实时校验、搜索框 |
| `ListSelectionListener` | 列表/表格选中行变化 | `JList`、`JTable` |
| `MouseListener` | 鼠标点击/双击 | 右键菜单、双击打开 |
| `WindowListener` | 窗口打开/关闭 | 关窗前确认 |
| `KeyListener` | 键盘按键 | 不建议，用键绑定替代 |

### 5.3 事件源与动作命令

```java
JButton save = new JButton("保存");
JButton open = new JButton("打开");

ActionListener handler = e -> {
    JButton source = (JButton) e.getSource();       // 谁触发的
    System.out.println("点了：" + source.getText());
    System.out.println("命令：" + e.getActionCommand());   // 默认就是按钮文字
};
save.addActionListener(handler);
open.addActionListener(handler);

save.setActionCommand("cmd-save");   // 改动作命令，方便统一判断
```

### 5.4 快捷键与回车键

```java
// 回车键触发某个按钮（登录框、搜索框最常用）
getRootPane().setDefaultButton(refreshButton);   // 在 JFrame / JDialog 里写

// 按钮助记符：Alt + 首字母
JButton ok = new JButton("确定");
ok.setMnemonic('Y');    // 按 Alt+Y 相当于点击

// 菜单快捷键
menuItem.setAccelerator(KeyStroke.getKeyStroke("control S"));

// 全局键绑定（不依赖焦点）：F5 刷新
InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
im.put(KeyStroke.getKeyStroke("F5"), "refresh");
getRootPane().getActionMap().put("refresh", new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
        loadData();
    }
});
```

### 5.5 关窗前确认

```java
frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
frame.addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) {
        int r = JOptionPane.showConfirmDialog(frame, "确定要退出吗？", "退出",
                JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
});
```

`WindowAdapter` 是空实现类，只覆盖你关心的方法，比实现整个 `WindowListener` 省事。

---

## 6. 对话框与文件选择

### 6.1 JOptionPane 五种用法

```java
// ① 提示信息
JOptionPane.showMessageDialog(this, "保存成功", "提示", JOptionPane.INFORMATION_MESSAGE);

// ② 警告 / 错误（图标不同）
JOptionPane.showMessageDialog(this, "请输入数字", "格式错误", JOptionPane.WARNING_MESSAGE);
JOptionPane.showMessageDialog(this, "数据库连接失败", "错误", JOptionPane.ERROR_MESSAGE);

// ③ 确认（返回 int）
int ret = JOptionPane.showConfirmDialog(this, "确定删除这一行？", "确认",
        JOptionPane.YES_NO_OPTION);
if (ret == JOptionPane.YES_OPTION) {
    // 执行删除
}

// ④ 输入（返回 String，取消时返回 null）
String name = JOptionPane.showInputDialog(this, "请输入设备编号：");
if (name != null) {
    System.out.println(name.trim());
}

// ⑤ 自定义组件（表单、复选框之类）
JTextField field = new JTextField("300", 8);
JPanel p = new JPanel(new GridLayout(1, 2, 8, 8));
p.add(new JLabel("光照 ALI："));
p.add(field);
int ok = JOptionPane.showConfirmDialog(this, p, "输入参数",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
if (ok == JOptionPane.OK_OPTION) {
    System.out.println(field.getText());
}
```

`this` 是父窗口：传父窗口对话框会居中在父窗口上；传 `null` 居中在屏幕。

### 6.2 输入校验：别让用户点了确定才发现错

```java
while (true) {
    String text = JOptionPane.showInputDialog(this, "温度（-20 ~ 60）：");
    if (text == null) return;                       // 用户点了取消
    try {
        double v = Double.parseDouble(text.trim());
        if (v < -20 || v > 60) {
            JOptionPane.showMessageDialog(this, "超出范围，请重新输入");
            continue;
        }
        break;                                      // 合法，跳出
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(this, "请输入数字");
    }
}
```

### 6.3 自定义 JDialog

```java
public class AddRecordDialog extends JDialog {
    private final JTextField ali = new JTextField(8);
    private final JTextField temp = new JTextField(8);
    private final JTextField hum = new JTextField(8);
    private boolean confirmed = false;

    public AddRecordDialog(JFrame owner) {
        super(owner, "添加监测记录", true);          // true = 模态，挡住父窗口
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("光照 ALI："));
        form.add(ali);
        form.add(new JLabel("温度："));
        form.add(temp);
        form.add(new JLabel("湿度："));
        form.add(hum);
        add(form, BorderLayout.CENTER);

        JButton ok = new JButton("确定");
        JButton cancel = new JButton("取消");
        ok.addActionListener(e -> { confirmed = true; dispose(); });
        cancel.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(ok);
        bottom.add(cancel);
        add(bottom, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(ok);
        pack();                                     // 按内容算大小
        setLocationRelativeTo(owner);               // 居中在父窗口上
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public double getAli() {
        return Double.parseDouble(ali.getText().trim());
    }
}
```

调用：

```java
AddRecordDialog dialog = new AddRecordDialog(this);
dialog.setVisible(true);        // 模态对话框：这行会阻塞，直到对话框关闭
if (dialog.isConfirmed()) {
    System.out.println(dialog.getAli());
}
```

### 6.4 选文件：JFileChooser

```java
JFileChooser chooser = new JFileChooser();
chooser.setDialogTitle("导入数据文件");
chooser.setFileFilter(new FileNameExtensionFilter("CSV 表格 (*.csv)", "csv", "txt"));

// 打开
if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
    File file = chooser.getSelectedFile();
    System.out.println("选中的文件：" + file.getAbsolutePath());
}

// 保存（会自动询问是否覆盖）
JFileChooser saveChooser = new JFileChooser();
saveChooser.setSelectedFile(new File("ems_export.csv"));   // 默认文件名
if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
    File out = saveChooser.getSelectedFile();
}
```

写完文件后提示一句 `JOptionPane.showMessageDialog(this, "已导出：" + out.getName())`，体验更好。

---

## 7. 表格 JTable

表格是这类监控界面最核心的组件，值得单独一节。

### 7.1 用 DefaultTableModel 建表

```java
String[] columns = {"ID", "光照", "温度", "湿度", "时间"};
DefaultTableModel model = new DefaultTableModel(columns, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;                          // 全部只读，防止用户误改
    }
};
JTable table = new JTable(model);

model.addRow(new Object[]{1, 320.5, 26.4, 55.0, "2026-01-01 08:00:00"});

JScrollPane scroll = new JScrollPane(table);    // 表格必须包滚动面板
```

`new DefaultTableModel(columns, 0)` 的第二个参数是初始行数，0 表示空表。

### 7.2 从数据库结果集填充

```java
String sql = "SELECT id, ALI, Temp, Humidity, `Time` FROM ems ORDER BY id DESC LIMIT 50";
try (Connection con = openConnection();
     Statement st = con.createStatement();
     ResultSet rs = st.executeQuery(sql)) {

    ResultSetMetaData md = rs.getMetaData();
    int columnCount = md.getColumnCount();

    String[] names = new String[columnCount];
    for (int i = 1; i <= columnCount; i++) {
        names[i - 1] = md.getColumnLabel(i);    // 用列别名做表头
    }
    model.setColumnIdentifiers(names);          // 换表头（会顺带清空数据）

    while (rs.next()) {
        Object[] row = new Object[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            row[i - 1] = rs.getObject(i);
        }
        model.addRow(row);
    }
}
```

注意：这段代码必须在 **EDT** 上执行（或者用 SwingWorker 的 `process`/`done` 回到 EDT），直接放在按钮回调里且查询很慢时，界面会卡住——第 10 章给出正确做法。

### 7.3 增删行与清空

```java
model.addRow(new Object[]{...});       // 追加一行
model.insertRow(0, new Object[]{...});// 插到第一行
model.removeRow(2);                    // 删除第 2 行
model.setRowCount(0);                  // 清空全部
int rows = model.getRowCount();        // 现有行数
```

表格数据变了以后如果显示没更新，可以手动 `model.fireTableDataChanged()`；用 `DefaultTableModel` 的增删方法一般会自动刷新。

### 7.4 取选中行的数据

```java
int viewRow = table.getSelectedRow();               // 用户看到的那一行
if (viewRow == -1) {
    JOptionPane.showMessageDialog(this, "请先选中一行");
    return;
}
int modelRow = table.convertRowIndexToModel(viewRow);   // 开启排序后必须转换
Object id = model.getValueAt(modelRow, 0);              // 第 1 列
```

**为什么要 `convertRowIndexToModel`**：用户点「温度」列排序后，表格显示的顺序和模型里的顺序不一样，直接用 viewRow 会取错行。

### 7.5 排序、列宽、行高

```java
table.setAutoCreateRowSorter(true);                       // 点击表头排序
table.setRowHeight(26);                                   // 行高
table.setFillsViewportHeight(true);                       // 数据少时也铺满
table.getColumnModel().getColumn(0).setPreferredWidth(50);
table.getColumnModel().getColumn(4).setPreferredWidth(160);
table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);   // 只能选一行
```

### 7.6 让数字列居中

```java
DefaultTableCellRenderer center = new DefaultTableCellRenderer();
center.setHorizontalAlignment(SwingConstants.CENTER);
table.getColumnModel().getColumn(2).setCellRenderer(center);   // 温度列居中
```

要按条件上色（比如温度 > 30 标红），继承 `DefaultTableCellRenderer` 重写 `getTableCellRendererComponent` 即可。

### 7.7 只读表格的另一种写法

如果想保留「双击进入编辑、但某些列只读」，把 `isCellEditable` 的判断写细：

```java
DefaultTableModel model = new DefaultTableModel(columns, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 0;    // 只有 ID 列可编辑（通常反过来：时间列只读）
    }
};
```

### 7.8 导出为 CSV

```java
StringBuilder sb = new StringBuilder();
for (int c = 0; c < model.getColumnCount(); c++) {
    sb.append(model.getColumnName(c)).append(c == model.getColumnCount() - 1 ? "\n" : ",");
}
for (int r = 0; r < model.getRowCount(); r++) {
    for (int c = 0; c < model.getColumnCount(); c++) {
        sb.append(model.getValueAt(r, c)).append(c == model.getColumnCount() - 1 ? "\n" : ",");
    }
}
Files.write(Paths.get("ems_export.csv"), sb.toString().getBytes(StandardCharsets.UTF_8));
```

用 Excel 打开中文乱码时，可以在文件开头写 UTF-8 BOM（`\uFEFF`）。

---

## 8. 菜单、工具栏与右键菜单

### 8.1 菜单栏

```java
JMenuBar menuBar = new JMenuBar();

JMenu fileMenu = new JMenu("文件");
JMenuItem refreshItem = new JMenuItem("刷新");
refreshItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
refreshItem.addActionListener(e -> loadData());
fileMenu.add(refreshItem);
fileMenu.addSeparator();
JMenuItem exitItem = new JMenuItem("退出");
exitItem.addActionListener(e -> System.exit(0));
fileMenu.add(exitItem);

JMenu helpMenu = new JMenu("帮助");
JMenuItem aboutItem = new JMenuItem("关于");
aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
        "EMS 环境监测 v1.0", "关于", JOptionPane.INFORMATION_MESSAGE));
helpMenu.add(aboutItem);

menuBar.add(fileMenu);
menuBar.add(helpMenu);
setJMenuBar(menuBar);     // JFrame 上设置菜单栏
```

子菜单用 `JMenu` 套 `JMenu`；勾选式菜单用 `JCheckBoxMenuItem`；单选式用 `JRadioButtonMenuItem`。

### 8.2 右键菜单（弹出菜单）

```java
JPopupMenu popup = new JPopupMenu();
JMenuItem delItem = new JMenuItem("删除这一行");
delItem.addActionListener(e -> deleteSelectedRow());
popup.add(delItem);

table.addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        showPopupIfNeeded(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        showPopupIfNeeded(e);
    }

    private void showPopupIfNeeded(MouseEvent e) {
        if (e.isPopupTrigger()) {                 // 不同系统的右键触发时机不同
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);   // 先选中右键那行
            }
            popup.show(table, e.getX(), e.getY());
        }
    }
});
```

`isPopupTrigger()` 在 Windows 上是「松开鼠标」时触发，在 macOS 上可能是「按下」时，所以按下和松开都要判断一遍。

### 8.3 工具栏

```java
JToolBar toolBar = new JToolBar();
toolBar.setFloatable(false);              // 禁止拖动
toolBar.add(new JButton("刷新"));
toolBar.addSeparator();
toolBar.add(new JButton("添加"));
toolBar.add(Box.createHorizontalGlue());  // 把后面的组件推到右边
toolBar.add(new JCheckBox("自动刷新"));
frame.add(toolBar, BorderLayout.NORTH);
```

---

## 9. 外观、字体与图标

### 9.1 切换外观（LookAndFeel）

```java
try {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());   // Windows 原生风格
    // 或者用 Nimbus（跨平台统一、圆润一些）
    // UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
} catch (Exception e) {
    e.printStackTrace();     // 换肤失败不影响运行，用默认风格
}
```

**顺序很重要**：必须在 `new JFrame()` 之前设置，否则已经创建的组件不会变。已存在的窗口可以手动刷新：

```java
SwingUtilities.updateComponentTreeUI(frame);
frame.pack();
```

### 9.2 全局字体（中文显示的关键）

```java
Font font = new Font("微软雅黑", Font.PLAIN, 14);
UIManager.put("Label.font", font);
UIManager.put("Button.font", font);
UIManager.put("TextField.font", font);
UIManager.put("Table.font", font);
UIManager.put("TableHeader.font", font);
UIManager.put("Menu.font", font);
UIManager.put("MenuItem.font", font);
```

最省事的做法：换肤之后遍历所有默认值统一设置（自己写个小工具方法），或者只在主窗口上 `setFont` 并递归子组件。

中文字体常见问题：用 `Dialog`、`SansSerif` 这类逻辑字体时，Java 会挑一个能显示中文的物理字体；但如果系统缺字体或用了纯英文的 `Monospaced`，中文会显示成方框。Windows 上直接指定「微软雅黑」「宋体」最稳。

### 9.3 图标

```java
// ① 文件系统路径（相对程序运行目录）
ImageIcon icon = new ImageIcon("icons/app.png");
JButton btn = new JButton("刷新", icon);

// ② classpath 里的资源（打包进 jar 后也能找到，推荐）
URL url = EmsApp.class.getResource("/icons/app.png");
if (url != null) {
    frame.setIconImage(new ImageIcon(url).getImage());
}
```

打包成 jar 后方式 ① 就失效了（除非图片也一起打进去），所以正式项目用 `getResource`。

### 9.4 高分屏缩放

JDK 9 以后一般自动适配。如果界面在 4K 屏上太小，可以启动时加参数：

```bash
java -Dsun.java2d.uiScale=2 -jar EmsMonitor.jar
```

---

## 10. 多线程：别让界面卡死

### 10.1 为什么界面会卡

所有事件回调都跑在 EDT 上。如果在按钮回调里直接查数据库、`Thread.sleep`、读写大文件，EDT 被占着，界面就「未响应」：按钮点不动、窗口拖不动、进度条不刷新。

**规则：耗时超过几十毫秒的操作，都交给 `SwingWorker`。**

### 10.2 SwingWorker 标准模板

```java
private void loadData() {
    statusLabel.setText("正在读取…");

    new SwingWorker<List<Object[]>, Void>() {
        @Override
        protected List<Object[]> doInBackground() throws Exception {
            List<Object[]> rows = new ArrayList<>();
            try (Connection con = openConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, Temp FROM ems ORDER BY id DESC LIMIT 50")) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getInt("id"), rs.getDouble("Temp")});
                }
            }
            return rows;      // 返回值会交给 done()
        }

        @Override
        protected void done() {
            try {
                List<Object[]> rows = get();          // get() 在 done 里调用，把后台异常抛出来
                model.setRowCount(0);
                for (Object[] row : rows) {
                    model.addRow(row);
                }
                statusLabel.setText("共 " + rows.size() + " 条记录");
            } catch (Exception ex) {
                statusLabel.setText("读取失败：" + ex.getCause());
            }
        }
    }.execute();
}
```

三个要点：

| 方法 | 在哪个线程 | 干什么 |
| --- | --- | --- |
| `doInBackground()` | 后台线程 | 查数据库、读文件等耗时工作 |
| `done()` | EDT | 拿结果更新界面 |
| `publish()` / `process()` | 后台 / EDT | 边算边分批更新界面 |

### 10.3 分批推送数据（适合数据很多的表格）

```java
new SwingWorker<Void, Object[]>() {
    @Override
    protected Void doInBackground() throws Exception {
        try (Connection con = openConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, Temp FROM ems")) {
            while (rs.next()) {
                publish(new Object[]{rs.getInt("id"), rs.getDouble("Temp")});  // 分批
            }
        }
        return null;
    }

    @Override
    protected void process(List<Object[]> chunks) {
        for (Object[] row : chunks) {
            model.addRow(row);      // 在 EDT 上批量加行
        }
    }
}.execute();
```

### 10.4 进度条绑定

```java
JProgressBar bar = new JProgressBar(0, 100);
bar.setStringPainted(true);

SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
    @Override
    protected Void doInBackground() throws Exception {
        for (int i = 1; i <= 100; i++) {
            Thread.sleep(30);        // 模拟耗时
            setProgress(i);          // 自动触发 propertyChange("progress")
        }
        return null;
    }
};
worker.addPropertyChangeListener(evt -> {
    if ("progress".equals(evt.getPropertyName())) {
        bar.setValue((Integer) evt.getNewValue());
    }
});
worker.execute();
```

### 10.5 Swing 的定时器

```java
Timer timer = new Timer(5000, e -> loadData());   // 每 5 秒执行一次，回调在 EDT
timer.start();
timer.stop();
timer.setRepeats(false);                          // 只执行一次（做提示消失之类）
```

注意 `javax.swing.Timer` 和 `java.util.Timer` 是两个类：

| | `javax.swing.Timer` | `java.util.Timer` |
| --- | --- | --- |
| 回调线程 | EDT，可以直接改界面 | 后台线程，改界面要 `invokeLater` |
| 适用 | 界面定时刷新 | 后台调度任务 |

界面刷新用 Swing 的版本，别用错。

### 10.6 取消任务

```java
SwingWorker<Void, Void> worker = new SwingWorker<>() { ... };
worker.execute();
worker.cancel(true);      // true = 如果正在运行，尝试中断
```

后台循环里要检查 `isCancelled()` 才能配合中断及时停下。

---

## 11. 编译、运行与打包

### 11.1 用命令行编译运行

```bash
# 编译到 out 目录（-sourcepath 让 javac 自动找到被引用的类）
javac -encoding UTF-8 -d out -cp "mysql-connector-java-5.1.49.jar" -sourcepath src src\com\github\rayman717\MysqlMain.java

# 运行（Windows 用分号分隔 classpath）
java -cp "out;mysql-connector-java-5.1.49.jar" com.github.rayman717.MysqlMain
```

Linux / macOS 上 classpath 分隔符换成冒号；PowerShell 里写含分号的 classpath 要加引号。

### 11.2 打成可执行 jar

```bash
# 先把 class 编到 out，再打 jar（-C 表示切换目录，. 表示当前目录全部内容）
jar --create --file EmsMonitor.jar --main-class com.github.rayman717.ui.EmsApp -C out .

# 运行
java -jar EmsMonitor.jar
```

双击 jar 能不能打开，取决于系统把 `.jar` 关联给了哪个程序：Windows 上装过 JDK 一般关联 `javaw.exe`，双击就是无控制台窗口运行。更稳的做法是写一个 `.bat`：

```bat
@echo off
java -jar EmsMonitor.jar
pause
```

`pause` 能让你看到报错信息，不然窗口一闪就没了。

### 11.3 在 IntelliJ IDEA 里跑

- 直接右键 `EmsApp.java` → Run，最省事；
- 运行配置里的 VM options 可加 `-Dsun.java2d.uiScale=2` 之类的参数；
- 数据库驱动 `mysql-connector-java-5.1.49.jar` 要留在 Project Structure → Libraries 里，不然运行时报 `ClassNotFoundException` 或 `No suitable driver`。

---

## 12. 实战：EMS 数据监控窗口

把前面所有内容串起来：一个能连 `EMS` 数据库、表格展示最新 50 条记录、支持自动刷新和插入记录的窗口。

### 12.1 完整代码（EmsMonitorFrame.java）

```java
package com.github.rayman717.ui;

import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.ConfigManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * EMS 数据监控窗口。
 * <p>
 * 表格展示 ems 表最近的记录，支持手动刷新、每 5 秒自动刷新、插入新记录。
 * 数据库连接参数复用项目的 ConfigManager / DbConfig。
 */
public class EmsMonitorFrame extends JFrame {

    private static final String[] COLUMNS = {"ID", "光照 ALI", "温度(℃)", "湿度(%)", "记录时间"};
    private static final int LIMIT = 50;

    /** 表格模型：只读，避免用户直接改单元格 */
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private final JLabel statusLabel = new JLabel(" 就绪");
    private final JCheckBox autoRefreshBox = new JCheckBox("自动刷新(5 秒)");
    private final JButton refreshBtn = new JButton("刷新");
    private final JButton addBtn = new JButton("添加记录");
    private final Timer autoTimer;
    private final DbConfig config;

    public EmsMonitorFrame() {
        super("EMS 环境监测 · 数据监控");

        // 1. 读取数据库配置（同项目原有逻辑）
        ConfigManager.initConfig();
        config = ConfigManager.getDbConfig();

        // 2. 窗口基础属性
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(880, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 3. 组装界面
        add(buildToolBar(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        // 4. 事件
        refreshBtn.addActionListener(e -> loadData());
        addBtn.addActionListener(e -> showAddDialog());
        getRootPane().setDefaultButton(refreshBtn);

        autoTimer = new Timer(5000, e -> loadData());
        autoRefreshBox.addActionListener(e -> {
            if (autoRefreshBox.isSelected()) {
                autoTimer.start();
            } else {
                autoTimer.stop();
            }
        });

        // 5. 首次加载
        loadData();
    }

    /** 顶部工具栏 */
    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(refreshBtn);
        bar.addSeparator();
        bar.add(addBtn);
        bar.add(Box.createHorizontalGlue());
        bar.add(autoRefreshBox);
        return bar;
    }

    /** 中部表格 */
    private JComponent buildTablePanel() {
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(170);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        return scroll;
    }

    /** 底部状态栏 */
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    /** 读取数据：耗时操作放后台，界面更新回 EDT */
    private void loadData() {
        statusLabel.setText(" 正在读取…");
        model.setRowCount(0);

        new SwingWorker<Void, Object[]>() {
            private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            @Override
            protected Void doInBackground() throws Exception {
                String sql = "SELECT id, ALI, Temp, Humidity, `Time` FROM ems "
                        + "ORDER BY id DESC LIMIT " + LIMIT;
                try (Connection con = openConnection();
                     Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        publish(new Object[]{
                                rs.getInt("id"),
                                String.format("%.1f", rs.getDouble("ALI")),
                                String.format("%.1f", rs.getDouble("Temp")),
                                String.format("%.1f", rs.getDouble("Humidity")),
                                fmt.format(rs.getTimestamp("Time"))
                        });
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    model.addRow(row);
                }
            }

            @Override
            protected void done() {
                try {
                    get();                        // 把后台异常抛出来
                    statusLabel.setText(" 共 " + model.getRowCount() + " 条记录");
                } catch (Exception ex) {
                    statusLabel.setText(" 读取失败：" + ex.getCause());
                }
            }
        }.execute();
    }

    /** 弹出表单，插入一条记录 */
    private void showAddDialog() {
        JLabel aliLabel = new JLabel("光照 ALI：");
        JLabel tempLabel = new JLabel("温度(℃)：");
        JLabel humLabel = new JLabel("湿度(%)：");
        JTextField aliField = new JTextField("300", 8);
        JTextField tempField = new JTextField("25.0", 8);
        JTextField humField = new JTextField("50.0", 8);

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(aliLabel);
        form.add(aliField);
        form.add(tempLabel);
        form.add(tempField);
        form.add(humLabel);
        form.add(humField);

        int ret = JOptionPane.showConfirmDialog(this, form, "添加监测记录",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ret != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            double ali = Double.parseDouble(aliField.getText().trim());
            double temp = Double.parseDouble(tempField.getText().trim());
            double hum = Double.parseDouble(humField.getText().trim());
            insertRecord(ali, temp, hum);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "三个字段都要填数字", "格式错误",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** 插入记录，同样放后台执行 */
    private void insertRecord(double ali, double temp, double hum) {
        statusLabel.setText(" 正在保存…");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String sql = "INSERT INTO ems (ALI, Temp, Humidity) VALUES (?, ?, ?)";
                try (Connection con = openConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setDouble(1, ali);
                    ps.setDouble(2, temp);
                    ps.setDouble(3, hum);
                    ps.executeUpdate();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadData();                   // 刷新表格
                } catch (Exception ex) {
                    statusLabel.setText(" 保存失败：" + ex.getCause());
                }
            }
        }.execute();
    }

    /** 按项目配置打开数据库连接 */
    private Connection openConnection() throws SQLException {
        String url = "jdbc:mysql://" + config.ip + ":3306/" + config.dbName
                + "?useSSL=false&characterEncoding=utf8";
        return DriverManager.getConnection(url, config.user, config.password);
    }
}
```

启动入口：

```java
package com.github.rayman717.ui;

import javax.swing.SwingUtilities;

public class EmsApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmsMonitorFrame().setVisible(true));
    }
}
```

### 12.2 这个窗口用到了手册里的哪些点

| 界面部分 | 用到的知识 |
| --- | --- |
| 窗口居中、关闭即退出 | 第 2 章 |
| 工具栏在 NORTH、表格在 CENTER、状态栏在 SOUTH | 第 3 章 `BorderLayout` |
| `JToolBar`、`JCheckBox`、`JLabel` | 第 4 章 |
| 按钮点击、复选框选中、回车刷新 | 第 5 章 |
| 添加记录的输入对话框 | 第 6 章 `JOptionPane` |
| 只读表格、排序、列宽、行高 | 第 7 章 `JTable` |
| 查询和插入放后台、`Timer` 自动刷新 | 第 10 章 |
| 从 `ConfigManager` 取连接参数 | 复用项目已有代码 |

### 12.3 运行步骤

```bash
# 1. 编译（在项目根目录 E:\UP\MySQL 下，-sourcepath 会自动编译依赖的类）
javac -encoding UTF-8 -d out -cp "mysql-connector-java-5.1.49.jar" -sourcepath src src\com\github\rayman717\ui\EmsApp.java

# 2. 运行
java -cp "out;mysql-connector-java-5.1.49.jar" com.github.rayman717.ui.EmsApp
```

第一次运行如果 `config.properties` 不存在（例如换电脑克隆项目后），`ConfigManager` 会让你在控制台输入 IP、端口、用户、密码、库名，输入完成后窗口就会连上 `EMS` 库并显示数据。文件已存在时直接读取，不会询问。

> 依赖 `ems` 表和 `user` 表的存在，见 `SqlUtil.CreateTable`。如果表还没建，窗口里会显示「读取失败」，先执行建表语句。

---

## 13. 常见错误与坑

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 界面完全不出现 | 忘了 `setVisible(true)`，或窗口建在后台线程 | 最后一行调用 `setVisible(true)`，整个创建过程放进 `invokeLater` |
| 加了组件但看不到 | 布局没给位置（`BorderLayout` 只放了一个区）或组件大小为 0 | 检查布局与区域，必要时 `revalidate()` + `repaint()` |
| 组件挤成一团 | 用了 `null` 布局或忘了设布局 | 用 `BorderLayout` + `JPanel` 嵌套组合 |
| 界面点了没反应、窗口「未响应」 | 在 EDT 里查数据库、`sleep`、读写大文件 | 改用 `SwingWorker`，见第 10 章 |
| 中文显示成方块 | 字体不含中文 | 指定「微软雅黑」「宋体」，见 9.2 |
| 表格内容显示不全 | 没包 `JScrollPane` | `new JScrollPane(table)` |
| 表格数据更新了但界面不变 | 直接改了 `Object[][]` 或 `Vector` 的副本 | 通过 `model.addRow` / `fireTableDataChanged()` 更新 |
| 排序后取到了错误的行 | 没做 `convertRowIndexToModel` | 见 7.4 |
| 关掉窗口程序还不退出 | 还有非守护线程在跑（自定义线程、连接池、`java.util.Timer`） | `System.exit(0)` 或把线程设成 daemon |
| 报 `HeadlessException` | 运行环境没有图形界面（远程服务器、无桌面） | 只能在有桌面的环境跑，或改用 Web 界面 |
| 运行报 `No suitable driver` | classpath 里没有 JDBC 驱动 jar | `-cp` 加上 `mysql-connector-java-5.1.49.jar` |
| 打包 jar 后图片/配置找不到 | 用了相对文件路径 | 用 `getClass().getResource("/...")`，或把资源一起打进 jar |
| 双击 jar 一闪而过 | 有异常但控制台看不到 | 用 `java -jar xxx.jar` 在终端运行，或写 `.bat` 加 `pause` |
| 字体缩小后重叠 | 固定了组件大小又换了大字体 | 少用 `setPreferredSize` 硬编码，交给布局管理器 |
| 对话框关不掉程序 | 模态对话框没 `dispose()`，或父窗口已关 | 对话框里按钮监听中调用 `dispose()` |

---

## 14. 表格进阶

第 7 章讲的是「能用」，这一章讲「用得好」：自定义模型、按值变色、下拉编辑、过滤搜索、排序、分页。

### 14.1 自定义 TableModel

`DefaultTableModel` 内部用 `Vector` 存数据，适合小表格。数据来自数据库、需要控制列类型和编辑权限时，继承 `AbstractTableModel` 更灵活：

```java
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class EmsTableModel extends AbstractTableModel {

    private final String[] columns = {"ID", "光照", "温度", "湿度", "时间"};
    private final List<Object[]> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;                     // 全部只读
    }

    /** 整批替换数据：清空 + 通知刷新 */
    public void setRows(List<Object[]> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableDataChanged();
    }

    public Object[] getRow(int rowIndex) {
        return rows.get(rowIndex);
    }
}
```

数据变了必须「通知」表格，否则界面不刷新：

| 方法 | 使用场景 |
| --- | --- |
| `fireTableDataChanged()` | 整表数据全变了 |
| `fireTableRowsInserted(first, last)` | 插入了若干行 |
| `fireTableRowsUpdated(first, last)` | 修改了若干行 |
| `fireTableRowsDeleted(first, last)` | 删除了若干行 |
| `fireTableStructureChanged()` | 列变了（列数、列名） |

```java
public void addRow(Object[] row) {
    rows.add(row);
    fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
}

public void removeRow(int index) {
    rows.remove(index);
    fireTableRowsDeleted(index, index);
}
```

### 14.2 按值变色：单元格渲染器

温度超过 30℃ 标红、湿度超标标黄，是最常见的需求。渲染器负责「怎么画」，不负责「存什么」。

```java
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

public class TempCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        setHorizontalAlignment(SwingConstants.CENTER);

        if (!isSelected && value != null) {
            try {
                double temp = Double.parseDouble(value.toString());
                c.setForeground(temp > 30 ? Color.RED : table.getForeground());
            } catch (NumberFormatException ignored) {
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }
}

// 只给温度列（第 3 列）设置
table.getColumnModel().getColumn(2).setCellRenderer(new TempCellRenderer());
```

三个要点：

- 一定要调 `super.getTableCellRendererComponent(...)`，否则选中高亮、边框都会丢；
- 判断 `isSelected`，选中时不要改前景色（否则选中行颜色怪）；
- 渲染器会被**反复调用**（每次重绘都会），里面只做轻量判断，不要查库、不要 new 图片。

### 14.3 用进度条渲染单元格

想让「湿度」列显示成进度条：

```java
public class ProgressRenderer extends JProgressBar implements TableCellRenderer {

    public ProgressRenderer() {
        super(0, 100);
        setStringPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        int v = value == null ? 0 : (int) Double.parseDouble(value.toString());
        setValue(Math.max(0, Math.min(100, v)));
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return this;
    }
}

table.getColumnModel().getColumn(3).setCellRenderer(new ProgressRenderer());
```

### 14.4 下拉框编辑某一列

```java
JComboBox<String> statusCombo = new JComboBox<>(new String[]{"在线", "离线", "未知"});
table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(statusCombo));
```

注意两点：

1. 模型里对应列的 `isCellEditable` 必须返回 `true`；
2. 编辑结果是写回模型的 `setValueAt`，`DefaultTableModel` 会自动处理；自定义模型要重写 `setValueAt`。

```java
@Override
public void setValueAt(Object value, int rowIndex, int columnIndex) {
    rows.get(rowIndex)[columnIndex] = value;
    fireTableCellUpdated(rowIndex, columnIndex);
}
```

### 14.5 表头设置

```java
JTableHeader header = table.getTableHeader();
header.setReorderingAllowed(false);          // 禁止拖动列顺序
header.setFont(new Font("微软雅黑", Font.BOLD, 13));
header.setBackground(new Color(240, 244, 250));
```

自定义表头渲染器（居中、加图标）继承 `DefaultTableCellRenderer`，然后 `header.setDefaultRenderer(...)`。

### 14.6 搜索过滤

输入框一打字，表格只显示匹配行：

```java
TableRowSorter<EmsTableModel> sorter = new TableRowSorter<>(model);
table.setRowSorter(sorter);

JTextField searchField = new JTextField(15);
searchField.getDocument().addDocumentListener(new DocumentListener() {
    private void apply() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // 用 Pattern.quote 防止用户输入被当成正则
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        }
    }

    @Override public void insertUpdate(DocumentEvent e) { apply(); }
    @Override public void removeUpdate(DocumentEvent e) { apply(); }
    @Override public void changedUpdate(DocumentEvent e) { apply(); }
});
```

过滤只影响「显示」，不改变底层数据；取选中行时仍然要 `convertRowIndexToModel`。

### 14.7 自定义排序

数字列如果存成字符串，默认按字典序排（"10" 会排在 "9" 前面）。给排序列指定比较器：

```java
sorter.setComparator(1, Comparator.comparingDouble(
        v -> Double.parseDouble(v.toString())));
sorter.setComparator(4, Comparator.comparing(
        v -> (java.sql.Timestamp) v));
```

### 14.8 隐藏列、固定列宽

```java
// 隐藏 ID 列
TableColumn idColumn = table.getColumnModel().getColumn(0);
idColumn.setMinWidth(0);
idColumn.setMaxWidth(0);
idColumn.setPreferredWidth(0);

// 关闭自动拉伸，让列宽按设置值显示
table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
```

### 14.9 选中行与选中单元格

```java
table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);   // 多选（默认）
table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);             // 单选
table.setCellSelectionEnabled(true);                                     // 单元格级选中

int[] viewRows = table.getSelectedRows();
for (int viewRow : viewRows) {
    int modelRow = table.convertRowIndexToModel(viewRow);
    Object[] row = model.getRow(modelRow);
    // ...
}
```

注意 `getSelectedRows()` 返回的顺序是**用户点击顺序**，不是从上到下，需要排序的话自己排。

### 14.10 分页

数据量大时一次查全部会卡，分页有两种做法：

| 做法 | 说明 |
| --- | --- |
| 数据库分页（推荐） | `LIMIT ? OFFSET ?`，只查当前页 |
| 内存分页 | 一次查全部，`subList` 切页显示，适合几千条以内 |

```java
int pageSize = 50;
int page = 0;                         // 第 0 页

String sql = "SELECT id, ALI, Temp, Humidity, `Time` FROM ems ORDER BY id LIMIT ? OFFSET ?";
try (PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setInt(1, pageSize);
    ps.setInt(2, page * pageSize);
    try (ResultSet rs = ps.executeQuery()) {
        // 填充 model
    }
}
```

界面底部配「上一页 / 下一页 / 第 N 页」和总条数即可。

---

## 15. 树与列表进阶

### 15.1 JTree 基础

树适合展示「有层级」的数据：设备 → 日期 → 记录。

```java
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

DefaultMutableTreeNode root = new DefaultMutableTreeNode("EMS 设备");
DefaultMutableTreeNode dev1 = new DefaultMutableTreeNode("设备 A");
dev1.add(new DefaultMutableTreeNode("2026-01-01"));
dev1.add(new DefaultMutableTreeNode("2026-01-02"));
root.add(dev1);
root.add(new DefaultMutableTreeNode("设备 B"));

DefaultTreeModel treeModel = new DefaultTreeModel(root);
JTree tree = new JTree(treeModel);
tree.setRootVisible(true);
tree.setShowsRootHandles(true);
tree.setRowHeight(24);
```

常用操作：

```java
// 全部展开 / 折叠
for (int i = 0; i < tree.getRowCount(); i++) {
    tree.expandRow(i);
}
tree.collapseRow(0);

// 选中节点
tree.setSelectionPath(new TreePath(dev1.getPath()));

// 读取选中的节点
tree.addTreeSelectionListener(e -> {
    TreePath path = e.getNewLeadSelectionPath();
    if (path != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        System.out.println("选中：" + node.getUserObject());
    }
});

// 动态加节点
DefaultMutableTreeNode child = new DefaultMutableTreeNode("2026-01-03");
treeModel.insertNodeInto(child, dev1, dev1.getChildCount());
```

### 15.2 自定义树渲染器

默认渲染器只显示文字，想加图标或颜色就继承 `DefaultTreeCellRenderer`：

```java
import javax.swing.tree.DefaultTreeCellRenderer;

class EmsTreeRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        Component c = super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);

        String text = value.toString();
        if (text.contains("设备")) {
            setIcon(null);
            setForeground(selected ? Color.WHITE : new Color(30, 100, 200));
        } else {
            setForeground(selected ? Color.WHITE : tree.getForeground());
        }
        return c;
    }
}

tree.setCellRenderer(new EmsTreeRenderer());
```

### 15.3 JList 自定义渲染器

列表的渲染器和表格类似：

```java
class DeviceRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        String text = value.toString();
        setText("● " + text);
        setForeground(isSelected ? Color.WHITE
                : (text.contains("离线") ? Color.GRAY : new Color(0, 150, 0)));
        return c;
    }
}

list.setCellRenderer(new DeviceRenderer());
```

### 15.4 列表与树的联动

「左边选设备，右边查这台设备的数据」是经典布局：

```java
JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(deviceList), new JScrollPane(table));
split.setDividerLocation(200);

deviceList.addListSelectionListener(e -> {
    if (e.getValueIsAdjusting()) {
        return;                       // 拖动选择时避免重复查询
    }
    String device = deviceList.getSelectedValue();
    if (device != null) {
        loadRecords(device);          // 后台查库，更新表格
    }
});
```

---

## 16. 输入框与文档模型

### 16.1 Document 与 DocumentListener

`JTextField` 的内容由 `Document` 管理。想「边输入边校验/统计」，监听 Document 变化：

```java
JTextField tempField = new JTextField(10);
JLabel hint = new JLabel();

tempField.getDocument().addDocumentListener(new DocumentListener() {
    @Override
    public void insertUpdate(DocumentEvent e) {
        check();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        check();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // 纯文本组件不会触发，写空方法即可
    }

    private void check() {
        String text = tempField.getText().trim();
        try {
            double v = Double.parseDouble(text);
            boolean ok = v >= -20 && v <= 60;
            hint.setText(ok ? "✓" : "超出 -20~60 范围");
            hint.setForeground(ok ? new Color(0, 130, 0) : Color.RED);
        } catch (NumberFormatException ex) {
            hint.setText(text.isEmpty() ? "" : "请输入数字");
            hint.setForeground(Color.RED);
        }
    }
});
```

**注意**：不要在 DocumentListener 里直接改同一个 `Document`（会递归触发），要改的话用 `SwingUtilities.invokeLater(...)` 包一层。

### 16.2 DocumentFilter：限制只能输数字

`DocumentListener` 只能「看到」，`DocumentFilter` 能「拦住」。

```java
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

((AbstractDocument) tempField.getDocument()).setDocumentFilter(new DocumentFilter() {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        if (string != null && string.matches("[0-9.]*")) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text == null || text.matches("[0-9.]*")) {
            super.replace(fb, offset, length, text, attrs);
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
    }
});
```

粘贴、删除、替换都会走 `replace` / `remove`，只覆盖 `insertString` 拦不住粘贴。

### 16.3 InputVerifier：失焦时校验

用户切到别的控件时才校验，不通过就不让走：

```java
tempField.setInputVerifier(new InputVerifier() {
    @Override
    public boolean verify(JComponent input) {
        JTextField field = (JTextField) input;
        String text = field.getText().trim();
        try {
            double v = Double.parseDouble(text);
            return v >= -20 && v <= 60;
        } catch (NumberFormatException e) {
            return false;
        }
    }
});
```

校验失败时焦点会停在这个输入框上。

### 16.4 格式化输入：JFormattedTextField

日期、编号这类固定格式，用 `MaskFormatter`：

```java
import javax.swing.text.MaskFormatter;

MaskFormatter dateMask = new MaskFormatter("####-##-##");
dateMask.setPlaceholderCharacter('_');
JFormattedTextField dateField = new JFormattedTextField(dateMask);
dateField.setColumns(10);

MaskFormatter codeMask = new MaskFormatter("EMS-****");
JFormattedTextField codeField = new JFormattedTextField(codeMask);
```

`*` 表示字母或数字，`#` 表示数字；`MaskFormatter` 的构造会抛 `ParseException`，用 try/catch 包一下。

### 16.5 JSpinner 模型

```java
// 数字：初值 25，范围 -20~60，步长 0.5
SpinnerNumberModel tempModel = new SpinnerNumberModel(25.0, -20.0, 60.0, 0.5);
JSpinner tempSpinner = new JSpinner(tempModel);
double temp = ((Number) tempSpinner.getValue()).doubleValue();

// 日期
SpinnerDateModel dateModel = new SpinnerDateModel();
JSpinner dateSpinner = new JSpinner(dateModel);
JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
dateSpinner.setEditor(editor);
```

### 16.6 可编辑 JComboBox 与简单补全

```java
JComboBox<String> combo = new JComboBox<>(new String[]{"ems-01", "ems-02", "ems-03"});
combo.setEditable(true);

// 监听编辑框内容，自动选中匹配项（简单补全）
combo.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
    @Override
    public void keyReleased(KeyEvent e) {
        String text = combo.getEditor().getItem().toString();
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).startsWith(text)) {
                combo.setSelectedIndex(i);
                break;
            }
        }
    }
});
```

补全只是「选中匹配项」，不会自动填字；要做真正的下拉过滤，需要自定义 ComboBoxModel 或过滤列表。

---

## 17. 自定义绘制与动画

### 17.1 重写 paintComponent

Swing 的组件都是「画」出来的。自定义绘图只需继承 `JPanel` 重写 `paintComponent`：

```java
class ChartPanel extends JPanel {
    private final List<Double> values = new ArrayList<>();

    public void setValues(List<Double> values) {
        this.values.clear();
        this.values.addAll(values);
        repaint();                        // 数据变了要重绘
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);          // 必须先调用：清背景
        Graphics2D g2 = (Graphics2D) g;

        // 抗锯齿，让线条更平滑
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 画坐标轴
        g2.setColor(Color.GRAY);
        g2.drawLine(40, 10, 40, h - 30);
        g2.drawLine(40, h - 30, w - 10, h - 30);

        if (values.size() < 2) {
            return;
        }

        // 数据映射到坐标
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double range = Math.max(1e-6, max - min);

        g2.setColor(new Color(220, 70, 70));
        g2.setStroke(new BasicStroke(2f));

        int prevX = -1;
        int prevY = -1;
        for (int i = 0; i < values.size(); i++) {
            int x = 40 + (int) ((w - 60) * i / (double) (values.size() - 1));
            int y = (h - 30) - (int) ((h - 40) * (values.get(i) - min) / range);
            if (prevX >= 0) {
                g2.drawLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
        }
    }
}
```

三条规则：

| 规则 | 原因 |
| --- | --- |
| 先调 `super.paintComponent(g)` | 否则背景不刷新，残留旧图像 |
| 不要在里面查库、读文件 | 每次重绘都会执行，卡死界面 |
| 数据变了调 `repaint()` | 不调就不会重画 |

### 17.2 Graphics2D 常用操作

```java
g2.setColor(new Color(30, 120, 200));               // 颜色
g2.setFont(new Font("微软雅黑", Font.BOLD, 14));      // 字体
g2.setStroke(new BasicStroke(2.0f));                 // 线宽
g2.fillRect(10, 10, 100, 50);                        // 实心矩形
g2.drawRect(10, 10, 100, 50);                        // 空心矩形
g2.fillOval(10, 10, 30, 30);                         // 圆
g2.drawString("温度", 50, 100);                       // 文字
g2.drawLine(0, 0, 100, 100);                         // 线

// 渐变
GradientPaint paint = new GradientPaint(0, 0, new Color(200, 230, 255), 0, 100, Color.WHITE);
g2.setPaint(paint);
g2.fillRect(0, 0, getWidth(), 100);
```

### 17.3 用 Timer 做动画

```java
int[] angle = {0};
Timer timer = new Timer(30, e -> {
    angle[0] = (angle[0] + 3) % 360;
    chartPanel.repaint();
});
timer.start();
```

`javax.swing.Timer` 的回调在 EDT 上，直接 `repaint()` 是安全的；不要用 `Thread.sleep` 做动画。

### 17.4 重绘性能

| 做法 | 说明 |
| --- | --- |
| `repaint()` | 重绘整个组件 |
| `repaint(x, y, w, h)` | 只重绘指定区域，局部动画更快 |
| 缓存 Font / Color / 图片 | 不要每次绘制都 new |
| 避免在 paint 里循环上万次 | 数据先降采样再画 |
| 双缓冲 | Swing 默认开启，一般不用管 |

---

## 18. 窗口进阶

### 18.1 图标、最大化、窗口状态

```java
setIconImage(new ImageIcon("icons/app.png").getImage());
setExtendedState(JFrame.MAXIMIZED_BOTH);          // 最大化
setLocationRelativeTo(null);                       // 居中（非最大化时）
```

### 18.2 记住窗口大小和位置

把窗口的位置、大小存进 `config.properties`，下次打开还原：

```java
private void restoreBounds() {
    Properties prop = loadConfig();
    int x = Integer.parseInt(prop.getProperty("win.x", "-1"));
    int y = Integer.parseInt(prop.getProperty("win.y", "-1"));
    int w = Integer.parseInt(prop.getProperty("win.w", "880"));
    int h = Integer.parseInt(prop.getProperty("win.h", "520"));
    if (x >= 0 && y >= 0) {
        setBounds(x, y, w, h);
    } else {
        setSize(w, h);
        setLocationRelativeTo(null);
    }
}

private void saveBounds() {
    Properties prop = loadConfig();
    prop.setProperty("win.x", String.valueOf(getX()));
    prop.setProperty("win.y", String.valueOf(getY()));
    prop.setProperty("win.w", String.valueOf(getWidth()));
    prop.setProperty("win.h", String.valueOf(getHeight()));
    storeConfig(prop);
}

// 关窗前保存
addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) {
        saveBounds();
    }
});
```

### 18.3 系统托盘

```java
if (SystemTray.isSupported()) {
    SystemTray tray = SystemTray.getSystemTray();
    Image image = Toolkit.getDefaultToolkit().getImage("icons/tray.png");

    PopupMenu menu = new PopupMenu();
    MenuItem showItem = new MenuItem("显示窗口");
    showItem.addActionListener(e -> setVisible(true));
    MenuItem exitItem = new MenuItem("退出");
    exitItem.addActionListener(e -> System.exit(0));
    menu.add(showItem);
    menu.addSeparator();
    menu.add(exitItem);

    TrayIcon trayIcon = new TrayIcon(image, "EMS 监控", menu);
    trayIcon.addActionListener(e -> setVisible(true));
    tray.add(trayIcon);
}
```

注意 `SystemTray.isSupported()` 在无桌面环境会返回 `false`，要先判断。

### 18.4 启动画面

```java
// 程序启动时显示，加载完成后自动消失
SplashScreen splash = SplashScreen.getSplashScreen();
if (splash != null) {
    Graphics2D g = splash.createGraphics();
    g.setColor(Color.WHITE);
    g.drawString("正在连接数据库…", 20, 100);
    splash.update();
    // 加载完成后 splash.close();
}
```

需要在启动参数里指定图片：

```bash
java -splash:icons/splash.png -cp "out;mysql-connector-java-5.1.49.jar" com.github.rayman717.ui.EmsApp
```

### 18.5 打印表格

Swing 的表格自带打印支持：

```java
table.print();          // 弹出打印对话框，直接打印表格
```

带标题的打印：

```java
MessageFormat header = new MessageFormat("EMS 数据报表");
MessageFormat footer = new MessageFormat("第 {0} 页");
table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
```

### 18.6 复制到剪贴板

```java
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

int viewRow = table.getSelectedRow();
if (viewRow >= 0) {
    int modelRow = table.convertRowIndexToModel(viewRow);
    StringBuilder sb = new StringBuilder();
    for (int c = 0; c < model.getColumnCount(); c++) {
        if (c > 0) {
            sb.append('\t');
        }
        sb.append(model.getValueAt(modelRow, c));
    }
    StringSelection selection = new StringSelection(sb.toString());
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
}
```

粘贴到 Excel 时，用制表符 `\t` 分隔列最方便。

### 18.7 文件拖放

把 CSV 文件拖到窗口上自动导入：

```java
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.datatransfer.DataFlavor;
import java.io.File;

new DropTarget(table, new DropTargetAdapter() {
    @Override
    public void drop(DropTargetDropEvent event) {
        try {
            event.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
            @SuppressWarnings("unchecked")
            java.util.List<File> files = (java.util.List<File>)
                    event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            for (File file : files) {
                importCsv(file.toPath());
            }
            event.dropComplete(true);
        } catch (Exception ex) {
            event.dropComplete(false);
        }
    }
});
```

---

## 19. 应用结构与 MVC

### 19.1 为什么分层

界面代码一多，最容易出现「一个 JFrame 里塞了 1000 行」：查库、拼 SQL、更新表格全混在一起。分层的目的只有一个：**改一处不影响别处**。

### 19.2 三层结构

```
view（界面）      只负责显示和收集输入，不含 SQL
service（业务）   校验参数、编排流程、事务
dao（数据访问）   只负责 SQL 与结果集映射
entity（实体）    数据结构：DbConfig、EmsRecord
```

```
com.github.rayman717
├── entity      EmsRecord、DbConfig
├── dao         EmsDao（查库、写库）
├── service     EmsService（业务逻辑）
├── view        界面（EmsMonitorFrame）
└── util        SqlUtil、ConfigManager、LogUtil
```

### 19.3 一个完整的分层例子

**entity**：

```java
package com.github.rayman717.entity;

public class EmsRecord {
    private int id;
    private double ali;
    private double temp;
    private double humidity;
    private String time;

    // getter / setter 省略
}
```

**dao**（只写 SQL）：

```java
package com.github.rayman717.dao;

public class EmsDao {

    public List<EmsRecord> findLatest(int limit) throws SQLException {
        String sql = "SELECT id, ALI, Temp, Humidity, `Time` FROM ems ORDER BY id DESC LIMIT ?";
        List<EmsRecord> list = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmsRecord r = new EmsRecord();
                    r.setId(rs.getInt("id"));
                    r.setAli(rs.getDouble("ALI"));
                    r.setTemp(rs.getDouble("Temp"));
                    r.setHumidity(rs.getDouble("Humidity"));
                    r.setTime(String.valueOf(rs.getTimestamp("Time")));
                    list.add(r);
                }
            }
        }
        return list;
    }
}
```

**service**（业务校验）：

```java
package com.github.rayman717.service;

public class EmsService {
    private final EmsDao dao = new EmsDao();

    public List<EmsRecord> latest(int limit) throws SQLException {
        if (limit <= 0) {
            limit = 10;
        }
        return dao.findLatest(Math.min(limit, 200));
    }
}
```

**view**（只碰界面）：

```java
private void loadData() {
    new SwingWorker<List<EmsRecord>, Void>() {
        @Override
        protected List<EmsRecord> doInBackground() throws Exception {
            return service.latest(50);            // 业务在后台跑
        }

        @Override
        protected void done() {
            try {
                List<EmsRecord> records = get();
                model.setRows(toRows(records));   // 界面只负责显示
                statusLabel.setText("共 " + records.size() + " 条");
            } catch (Exception ex) {
                statusLabel.setText("读取失败：" + ex.getCause());
            }
        }
    }.execute();
}
```

### 19.4 和本项目现有代码的对照

| 现有代码 | 属于哪一层 | 建议 |
| --- | --- | --- |
| `SqlUtil.getConnection` | util | 保留，dao 复用 |
| `ConfigManager` / `DbConfig` | util / entity | 保留 |
| `LogUtil` | util | 保留 |
| `CreateTable` 里的建表 SQL | dao | 移到 `EmsDao` |
| `MysqlMain` 里的连接逻辑 | 入口 | 界面启动应调用 service，不直接拼 SQL |
| 界面里的 `loadData` | view | 只调 service，不写 `SELECT` |

### 19.5 反例

```java
// 反例：界面里直接拼 SQL、开连接
btnRefresh.addActionListener(e -> {
    try (Connection con = DriverManager.getConnection(url, user, pwd);
         Statement st = con.createStatement();
         ResultSet rs = st.executeQuery("SELECT * FROM ems")) {
        while (rs.next()) {
            model.addRow(new Object[]{rs.getInt(1), rs.getDouble(2)});
        }
    } catch (Exception ex) { }
});
```

问题：换库、改表结构、加缓存、写单测时都要改界面代码；而且这里还在 EDT 上做数据库 IO，界面会卡。

---

## 20. 调试与性能

### 20.1 常见异常速查

| 异常 | 常见原因 |
| --- | --- |
| `NullPointerException` | 控件还没创建就调用；`getSelectedRow()` 返回 -1 时没判断 |
| `HeadlessException` | 无图形环境（远程/服务端）跑 Swing |
| `ArrayIndexOutOfBoundsException` | TableModel 的 `getValueAt` 列数对不上 |
| `IllegalStateException: not on FX/EDT` | 在后台线程更新界面（AWT 会抛 `NullPointerException` 或界面错乱） |
| `ConcurrentModificationException` | 遍历集合时另一个线程修改 |
| `IllegalArgumentException: Width (-1) and height (-1)` | 图片路径错，加载出空图 |

### 20.2 布局调试

给组件加个红色边框，一眼看出谁占了多大空间：

```java
panel.setBorder(BorderFactory.createLineBorder(Color.RED));
```

排查完记得删掉。

### 20.3 定位 EDT 卡顿

```bash
jps                       # 找 pid
jstack <pid>              # 看线程栈
```

如果 `AWT-EventQueue-0` 线程卡在 `SocketInputStream.read`、`ResultSet.next`、`Thread.sleep`，说明有耗时操作在 EDT 上，要挪到 `SwingWorker`。

### 20.4 表格大数据性能

| 问题 | 做法 |
| --- | --- |
| 一次查 10 万条 | 分页，只查当前页 |
| 每行都触发重绘 | 批量 `fireTableRowsInserted` 一次通知 |
| 单元格里查库 | 绝对不行，数据在后台一次查好 |
| 频繁 `repaint` | 合并成一帧，用 Timer 限频 |

### 20.5 监听器与内存泄漏

窗口反复开关、每次都 `addActionListener` 而不移除，监听器会越积越多：

```java
// 同一个监听器只加一次
btn.addActionListener(this::refresh);
btn.removeActionListener(this::refresh);   // 需要移除时（注意方法引用每次是新对象，要用变量保存）
```

正确做法：

```java
private final ActionListener refreshListener = e -> refresh();
...
btn.addActionListener(refreshListener);
btn.removeActionListener(refreshListener);
```

### 20.6 捕获 EDT 上的未处理异常

默认情况下 EDT 抛异常只在控制台打印，界面可能处于半坏状态：

```java
Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
    ex.printStackTrace();
    JOptionPane.showMessageDialog(null, "程序出错：" + ex, "错误", JOptionPane.ERROR_MESSAGE);
});
```

### 20.7 启动慢

| 原因 | 处理 |
| --- | --- |
| 启动时连数据库、查大量数据 | 窗口先显示，数据在 `done()` 后再加载 |
| 加载大量图片 | 懒加载、缓存 |
| 一次创建所有面板 | `CardLayout` 按需创建 |

---

## 21. 组件速查补充

| 需求 | 组件 | 关键方法 |
| --- | --- | --- |
| 树形展示 | `JTree` | `expandRow`、`addTreeSelectionListener` |
| 带图标的列表 | `JList` + 渲染器 | `setCellRenderer` |
| 分页切换 | `JTabbedPane` | `addTab`、`getSelectedIndex` |
| 折叠面板 | `JSplitPane` | `setDividerLocation` |
| 显示 HTML | `JEditorPane` | `setContentType("text/html")`、`setPage` |
| 富文本 | `JTextPane` | `StyledDocument` |
| 日期输入 | `JSpinner` + `SpinnerDateModel` | `DateEditor` |
| 格式化输入 | `JFormattedTextField` | `MaskFormatter` |
| 颜色选择 | `JColorChooser` | `showDialog` |
| 打印 | `JTable` | `print()` |
| 托盘 | `TrayIcon` | `SystemTray.add` |
| 启动画面 | `SplashScreen` | `getSplashScreen` |
| 剪贴板 | `Toolkit` | `getSystemClipboard` |
| 拖放 | `DropTarget` | `DropTargetAdapter` |
| 进度对话框 | `ProgressMonitor` | `setProgress`、`setNote` |

几个常用代码片段：

```java
// 显示一段 HTML
JEditorPane pane = new JEditorPane();
pane.setContentType("text/html");
pane.setText("<h2>EMS 环境监测</h2><p>温度：26.4℃</p>");
pane.setEditable(false);

// 颜色选择
Color color = JColorChooser.showDialog(this, "选择颜色", Color.WHITE);

// 进度对话框
ProgressMonitor monitor = new ProgressMonitor(this, "正在导出…", "", 0, 100);
monitor.setProgress(50);
monitor.setNote("已处理 50%");
```

---

## 22. 速查表

### 14.1 建界面

| 我想…… | 代码 |
| --- | --- |
| 创建窗口 | `JFrame f = new JFrame("标题");` |
| 设置大小并居中 | `f.setSize(800, 600); f.setLocationRelativeTo(null);` |
| 显示 | `f.setVisible(true);` |
| 关闭即退出 | `f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);` |
| 加组件到某区 | `f.add(comp, BorderLayout.CENTER);` |
| 换布局 | `f.setLayout(new BorderLayout());` |
| 按内容自适应大小 | `f.pack();` |

### 14.2 组件

| 我想…… | 代码 |
| --- | --- |
| 按钮 | `new JButton("刷新");` |
| 取输入框文字 | `field.getText().trim()` |
| 密码 | `new String(pwdField.getPassword())` |
| 多行文本 | `new JTextArea(8, 30)` + `new JScrollPane(area)` |
| 勾选状态 | `checkBox.isSelected()` |
| 下拉选项 | `(String) combo.getSelectedItem()` |
| 表格 | `new JTable(model)` + `new JScrollPane(table)` |
| 弹提示 | `JOptionPane.showMessageDialog(this, "ok");` |
| 弹确认 | `JOptionPane.showConfirmDialog(this, "确定？", "确认", JOptionPane.YES_NO_OPTION)` |

### 14.3 交互与线程

| 我想…… | 代码 |
| --- | --- |
| 按钮点击执行 | `btn.addActionListener(e -> doSomething());` |
| 回车触发按钮 | `getRootPane().setDefaultButton(btn);` |
| 后台加载数据 | `new SwingWorker<...>() { ... }.execute();` |
| 回到 EDT 更新 | `SwingUtilities.invokeLater(() -> label.setText("ok"));` |
| 每 5 秒刷新 | `new Timer(5000, e -> loadData()).start();` |
| 关窗前确认 | `setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)` + `WindowListener` |

### 14.4 表格

| 我想…… | 代码 |
| --- | --- |
| 只读表格 | 覆盖 `isCellEditable` 返回 `false` |
| 加一行 | `model.addRow(new Object[]{...});` |
| 清空 | `model.setRowCount(0);` |
| 排序 | `table.setAutoCreateRowSorter(true);` |
| 取选中行 | `table.convertRowIndexToModel(table.getSelectedRow())` |
| 设列宽 | `table.getColumnModel().getColumn(i).setPreferredWidth(120);` |
