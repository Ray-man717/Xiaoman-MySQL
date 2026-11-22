package com.github.rayman717.Server;

import com.github.rayman717.entity.DbConfig;
import com.github.rayman717.util.ConfigManager;
import com.github.rayman717.util.LogUtil;

import java.util.Scanner;

/** HTTP 网关的启动入口，负责拉起监听线程。 */
public class NetworkMain {
    private static DbConfig config;
    /** 启动网关线程（Main 中调用）。 */
    public static void startServer(DbConfig dbConfig) {
        config = dbConfig; // 存起来备用
        // 启动时，把 config 塞进 NetworkHttpThread
        new Thread(new NetworkHttpThread(config)).start();
        LogUtil.serverInfo("HTTP网关线程已启动", "NetworkMain");
    }
    /** 独立启动：起线程后监听 stop/exit 命令退出。 */
    public static void main(String[] args) {
        ConfigManager.initConfig();
        config = ConfigManager.getDbConfig();
        startServer(config);

        Scanner sc = new Scanner(System.in);
        System.out.print("exit，stop退出:");
        while (true) {
            String command = sc.nextLine();
            if (command.equals("stop") ||  command.equals("exit")) {
                LogUtil.serverInfo("退出",NetworkHttpThread.class.getSimpleName());
                System.exit(0);
            }
        }
    }
}
