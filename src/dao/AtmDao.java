package dao;

import domain.Atm;
import orm.SqlSession;
import orm.annotation.*;

import java.util.List;

public interface AtmDao {

    @Insert("insert into atm values(#{aname},#{apassword},#{abalance})")
    void insert(Atm atm);

    @Delete("delete from atm where aname=#{aname}")
    void delete(String aname);

    @Update("update atm set aname=#{aname},apassword=#{apassword},abalance=#{abalance} WHERE aname=#{aname}")
    void update(Atm atm);

    @Select("select * from atm where aname=#{aname}")
    Atm selectOne(String aname);

    @Select("select * from atm")
    List<Atm> selectList();



//    private final SqlSession sqlSession = new SqlSession();
//
//    public void delete(String aname) {
//        String sql = "delete from atm where aname=#{aname}";
//        sqlSession.delete(sql,aname);
//    }

//    public void delete() {
//        String sql = "delete from atm";
//        sqlSession.delete(sql);
//    }
//
//    public void update(Atm atm) {
//        String sql = "update atm set apassword=#{apassword},abalance=#{abalance} WHERE aname=#{aname}";
//        sqlSession.update(sql,atm);
//    }
//
//    public void insert(Atm atm) {
//        String sql = "insert into atm values (#{aname},#{apassword},#{abalance})";
//        sqlSession.insert(sql,atm);
//    }
//    public Atm selectOne(String aname) {
//        String sql = "select aname,apassword,abalance from atm where aname=#{anme}";
//        return sqlSession.selectOne(sql, aname, Atm.class);
//    }
//
//    public List<Atm> selectList() {
//        String sql = "select * from atm";
//        return sqlSession.selectList(sql, null, Atm.class);
//    }
//
//    public static void main(String[] args) {
//        Atm atm = new AtmDao().selectOne("mal");
//        System.out.println(atm);
//    }

}
