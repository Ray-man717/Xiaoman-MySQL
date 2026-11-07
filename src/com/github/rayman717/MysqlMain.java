package com.github.rayman717;

import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.ConfigManager;
import com.github.rayman717.util.SqlUtil;

import java.util.*;

public class MysqlMain {
    private static final Scanner sc = new Scanner(System.in);//创建全局扫描器

    public static void main(String[] args) {
        SqlUtil c = new SqlUtil();
        ConfigManager.initConfig();
        DbConfig config = ConfigManager.getDbConfig();
        c.getConnection(config.ip,config.dbName,config.user,config.password);//连接SQL
        //CreateTable(c.con);      //建表
    }
}
