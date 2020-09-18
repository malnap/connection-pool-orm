package pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 自定义一个连接类
 * 包含 一个真实的Connection连接 和 一个标记是否使用的状态
 */
public class MyConnection extends AdapterConnection {

    private static String url;
    private static String user;
    private static String password;

    //考虑conn的创建过程应放在哪里写
    private Connection conn;
    //默认为false,最好写上
    private boolean used = false;

    /** 加载驱动类,在内存中只需要一份 */
    static {
        try {
            String className = ConfigReader.getPropertyValue("className");
            Class.forName(className);

            url = ConfigReader.getPropertyValue("url");
            user = ConfigReader.getPropertyValue("user");
            password = ConfigReader.getPropertyValue("password");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** 连接应该在创建对象之前就准备好了,不应该只有一份,很多人来需要很多个连接 */
    {
        try {
            conn = DriverManager.getConnection(url,user,password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return this.conn.prepareStatement(sql);
    }

    @Override
    public void close() {
        this.setUsed(false);
    }
}
