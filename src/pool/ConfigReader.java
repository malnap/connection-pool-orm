package pool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 这个类的目的是为了在启动程序时候
 * 一次性读取properties配置文件的信息
 * 这些信息存在一个map缓存中
 * 为了创建连接和连接池管理而使用
 */
public class ConfigReader {

    private static Properties pro;
    private static Map<String, String> configMap;

    /**
     * 在内存中只需要一份
     */
    static {
        pro = new Properties();
        configMap = new HashMap<>();
        InputStream is = null;
        BufferedReader br = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
            br = new BufferedReader(new InputStreamReader(is));
            pro.load(br);

            Enumeration<?> en = pro.propertyNames();
            while (en.hasMoreElements()) {
                //读键
                String key = (String)en.nextElement();
                //读值
                String value = pro.getProperty(key);
                //存入map映射集合
                configMap.put(key,value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 给使用者提供一个获取value的方法
     * @param key 键
     * @return 值
     */
    public static String getPropertyValue(String key) {
        return configMap.get(key);
    }
}
