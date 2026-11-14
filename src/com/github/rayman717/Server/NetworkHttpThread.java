package com.github.rayman717.Server;

import com.sun.net.httpserver.HttpServer;
import com.github.rayman717.util.LogUtil;
import java.io.IOException;
import java.net.InetSocketAddress;


public class NetworkHttpThread implements Runnable {
    private static final String IP = "0.0.0.0";
    private static final String PORT = "8080";

    @Override
    public void run() {
        HttpServer server = null;

        try {
            // 1. 建立服务器，监听 8080 端口
            server = HttpServer.create(new InetSocketAddress(IP,Integer.parseInt(PORT)), 0);

            // 2. 配置接收 K10 数据的接口
            server.createContext("/api/upload", exchange -> {
                // 直接调用封装好的回信方法（只有一行，不再乱套了！）
                reply(exchange, "{\"status\"" +
                        ":\"ok\"}");
                System.out.println("✅ 收到 K10 请求，已回复 ok！");
                LogUtil.info("已接受请求");
            });

            // 3. 启动服务器！（只有这一句启动）
            server.start();
            System.out.println("服务器已经成功启动，监听 8080 端口！");
            LogUtil.info("服务器启动成功，端口8080");

        } catch (IOException e) {
            System.out.println("启动失败，可能是 8080 端口被其他程序占用了！");
            LogUtil.error("启动失败:"+e.getMessage());
            e.printStackTrace();
        }
    }

    // 🌟 把重复的脏活累活单独拎出来，放在 run() 方法的外面
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
            LogUtil.error("线程错误:"+e.getMessage());
            e.printStackTrace();
        }
    }
}
