package com.github.rayman717.entity;

/** 数据库连接配置实体，装连接参数。 */
public class DbConfig {
    public String ip;        //服务器 IP
    public String port ;     //端口（SqlUtil 里写死 3306，暂未生效）
    public String dbName;    //数据库名
    public String user;      //用户名
    public String password;  //密码

    /** 全参构造：注入连接参数。 */
    public DbConfig(String ip, String port, String dbName, String user, String password) {
        this.ip = ip;
        this.port = port;
        this.dbName = dbName;
        this.user = user;
        this.password = password;
    }
}
