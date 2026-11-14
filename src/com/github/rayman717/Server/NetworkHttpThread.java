package com.github.rayman717.Server;


import com.github.rayman717.entity.DbConfig;
import com.sun.net.httpserver.HttpServer;
import com.github.rayman717.util.LogUtil;
import java.io.IOException;
import java.net.InetSocketAddress;


public class NetworkHttpThread implements Runnable {
    private static final String IP = "127.0.0.1";
    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000),0);
        }catch (IOException e){
            LogUtil.error("创建连接服务器失败："+e.getMessage());
            e.printStackTrace();
        }

    }

}
