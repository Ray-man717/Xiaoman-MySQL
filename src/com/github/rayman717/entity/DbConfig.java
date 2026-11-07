package com.github.rayman717.entity;

/**
 * 数据库连接配置实体类。
 * <p>
 * 用于承载从 config.properties 中读取出来的数据库连接参数，
 * 由 ConfigManager.getDbConfig() 组装，再交给 SqlUtil.getConnection(...) 建立连接。
 * 字段为 public 是该项目简化写法，方便调用方直接取值。
 */
public class DbConfig {
    public  String ip;        //数据库服务器 IP，如 127.0.0.1
    public  String port ;     //数据库端口，MySQL 默认 3306（注意：SqlUtil 中端口写死为 3306，此字段暂未参与拼接）
    public  String dbName;    //要连接的数据库名，如 EMS
    public  String user;      //登录数据库的用户名，如 root
    public  String password;  //登录数据库的密码

    /**
     * 全参构造方法：创建一份完整的数据库连接配置。
     *
     * @param ip       数据库服务器 IP
     * @param port     数据库端口
     * @param dbName   数据库名
     * @param user     用户名
     * @param password 密码
     */
    public DbConfig(String ip, String port, String dbName, String user, String password) {
        this.ip = ip;              //保存 IP
        this.port = port;          //保存端口
        this.dbName = dbName;      //保存数据库名
        this.user = user;          //保存用户名
        this.password = password;  //保存密码
    }
}
