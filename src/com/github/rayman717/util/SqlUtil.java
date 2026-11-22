package com.github.rayman717.util;

import com.github.rayman717.entity.ActivityLog;
import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.enums.ActivityState;

import java.sql.*;

/** 数据库工具：加载驱动、连库、建表。 */
public class SqlUtil {
    public DbConfig config;
    @Deprecated
    public void Info(DbConfig config){
        this.config = config;
    }

    /** 加载 MySQL 驱动并建立连接，失败返回 null。 */
    public static Connection getConnection(DbConfig config) {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://" + config.ip + ":3306/" + config.dbName + "?useSSL=false&characterEncoding=utf8";

            conn = DriverManager.getConnection(url, config.user, config.password);

            LogUtil.serverInfo("数据库连接成功", "SqlUtil");

        } catch (ClassNotFoundException e) {
            LogUtil.serverError("找不到MySQL驱动，请检查是否导入了jar包", "SqlUtil");
            e.printStackTrace();
        } catch (SQLException e) {
            LogUtil.serverError("数据库连接失败: " + e.getMessage(), "SqlUtil_");
            e.printStackTrace();
        }

        // 返回连接（如果是失败，这里会是 null，调用它的地方要记得判空）
        return conn;
    }

    public static void saveActivityLog(DbConfig config, ActivityLog log) {
        String sqlActivity_log = "INSERT INTO activity_log (device_id,person,active,idle_minutes,temp,humi,lux,state) VALUES (?,?,?,?,?,?,?,?);";
        //String sqlEvents = "INSERT INTO events (device_id, type, level,detail,handled,handled_at) VALUES (?,?,?,?,?,?);";
        try(Connection conn = SqlUtil.getConnection(config)){
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement(sqlActivity_log)) {
                    ps.setString(1, log.device_id);
                    ps.setInt(2, log.person);
                    ps.setInt(3, log.active);
                    ps.setInt(4, log.idle_minutes);
                    ps.setDouble(5, log.temp);
                    ps.setDouble(6, log.humi);
                    ps.setInt(7, log.lux);
                    ActivityState stateEnums = ActivityState.fromString(log.state);
                    ps.setString(8, stateEnums.getDbValue());

                    int rows = ps.executeUpdate();
                    LogUtil.serverInfo("数据上传成功", "SqlUtil_SaveActivityLog");


                } catch (SQLException e) {
                    LogUtil.serverInfo("上传失败：" + e.getMessage(), "SqlUtil_SaveActivityLog");
                }
            }
        }catch (SQLException e){
            LogUtil.serverError("SQL连接错误：", "SqlUtil_SaveActivityLog");
        }
    }

    /** 建表：ems 表（当前未执行）和 user 表。 */
    public static void createTable(Connection con) {
        //创建表方法
        //ems 表：采集数据（id/光照/温度/湿度/时间），注意下面只执行了 user 表
        String ems = "CREATE TABLE ems" + "(" +
                "id INT PRIMARY KEY AUTO_INCREMENT" + "," +   //自增主键
                "ALI DOUBLE NOT NULL DEFAULT 0" + "," +       //光照强度，默认 0
                "Temp DOUBLE NOT NULL DEFAULT 0" + "," +      //温度，默认 0
                "Humidity DOUBLE NOT NULL DEFAULT 0" + "," +//湿度，默认 0
                "Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" + ")";   //记录时间
        //user 表：账号密码
        String user = "CREATE TABLE user" + "(" +
                "id INT PRIMARY KEY AUTO_INCREMENT" + "," +
                "User CHAR(255) NOT NULL DEFAULT 0 " + "," +
                "Password CHAR(255) NOT NULL " + ")" ;
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(user);//执行动态SQL语句
            LogUtil.info("建表完成",SqlUtil.class.getSimpleName()+"_CreateTable");
            stmt.close();//释放内存与资源
        }catch(SQLException e){
            LogUtil.error("建表失败:"+e.getMessage(),SqlUtil.class.getSimpleName()+"_CreateTable");
            e.printStackTrace();
        }
    }

}
