package orm;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SqlSession的辅助类
 */
public class SqlSessionHandler {

    /**
     * 解析SQL语句,将#{}形式替换为?
     * 再将一条带?形式的SQL语句和参数集合包装成一个SQLAndKey对象,将其返回给调用处
     * @param sql 需要解析的SQL语句
     * @return 返回一个包装了 带?形式的SQL语句和参数集合的 对象
     */
    public SQLAndKey parseSQL(String sql) {

        //解析之前有两个变量,存储解析SQL后最终的两个部分
        StringBuilder newSql = new StringBuilder();
        List<String> keyList = new ArrayList<>();

        while (sql.length() != 0) {
            //按照规定的索引位置寻找下标位置
            //比如insert into table values(#{uno},#{uname})
            int left = sql.indexOf("#{");
            int right = sql.indexOf("}");
            if (left != -1 && right != -1 && left < right) {
                //截取前面的部分并拼接到newSql
                newSql.append(sql, 0, left);
                //拼接问号
                newSql.append("?");
                //将#{}中间的参数添加到集合
                keyList.add(sql.substring(left + 2, right));
            } else {
                //说明left为-1,没有更多的部分了,将最后的)拼接上去
                newSql.append(sql);
                //跳出循环
                break;
            }
            sql = sql.substring(right + 1);
        }
        //将上面解析过的两个变量(sql语句和参数集合)组合成一个对象
        return new SQLAndKey(newSql, keyList);
    }

    /**
     * 这个方法负责将SQL和问号组装完整
     * 参数包装在SQLAndKey里面,我们需要将SQLAndKey类里的keyList取出来并赋到sql语句?处
     * 利用反射机制,将参数obj拿出来
     * @param pstmt  pstmt已经拿到了一条带有问号的sql,它负责将新的sql和obj对象中的值组合在一起
     * @param obj 用户传来的,需要放置在sql语句?处的参数
     * @param keyList sql语句的参数集合
     */
    public void handleParameter(PreparedStatement pstmt, Object obj, List<String> keyList) throws SQLException, NoSuchFieldException, IllegalAccessException {
        //获取obj对应的Class
        Class<?> clazz = obj.getClass();

        //case 1:obj为单个基本数据类型,说明只有一个参数需要组合
        if (clazz == Integer.class) {
            pstmt.setInt(1, (Integer) obj);
        } else if (clazz == Float.class) {
            pstmt.setFloat(1, (Float) obj);
        } else if (clazz == String.class) {
            pstmt.setString(1, ((String) obj));
        } else {
            if (obj instanceof Map) { //case 2:obj为map集合
                //找小弟处理
                setMap(pstmt, obj, keyList);
            } else { //case 3:obj为domain类型
                //找小弟处理
                setDomain(pstmt,obj,keyList);
            }
        }
    }

    /**
     * 负责SQL和map集合的拼接
     */
    private void setMap(PreparedStatement pstmt, Object obj, List<String> keyList) throws SQLException {
        //将obj强转为map类型
        Map map = (Map) obj;
        for (int i = 0; i < keyList.size(); i++) {
            pstmt.setObject(i + 1, map.get(keyList.get(i)));
        }
    }

    /**
     * 负责SQl和domain对象的拼接
     */
    private void setDomain(PreparedStatement pstmtm, Object obj, List<String> keyList) throws NoSuchFieldException, IllegalAccessException, SQLException {
        //获取obj对应的Class
        Class<?> clazz = obj.getClass();
        for (int i = 0; i < keyList.size(); i++) {
            //先找到key
            String key = keyList.get(i);
            //通过key反射找到obj对象中对应的属性,取值
            Field field = clazz.getDeclaredField(key);
            //设置私有属性的值
            field.setAccessible(true);
            //找到私有属性对应的那个get方法,从obj对象内取得属性的值
            Object value = field.get(obj);
            pstmtm.setObject(i + 1, value);
        }
    }


    /**
     * 该方法负责将结果集的值组合成一个对象
     * @param rs 结果集
     * @param resultType 用户需要的返回结果的类型
     * @return 封装好的对象
     */
    public Object handleResult(ResultSet rs,Class<?> resultType) throws SQLException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        if (resultType ==  Integer.class) {
            return rs.getInt(1);
        } else if (resultType == Float.class) {
            return rs.getFloat(1);
        } else if (resultType == Double.class) {
            return rs.getDouble(1);
        } else if (resultType == String.class) {
            return rs.getString(1);
        } else {
            if (resultType == Map.class || resultType == HashMap.class) {
                return this.getMap(rs);
            } else {
                return this.getObject(rs, resultType);
            }
        }
    }

    /**
     * 将结果集的信息组成一个map集合
     */
    private Map<String,Object> getMap(ResultSet rs) throws SQLException {
        //创建map
        Map<String, Object> map = new HashMap<>();

        //获取结果集中的全部信息
        ResultSetMetaData metaData = rs.getMetaData();
        //遍历结果集中的全部列
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            //获取每一个列名
            String columnName = metaData.getColumnName(i);
            //根据每一个列名获取值
            Object value = rs.getObject(columnName);
            //存入map中
            map.put(columnName, value);
        }
        return map;
    }

    /**
     * 将结果集的信息组合成一个domain
     */
    private Object getObject(ResultSet rs, Class<?> resultType) throws SQLException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        //通过反射创建对象
        Object obj = resultType.newInstance();

        //获取结果集中的全部信息
        ResultSetMetaData metaData = rs.getMetaData();
        //遍历结果集中的全部列
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            //拿到结果集的每一个列名
            String columnName = metaData.getColumnName(i);
            //反射找到列名对应的那个属性
            Field field = resultType.getDeclaredField(columnName);
            //操作私有属性
            field.setAccessible(true);
            //给obj对象的属性赋值
            field.set(obj, rs.getObject(columnName));
        }
        return obj;
    }

}
