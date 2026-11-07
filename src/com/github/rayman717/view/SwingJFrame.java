package com.github.rayman717.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SwingJFream extends JFrame {
    public SwingJFream(String title) {
        setTitle(title);
        Container container = getContentPane();
        setBounds(200, 200, 400, 400);

        JLabel label = new JLabel("欢迎使用 EMS");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        container.add(label);

        JButton logIn = new JButton("登录");
        logIn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MyDialog text = new MyDialog(SwingJFream.this,"登录");
                text.setVisible(true);
            }
        });
        container.add(logIn);
        container.setBackground(Color.WHITE);

        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


    }

}
class MyDialog extends JDialog{
    public MyDialog(SwingJFream frame, String title) {
        super(frame,title,true);
        Container container = getContentPane();
        container.add(new JLabel("登录"));
        setBounds(300,300,400,400);
    }
}