package com.github.rayman717.util;

import com.github.rayman717.entity.DbConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static Properties prop = new Properties();
    public static void initConfig(){
        File file = new File(CONFIG_FILE);
        if(!file.exists()){
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

            prop.setProperty("db.ip",ip);
            prop.setProperty("db.port",port);
            prop.setProperty("db.user",user);
            prop.setProperty("db.password", password);
            prop.setProperty("db.name",dbName);
            try (FileWriter fw = new FileWriter(CONFIG_FILE)) {
                prop.store(fw,"Database Configuration");
                LogUtil.info("Config is ready 已创建");
            }catch (IOException e){
                LogUtil.error("Config error 创建失败"+e.getMessage());
            }
        }else {
            try (FileReader fis = new FileReader(CONFIG_FILE)) {
                prop.load(fis);
                LogUtil.info("Config is pass 读取成功");
            }catch (IOException e){
                LogUtil.error("Config error 读取失败"+e.getMessage());
            }
        }

    }
    public static DbConfig getDbConfig(){
        String ip = prop.getProperty("db.ip","127.0.0.1");
        String port = prop.getProperty("db.port","3306");
        String user = prop.getProperty("db.user","root");
        String password = prop.getProperty("db.password","");
        String dbName = prop.getProperty("db.name","");

        return new DbConfig(ip,port,dbName,user,password);
    }
}
