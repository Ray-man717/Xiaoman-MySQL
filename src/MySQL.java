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
        CreateTable(c.con);
    }
    public static String getInput(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
        //自定义输入
    }
    public static void CreateTable(Connection con) {
        //创建表方法
        String sql = "CREATE TABLE EMS(" +
                "id INT PRIMARY KEY AUTO_INCREMENT" + "," +
                "ALI DOUBLE NOT NULL DEFAULT 0" + "," +
                "Temp DOUBLE NOT NULL DEFAULT 0" + "," +
                "Humidity DOUBLE NOT NULL DEFAULT 0" + "," +
                "Time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" + ")";

        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(sql);//执行动态SQL语句
            System.out.println("建表完成");
            stmt.close();//释放内存与资源
        }catch(SQLException e){
            System.out.println("建表失败，报错：");
            e.printStackTrace();
        }
    }
}
