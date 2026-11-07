package com.github.rayman717.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlUtil {
    Connection con;
    public Connection getConnection(String ip, String name,String user, String pass) {
        String url = "JDBC:mysql://"+ ip +":3306/"+ name + "?useSSL=false&characterEncoding=utf8";
        try{
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("数据库驱动加载成功");
            LogUtil.info("数据库驱动加载成功");
        }catch(ClassNotFoundException e){
            LogUtil.error("驱动加载失败"+e.getMessage());
            e.printStackTrace();
        }
        try{
            con = DriverManager.getConnection(url,user,pass);
            System.out.println("数据库连接完成");
            LogUtil.info("数据库路径成功");
        }catch(SQLException e){
            LogUtil.error("连接失败"+e.getMessage());
            e.printStackTrace();
        }
        return con;
    }
    public static void CreateTable(Connection con) {
        //创建表方法
        String sql = "CREATE TABLE ems(" +
                "id INT PRIMARY KEY AUTO_INCREMENT" + "," +
                "ALI DOUBLE NOT NULL DEFAULT 0" + "," +
                "Temp DOUBLE NOT NULL DEFAULT 0" + "," +
                "Humidity DOUBLE NOT NULL DEFAULT 0" + "," +
                "Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" + ")";

        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(sql);//执行动态SQL语句
            System.out.println("建表完成");
            LogUtil.info("建表完成");
            stmt.close();//释放内存与资源
        }catch(SQLException e){
            System.out.println("建表失败，报错：");
            LogUtil.error("建表失败 "+e.getMessage());
            e.printStackTrace();
        }
    }
}
