package com.github.rayman717.util;

import com.github.rayman717.entity.DbConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

/**
 * 配置管理类。
 * <p>
 * 负责 config.properties 的创建、读取与解析：
 * 文件不存在时在控制台交互式询问连接参数并写盘；
 * 文件已存在时直接加载，最后由 getDbConfig() 包装成 DbConfig 返回。
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";   //配置文件名（相对程序运行目录）
    private static Properties prop = new Properties();               //配置内容容器，由 initConfig 填充

    /**
     * 初始化配置：没有配置文件就引导用户创建，有就直接读取。
     */
    public static void initConfig(){
        File file = new File(CONFIG_FILE);
        if(!file.exists()){
            //没有配置文件：逐项询问，直接回车则使用括号里的默认值
            System.out.println("未找到config");
            Scanner sc = new Scanner(System.in);
            System.out.println("输入信息");
            System.out.print("MySQL IP(默认值:127.0.0.1):");
            String ip = sc.nextLine();
            if(ip.trim().isEmpty()) ip = "127.0.0.1";
            System.out.print("MySQL Port(3306):");
            String port = sc.nextLine();
            if(port.trim().isEmpty()) port = "3306";
            System.out.print("MySQL User(root):");
            String user = sc.nextLine();
            if(user.trim().isEmpty()) user = "root";
            System.out.print("MySQL Password:");
            String password = sc.nextLine();
            System.out.print("MySQL dbName:");
            String dbName = sc.nextLine();

            //把用户输入写入 Properties，键名与读取时保持一致
            prop.setProperty("db.ip",ip);
            prop.setProperty("db.port",port);
            prop.setProperty("db.user",user);
            prop.setProperty("db.password", password);
            prop.setProperty("db.name",dbName);
            try (FileWriter fw = new FileWriter(CONFIG_FILE)) {
                prop.store(fw,"Database Configuration");   //落盘，第二参数是文件头注释
                LogUtil.info("Config is ready 已创建");
            }catch (IOException e){
                LogUtil.error("Config error 创建失败"+e.getMessage());
            }
        }else {
            try (FileReader fis = new FileReader(CONFIG_FILE)) {
                prop.load(fis);   //读取已有配置
                LogUtil.info("Config is pass 读取成功");
            }catch (IOException e){
                LogUtil.error("Config error 读取失败"+e.getMessage());
            }
        }

    }
    /**
     * 从已加载的配置中取出各项参数，组装成 DbConfig 实体。
     * <p>
     * 取不到时使用默认值：IP 127.0.0.1、端口 3306、用户 root、密码与库名为空。
     *
     * @return 数据库连接配置实体
     */
    public static DbConfig getDbConfig(){
        String ip = prop.getProperty("db.ip","127.0.0.1");
        String port = prop.getProperty("db.port","3306");
        String user = prop.getProperty("db.user","root");
        String password = prop.getProperty("db.password","");
        String dbName = prop.getProperty("db.name","");

        return new DbConfig(ip,port,dbName,user,password);   //注意参数顺序：ip, port, dbName, user, password
    }
}
