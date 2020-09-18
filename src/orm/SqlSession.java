package orm;

import orm.annotation.*;
import pool.ConnectionPool;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 该类负责读写数据库,类中全都是纯粹的JDBC操作和SQL语句
 */
public class SqlSession {

    private final SqlSessionHandler handler = new SqlSessionHandler();

    /**
     * 增删改操作
     * @param sql SQL语句
     * @param obj SQL语句上面的参数
     */
    public void update(String sql, Object obj) {
        try {
            //需要将用户提供给我们的特殊结构的SQL先做一个解析
            //1.解析SQL语句后最终会解析成两部分,SQL语句和keyList
            SQLAndKey sqlAndKey = handler.parseSQL(sql);

            //2.获取连接
            ConnectionPool pool = ConnectionPool.getInstance();
            Connection conn = pool.getConnection();

            //3.预处理集
            PreparedStatement pstmt = conn.prepareStatement(sqlAndKey.getSql());

            //如果参数为空,则无需处理参数
            if (obj != null) {
                //4.利用反射将SQL与参数组合起来
                handler.handleParameter(pstmt,obj,sqlAndKey.getKeyList());
            }

            //5.执行操作
            pstmt.executeUpdate();

            //6.关闭,conn只是释放
            pstmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 新增操作
     * @param sql SQL语句
     * @param obj SQL语句上的参数
     */
    public void insert(String sql, Object obj) {
        this.update(sql, obj);
    }

    /**
     * 删除操作
     * @param sql SQL语句
     * @param obj SQL语句上的参数
     */
    public void delete(String sql, Object obj) {
        this.update(sql, obj);
    }

    /**
     * 查询单条记录
     * 单条记录的返回值为什么不是ResultSet,因为rs对象在返回对象之前就需要关闭rs.close()
     * @param sql SQL语句
     * @param obj SQL语句上面的参数
     * @param resultType 查询以后的返回结果
     */
    @SuppressWarnings("unchecked")
    public <T> T selectOne(String sql,Object obj,Class<?> resultType) {
        return (T)this.selectList(sql, obj, resultType).get(0);
    }

    /**
     * 查询多条记录
     * @param sql SQL语句
     * @param obj SQL语句上面的参数
     * @param resultType 查询以后的返回结果
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> selectList(String sql, Object obj, Class<?> resultType) {
        List<T> list = new ArrayList<>();
        try {
            //1.解析SQL语句
            SQLAndKey sqlAndKey = handler.parseSQL(sql);

            //2.获取连接
            ConnectionPool pool = ConnectionPool.getInstance();
            Connection conn = pool.getConnection();

            //3.状态参数
            PreparedStatement pstmt = conn.prepareStatement(sqlAndKey.getSql());

            //4.把SQL和问号拼在一起
            if (obj != null) {
                handler.handleParameter(pstmt,obj,sqlAndKey.getKeyList());
            }

            //5.执行操作
            ResultSet rs = pstmt.executeQuery();

            //6.处理结果
            while (rs.next()) {
                list.add((T)handler.handleResult(rs, resultType));
            }

            //7.关闭
            rs.close();
            pstmt.close();
            //实质上只是释放连接
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 让SqlSession帮DAO创建一个小弟(代理对象)
     *
     * 前提:如果想要使用动态代理对象帮忙做事,被代理的DAO必须是个接口
     *
     * @param clazz 指明是具体的哪个DAO
     * @param <T> 指明是个泛型方法,方法里可能会用到该泛型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class clazz) {
        // 创建一个代理对象需要三个条件
        // 1.类加载器,将存在硬盘上的文件加载到内存
        ClassLoader classLoader = clazz.getClassLoader();

        // 2.Class[]加载的类
        Class[] interfaces = new Class[]{clazz};

        // 3.具体实现接口InvocationHandle,指明该怎么做事
        return (T) Proxy.newProxyInstance(classLoader, interfaces, new InvocationHandler() {

            /**
             * invoke方法是代理对象具体做事的方式,帮助原来的DAO做的事情,调用自己的增删改查方法
             *
             * 分析:
             * DAO原来是调用SqlSession的某个方法,现在代理也是调用SqlSession中的某个方法,
             * 而需要调用哪个方法却取决于注解名,因为注解名才决定了那个方法真正该做什么,与方法名是什么无关
             *
             * @param proxy 代理对象
             * @param method 被代理的方法
             * @param args 被代理方法的参数
             * @return 返回一个实现了InvocationHandle接口的具体对象,即告知了具体该怎么做事
             */
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //1.获取方法上面的注解
                Annotation annotation = method.getAnnotations()[0];

                //2.获取方法上面的注解的类型,从而可以确定该调用哪个方法
                Class annotationType = annotation.annotationType();

                //3.解析注解得到SQl,因为调用上面的方法需要SQl语句
                //3.1 找寻当前type注解类型中的那个value方法
                Method valueMethod = annotationType.getDeclaredMethod("value");
                //3.2 执行这个value方法获取里面搬运过来的SQL
                String sql = (String) valueMethod.invoke(annotation);

                //4.提供SQL上面的参数,因为调用DAO方法需要方法名,SQL,参数
                //分析:值只有几种情况 1.基本类型int float String 2.map 3.domain 4.没有
                Object param = (args == null) ? null : args[0];

                //4.1 根据annotationType判断该调用上述的哪个方法
                if (annotationType == Insert.class) {
                    SqlSession.this.insert(sql, param);
                } else if (annotationType == Delete.class) {
                    SqlSession.this.delete(sql, param);
                } else if (annotationType == Update.class) {
                    SqlSession.this.update(sql, param);
                } else if (annotationType == Select.class) {
                    /*
                     根据注解名字是无法确定该调用哪个select方法
                     可以根据method反射,根据返回值来判断,domain List<domain>
                    */
                    Class<?> returnType = method.getReturnType();
                    if (returnType == List.class) {

                        /*
                         说明是多条查询,解析returnType里面的那个泛型
                         返回值的具体类型(java.util.List<domain.Atm>)
                        */
                        Type genericReturnType = method.getGenericReturnType();

                        /*
                         上述方法的返回值类型正常应该是个Class,由于这个Class没有办法操作泛型
                         Type是一个接口,Class子类实现了它,因此需要将type还原成可以操作泛型的那个类型
                        */
                        ParameterizedTypeImpl realReturnType = (ParameterizedTypeImpl) genericReturnType;

                        /* 获取返回值List<domain>中具体的泛型类中的第一个元素 */
                        Type patternType = realReturnType.getActualTypeArguments()[0];

                        /* 还原成需要的Class */
                        Class resultType = (Class) patternType;

                        return SqlSession.this.selectList(sql, param, resultType);
                    } else {
                        //说明是单条查询
                        return SqlSession.this.selectOne(sql, param, returnType);
                    }
                } else {
                    throw new RuntimeException("can't find the annotation");
                }
                return null;
            }
        });


    }
}
