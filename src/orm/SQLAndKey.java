package orm;

import java.util.List;

/**
 * 这个类的目的是为了存储被解析后SQL和SQL上的参数信息
 * 比如insert into table values(#{uno},#{uname}):
 * sql:<code> insert into table values(?,?) </code>
 * keyList:uno,uname
 */
public class SQLAndKey {

    /** 将特殊结构的SQL还原回问号结构 */
    private final StringBuilder sql;
    /** 特殊结构的SQL中的参数信息 */
    private final List<String> keyList;

    public SQLAndKey(StringBuilder sql, List<String> keyList) {
        this.sql = sql;
        this.keyList = keyList;
    }

    public String getSql() {
        return sql.toString();
    }

    public List<String> getKeyList() {
        return keyList;
    }
}
