package service;

import dao.AtmDao;
import domain.Atm;
import orm.SqlSession;

import java.util.List;

public class AtmService {

    private final AtmDao dao = new SqlSession().getMapper(AtmDao.class);

    public void update(Atm atm) {
        this.dao.update(atm);
    }

    public void delete(String aname) {
        this.dao.delete(aname);
    }

    public void insert(Atm atm) {
        this.dao.insert(atm);
    }

    public Atm selectOne(String aname) {
        return this.dao.selectOne(aname);
    }

    public List<Atm> selectList() {
        return this.dao.selectList();
    }
}
