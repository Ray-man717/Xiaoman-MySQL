package com.github.rayman717.entity;

public class DbConfig {
    public  String ip;
    public  String port ;
    public  String dbName;
    public  String user;
    public  String password;

    public DbConfig(String ip, String port, String dbName, String user, String password) {
        this.ip = ip;
        this.port = port;
        this.dbName = dbName;
        this.user = user;
        this.password = password;
    }
}
