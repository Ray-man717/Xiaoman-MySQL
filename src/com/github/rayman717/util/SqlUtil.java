package com.github.rayman717.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库操作工具类。
 * <p>
 * 负责加载 MySQL 驱动、获取数据库连接，以及提供建表等基础操作。
 */
public class SqlUtil {
    public Connection con;   //当前持有的数据库连接对象（包可见，MysqlMain 中通过 c.con 访问）

    /**
     * 获取 MySQL 数据库连接。
     * <p>
     * 先反射加载 com.mysql.jdbc.Driver，再用 DriverManager 建立连接；
     * 成功或失败都会写日志，失败时返回 null。
     *
     * @param ip   数据库服务器 IP
     * @param name 数据库名
     * @param user 用户名
     * @param pass 密码
     * @return 建立好的 Connection，失败时为 null
     */
    public Connection getConnection(String ip, String name,String user, String pass) {
        //JDBC 连接串：关闭 SSL、使用 utf8 编码；端口固定 3306
        String url = "JDBC:mysql://"+ ip +":3306/"+ name + "?useSSL=false&characterEncoding=utf8";
        try{
            Class.forName("com.mysql.jdbc.Driver");   //加载 MySQL 驱动（5.x 驱动类名）
            System.out.println("数据库驱动加载成功");
            LogUtil.info("数据库驱动加载成功");
        }catch(ClassNotFoundException e){
            LogUtil.error("驱动加载失败"+e.getMessage());
            e.printStackTrace();
        }
        try{
            con = DriverManager.getConnection(url,user,pass);   //真正建立连接
            System.out.println("数据库连接完成");
            LogUtil.info("数据库路径成功");
        }catch(SQLException e){
            LogUtil.error("连接失败"+e.getMessage());
            e.printStackTrace();
        }
        return con;
    }
    /**
     * 创建 ems 表。
     * <p>
     * 表字段：id 自增主键、ALI（光照）、Temp（温度）、Humidity（湿度）、Time（记录时间）。
     * 注意：表已存在时执行会抛异常，需要在外部先删表或自行改成 IF NOT EXISTS。
     *
     * @param con 已建立好的数据库连接
     */
    public static void CreateTable(Connection con) {
        //创建表方法
        String ems = "CREATE TABLE ems" + "(" +
                "id INT PRIMARY KEY AUTO_INCREMENT" + "," +   //自增主键
                "ALI DOUBLE NOT NULL DEFAULT 0" + "," +       //光照强度，非空，默认 0
                "Temp DOUBLE NOT NULL DEFAULT 0" + "," +      //温度，非空，默认 0
                "Humidity DOUBLE NOT NULL DEFAULT 0" + "," +//湿度，非空，默认 0
                "Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" + ")";   //记录时间，默认取当前时间

        String user = "CREATE TABLE user" + "(" +
                "id INT PRIMARY KEY AUTO_INCREMENT" + "," +
                "User CHAR(255) NOT NULL DEFAULT 0 " + "," +
                "Password CHAR(255) NOT NULL " + ")" ;
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(user);//执行动态SQL语句
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
