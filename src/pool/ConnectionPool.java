package pool;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPool {

    /**
     * 单例模式,volatile防止指令重排序
     */
    private volatile static ConnectionPool pool;

    private ConnectionPool(){}

    public static ConnectionPool getInstance(){
        if (pool == null) {
            //类锁 因为创建连接池 需要类模版
            //对象锁的话 本来就是没有对象创建对象 无意义
            synchronized (ConnectionPool.class) {
                if (pool == null) {
                    //可能出现指令重排 —— volatile关键字
                    pool = new ConnectionPool();
                }
            }
        }
        return pool;
    }

    /**
     * 最小连接个数
     */
    private final int minConnectCount = Integer.parseInt(ConfigReader.getPropertyValue("minConnectCount"));


    /**
     * 封装了多个连接的连接池
     */
    private final List<Connection> connectionPool = new ArrayList<>();

    /* 连接池应该在对象创建前就造好了,所以考虑用块,而不用构造方法或普通方法 */
    {
        for (int i = 1;i <= minConnectCount; i++) {
            connectionPool.add(new MyConnection());
        }
    }

    /**
     * 获取连接
     * 考虑返回值该是Conn还是MyConn
     * 从用户使用的角度来看,1和2都行
     * 用户需要操作状态,用户用完了以后,不能关闭,需要切换状态---释放
     * 这里锁里面和锁方法上,性能没有多少区别
     */
    private Connection getMyConnection() {
        Connection result = null;
        //从池子中拿一个没有被使用的连接出来
        for (Connection conn: connectionPool) {
            //强转,向下转型
            MyConnection myConn = (MyConnection) conn;
            if (!myConn.isUsed()) {
                //锁住当前连接对象,锁this也可以,整个连接池直接锁了,里面的连接都不能操作了
                synchronized (conn) {
                    //判断之后才锁,可能有多个人同时判断为未使用,所以需要再判断一次
                    if (!myConn.isUsed()) {
                        myConn.setUsed(true);
                        result = myConn;
                    }
                }
                break;
            }
        }
        return result;
    }

    private final int waitTime = Integer.parseInt(ConfigReader.getPropertyValue("waitTime"));

    /**
     * 当连接数不够又有多的人来使用时直接返回空,用户体验不好
     * 可以建立用户等待机制,等5s
     */
    public Connection getConnection() {

        //多态的效果,父类引用指向子类对象,如果出现同名方法调用子类重写后的,否则调用父类的方法
        Connection conn = this.getMyConnection();

        //尝试连接50次,每次间隔0.1s,用户等待机制等5s
        int count = 0;
        while (conn == null && count < waitTime * 10) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //每隔一会又来尝试获取一次连接
            conn = this.getMyConnection();
            count++;
        }

        //当循环条件不满足后,若连接仍为空,则抛出异常
        if (conn == null) {
            throw new SystemBusyException("系统繁忙,请稍后再试");
        }

        return conn;
    }
}
