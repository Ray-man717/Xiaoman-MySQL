package com.github.rayman717.util;

import com.github.rayman717.entity.DbConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import static com.github.rayman717.util.InputUtil.input;

/** 配置管理：读写 config.properties，缺文件时交互式创建。 */
public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";   //配置文件名
    private static Properties prop = new Properties();               //配置内容

    /** 初始化配置：无文件则询问输入并落盘，有则加载。 */
    public static void initConfig() {
        Scanner sc = new Scanner(System.in);
        File file = new File(CONFIG_FILE);
        if(!file.exists()){
            //没有配置文件：逐项询问，回车用默认值
            System.out.println("未找到config");
            System.out.println("输入信息");
            String ip = input(sc,"MySQL IP(默认值:127.0.0.1):");
            if(ip.trim().isEmpty()) ip = "127.0.0.1";
            String port = input(sc,"MySQL Port(3306):");
            if(port.trim().isEmpty()) port = "3306";
            String user = input(sc,"MySQL User(root):");
            if(user.trim().isEmpty()) user = "root";
            String password = input(sc,"MySQL Password:");
            String dbName = input(sc,"MySQL dbName:");

            //写入 Properties，键名与读取时一致
            prop.setProperty("db.ip",ip);
            prop.setProperty("db.port",port);
            prop.setProperty("db.user",user);
            prop.setProperty("db.password", password);
            prop.setProperty("db.name",dbName);
            try (FileWriter fw = new FileWriter(CONFIG_FILE)) {
                prop.store(fw,"Database Configuration");   //落盘，第二参数是文件头注释
                LogUtil.info("Config is ready 已创建",ConfigManager.class.getSimpleName());
            }catch (IOException e){
                LogUtil.error("Config error 创建失败"+e.getMessage(),ConfigManager.class.getSimpleName());
            }
        }else {
            try (FileReader fis = new FileReader(CONFIG_FILE)) {
                prop.load(fis);   //读取已有配置
                LogUtil.info("Config is pass 读取成功",ConfigManager.class.getSimpleName());
            }catch (IOException e){
                LogUtil.error("Config error 读取失败"+e.getMessage(),ConfigManager.class.getSimpleName());
            }
        }

    }
    /** 取出配置组装成 DbConfig，缺项用默认值。 */
    public static DbConfig getDbConfig(){
        String ip = prop.getProperty("db.ip","127.0.0.1");
        String port = prop.getProperty("db.port","3306");
        String user = prop.getProperty("db.user","root");
        String password = prop.getProperty("db.password","");
        String dbName = prop.getProperty("db.name","");

        return new DbConfig(ip,port,dbName,user,password);   //参数顺序：ip, port, dbName, user, password
    }
}
