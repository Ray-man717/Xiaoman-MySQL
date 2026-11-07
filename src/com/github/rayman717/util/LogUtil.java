package com.github.rayman717.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtil {
    private LogUtil(){
    }//构造方法私有化
    //文件夹:Logs,文件:app.log
    private static final String LOG_FOLDER = "logs";
    private static final String LOG_FILE = LOG_FOLDER + "/app.log";

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
        File file = new File(LOG_FILE);
        if(!file.exists()){
            try{
                if(file.createNewFile()) System.out.println(LOG_FILE+" created");
            }catch (IOException e){
                System.err.println("创建失败" + e.getMessage());
                return;//创建失败，我写nm
            }
        }

        //写入文件
        try (BufferedWriter fw = new BufferedWriter(new FileWriter(LOG_FILE,true))){
            fw.write(logLine);
            fw.newLine();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static void info(String msg){
        writeLog(msg,"INFO");
    }
    public static void error(String msg){
        writeLog(msg,"ERROR");
    }

}
