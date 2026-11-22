package com.github.rayman717;

import com.github.rayman717.Server.NetworkMain;
import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.ConfigManager;
import com.github.rayman717.util.LogUtil;
import com.github.rayman717.util.SqlUtil;
import com.github.rayman717.view.JavaFX;
import javafx.application.Application;
//import com.github.rayman717.view.SwingJFrame;

import java.sql.Connection;
import java.util.Scanner;

/** 程序入口：初始化配置 → 连数据库 → 启网关 → 开界面。 */
public class Main {
    private static final Scanner sc = new Scanner(System.in);//创建全局扫描器

    /** 主方法。 */
    public static void main(String[] args) {
        LogUtil.info("————————程序开始————————","Main");
        SqlUtil c = new SqlUtil();                              //数据库工具
        ConfigManager.initConfig();                           //加载或创建配置
        DbConfig config = ConfigManager.getDbConfig();          //取出连接配置
        //c.Info(config);
        Connection con = SqlUtil.getConnection(config);//连接SQL
        NetworkMain network = new NetworkMain();
        network.startServer(config);                           //启动 HTTP 网关
        //createTable(c.con);      //建表
        //SwingJFrame fw = new SwingJFrame("HMS");
        Application.launch(JavaFX.class, args);                 //启动 JavaFX 界面
        LogUtil.info("——————程序结束——————","Main");
    }
}
