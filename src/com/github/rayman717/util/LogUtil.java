package com.github.rayman717.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 简易日志工具类。
 * <p>
 * 对外只提供 info / error 两个级别，日志同时输出到控制台和文件；
 * 文件位置为 logs/app.log，每行格式：[时间][级别]: 内容。
 * 全部方法都是静态的，不允许实例化。
 */
public class LogUtil {
    private LogUtil(){
    }//构造方法私有化（工具类惯例，防止外部 new 出对象）
    //文件夹:Logs,文件:app.log
    private static final String LOG_FOLDER = "logs";   //日志目录（相对程序运行目录）
    private static final String LOG_FILE = LOG_FOLDER + "/app.log";   //日志文件完整路径

    /**
     * 核心写日志方法：拼日志行 → 打印控制台 → 确保目录/文件存在 → 追加写入文件。
     *
     * @param message 日志正文
     * @param level   日志级别（INFO / ERROR）
     */
    private static void writeLog(String message, String level) {
        //时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = sdf.format(new Date());
        //Log格式
        String logLine = "["+date+"]"+"["+level+"]"+": "+message;

        System.out.println(logLine);//控制台输出Log

        //检查文件夹，没有就创建
        File folder = new File(LOG_FOLDER);
        if(!folder.exists()){
            if(folder.mkdirs()){
                System.out.println(LOG_FOLDER+" created");
            }
        }
        //检查日志文件，没有就创建
        File file = new File(LOG_FILE);
        if(!file.exists()){
            try{
                if(file.createNewFile()) System.out.println(LOG_FILE+" created");
            }catch (IOException e){
                System.err.println("创建失败" + e.getMessage());
                return;//创建失败，我写nm
            }
        }

        //写入文件：FileWriter 第二个参数 true 表示追加；try-with-resources 会自动关闭流
        try (BufferedWriter fw = new BufferedWriter(new FileWriter(LOG_FILE,true))){
            fw.write(logLine);
            fw.newLine();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    /**
     * 记录一条 INFO 级别日志（正常流程信息）。
     *
     * @param msg 日志正文
     */
    public static void info(String msg){
        writeLog(msg,"INFO");
    }
    /**
     * 记录一条 ERROR 级别日志（异常/失败信息）。
     *
     * @param msg 日志正文
     */
    public static void error(String msg){
        writeLog(msg,"ERROR");
    }

}
