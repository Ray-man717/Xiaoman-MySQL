package com.github.rayman717.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SwingJFrame extends JFrame {
    public SwingJFrame(String title) {
        setTitle(title);
        Container container = getContentPane();
        setBounds(200, 200, 400, 300); // 稍微改扁一点
        container.setBackground(Color.WHITE);
        container.setLayout(new BorderLayout());

        // 1. 顶部标题
        JLabel label = new JLabel("欢迎使用 EMS 监测系统", SwingConstants.CENTER);
        label.setFont(new Font("微软雅黑", Font.BOLD, 20));
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        container.add(label, BorderLayout.NORTH);

        // 2. 底部登录按钮
        JPanel btnPanel = new JPanel(); // 用一个面板把按钮包起来，防止它被拉伸
        btnPanel.setBackground(Color.WHITE);
        JButton logIn = new JButton("点击登录");
        logIn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        logIn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 弹出登录框
                SignIn text = new SignIn(SwingJFrame.this, "系统登录");
                text.setVisible(true);
            }
        });
        btnPanel.add(logIn);
        container.add(btnPanel, BorderLayout.SOUTH); // 放在底部，自动居中

        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

class SignIn extends JDialog {
    public SignIn(SwingJFrame frame, String title) {
        super(frame, title, true);
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        // 设置弹窗大小，并用 setLocationRelativeTo 让它绝对居中于主窗口
        setSize(300, 150);
        setLocationRelativeTo(frame);

        // 1. 提示文字
        JLabel sign = new JLabel("请输入密码:", SwingConstants.CENTER);
        sign.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        container.add(sign, BorderLayout.NORTH);

        // 2. 密码输入框
        JPasswordField pwdField = new JPasswordField();
        pwdField.setBounds(50, 50, 200, 30);
        JPanel pwdPanel = new JPanel(new FlowLayout());
        pwdPanel.setBackground(Color.WHITE);
        pwdField.setPreferredSize(new Dimension(200, 30));
        pwdPanel.add(pwdField);
        container.add(pwdPanel, BorderLayout.CENTER);

        // 3. 确认按钮
        JButton confirmBtn = new JButton("确认");
        confirmBtn.addActionListener(e -> {
            String pwd = new String(pwdField.getPassword());
            System.out.println("你输入的密码是: " + pwd);
            dispose(); // 关闭弹窗
        });
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(confirmBtn);
        container.add(btnPanel, BorderLayout.SOUTH);

        container.setBackground(Color.WHITE);
    }
}