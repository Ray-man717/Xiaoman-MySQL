package com.github.rayman717.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 简易日志工具：info/error 两级，同时输出到控制台和日志文件。 */
public class LogUtil {
    private LogUtil(){
    }//构造方法私有化（工具类惯例，防止外部 new 出对象）
    //文件夹:Logs,文件:app.log
    private static final String LOG_FOLDER = "logs";   //日志目录
    private static final String LOG_FILE = LOG_FOLDER + "/app.log";   //应用日志
    private static final String SERVER_LOG_FILE = LOG_FOLDER + "/server.log"; // 服务器的日志

    /** 拼日志行（带模块名）→ 打印控制台 → 追加写入 logFile。 */
    private static void writeLog(String message, String level, String logFile,String moduleName) {
        //时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = sdf.format(new Date());
        //Log格式
        String logLine = "["+date+"]"+"["+level+"]"+"["+ moduleName +"]"+": "+message;

        System.out.println(logLine);//控制台输出Log

        //检查文件夹，没有就创建
        File folder = new File(LOG_FOLDER);
        if(!folder.exists()){
            if(folder.mkdirs()){
                System.out.println(LOG_FOLDER+" created");
            }
        }
        //检查日志文件，没有就创建
        File file = new File(logFile);
        if(!file.exists()){
            try{
                if(file.createNewFile()) System.out.println(logFile+" created");
            }catch (IOException e){
                System.err.println("创建失败" + e.getMessage());
                return;//创建失败，我写nm
            }
        }

        //追加写入（true 为追加模式，try-with-resources 自动关流）
        try (BufferedWriter fw = new BufferedWriter(new FileWriter(logFile,true))){
            fw.write(logLine);
            fw.newLine();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    /** INFO 日志。 */
    public static void info(String msg,String moduleName){writeLog(msg,"INFO",LOG_FILE,moduleName);}
    /** ERROR 日志。 */
    public static void error(String msg,String moduleName){
        writeLog(msg,"ERROR",LOG_FILE,moduleName);
    }

    /** 服务器日志 INFO。 */
    public static void serverInfo(String msg,String moduleName){writeLog(msg,"INFO",SERVER_LOG_FILE,moduleName);}
    /** 服务器日志 ERROR。 */
    public static void serverError(String msg,String moduleName){writeLog(msg,"ERROR",SERVER_LOG_FILE,moduleName);}

}

