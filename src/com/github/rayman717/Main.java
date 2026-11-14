package com.github.rayman717;

import com.github.rayman717.Server.NetworkMain;
import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.ConfigManager;
import com.github.rayman717.util.LogUtil;
import com.github.rayman717.util.SqlUtil;
import com.github.rayman717.view.SwingJFrame;

import java.util.Scanner;

/**
 * 程序入口类。
 * <p>
 * 启动流程：初始化配置 → 取出数据库配置 → 建立数据库连接。
 * （建表等后续操作可按需在此扩展）
 */
public class Main {
    private static final Scanner sc = new Scanner(System.in);//创建全局扫描器

    /**
     * 主方法：程序启动入口。
     *
     * @param args 命令行参数（本程序未使用）
     */
    public static void main(String[] args) {
        LogUtil.info("————————程序开始————————");
        SqlUtil c = new SqlUtil();                              //数据库操作工具实例
        ConfigManager.initConfig();                             //加载或创建 config.properties
        DbConfig config = ConfigManager.getDbConfig();          //取出数据库连接配置
        c.getConnection(config.ip, config.dbName, config.user, config.password);//连接SQL
        NetworkMain network = new NetworkMain();
        network.HttpNetworkConnect();
        //CreateTable(c.con);      //建表
        SwingJFrame fw = new SwingJFrame("HMS");
    }
}
