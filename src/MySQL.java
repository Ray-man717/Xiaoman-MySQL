import java.sql.*;
import java.util.Scanner;

public class MySQL {
    private static final Scanner sc = new Scanner(System.in);//创建全局扫描器
    Connection con;
    public Connection getConnection(String ip, String name,String user, String pass) {
        String url = "jdbc:mysql://"+ ip +":3306/"+ name + "?useSSL=false&characterEncoding=utf8";
        try{
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("数据库驱动加载成功");
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        try{
            con = DriverManager.getConnection(url,user,pass);
            System.out.println("数据库连接完成");
        }catch(SQLException e){
            e.printStackTrace();
        }
        return con;
    }
    public static void main(String[] args) {
        MySQL c = new MySQL();
        String ip = getInput("IP:");
        String name = getInput("Name:");
        String user = getInput("User:");
        String password = getInput("Password:");
        c.getConnection(ip,name,user, password);//连接SQL
    }
    public static String getInput(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
        //自定义输入
    }
}
