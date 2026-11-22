package com.github.rayman717.Server;

import com.github.rayman717.entity.ActivityLog;
import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.SqlUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.github.rayman717.util.LogUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;


/** 网关 HTTP 线程：监听 8080，提供 /api/upload 供 K10 上报。 */
public class NetworkHttpThread implements Runnable {
    private static final String IP = "0.0.0.0";   //监听所有网卡
    private static final String PORT = "8080";    //监听端口

    private final DbConfig config;
    public NetworkHttpThread(DbConfig config) {
        this.config = config;
    }
    @Override
    public void run() {
        HttpServer server = null;

        try {
            // 1. 建立服务器，监听 8080 端口
            server = HttpServer.create(new InetSocketAddress(IP,Integer.parseInt(PORT)), 0);

            // 2. 配置接收 K10 数据的接口
            server.createContext("/api/report", exchange -> {

                InputStream inputStream = exchange.getRequestBody();
                String json = new String(inputStream.readAllBytes(), "UTF-8");
                LogUtil.serverInfo("K10发出Json:"+json,NetworkHttpThread.class.getSimpleName());

                Gson gson = new Gson();
                ActivityLog log = gson.fromJson(json, ActivityLog.class);
                SqlUtil.saveActivityLog(config,log);

                // 直接调用封装好的回信方法
                reply(exchange, "{\"status\"" + ":\"ok\"}");
                System.out.println(" 收到 K10 请求，已回复 ok！");
                LogUtil.serverInfo("收到 K10 请求",NetworkHttpThread.class.getSimpleName());
            });

            // 3. 启动服务器！（只有这一句启动）
            server.start();
            System.out.println("服务器已经成功启动，监听 8080 端口！");
            LogUtil.serverInfo("网关服务器启动成功，端口8080",NetworkHttpThread.class.getSimpleName());

        } catch (IOException e) {
            System.out.println("启动失败，可能是 8080 端口被其他程序占用了！");
            LogUtil.serverError("网关启动失败:"+e.getMessage(),NetworkHttpThread.class.getSimpleName());
            e.printStackTrace();
        }
    }

    //把重复的脏活累活单独拎出来，放在 run() 方法的外面
    public static void reply(com.sun.net.httpserver.HttpExchange exchange, String message) {
        try {
            byte[] responseBytes = message.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);

            java.io.OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
            exchange.close();
        } catch (Exception e) {
            LogUtil.serverError("网关服务器线程错误:"+e.getMessage(),NetworkHttpThread.class.getSimpleName());
            e.printStackTrace();
        }
    }
}
