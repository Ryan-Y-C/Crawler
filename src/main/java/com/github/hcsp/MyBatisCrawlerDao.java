package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        String resource = "db/mybatis/config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String getNextLinkAndDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("com.github.hcsp.MyMapper.selectNextLink");
            if (link != null) {
                session.delete("com.github.hcsp.MyMapper.deleteLink", link);
            }
            return link;
        }
    }


    @Override
    public void insertProcessedLink(String link) {
        System.out.println("处理过的链接"+link);
        insertIntoTable(link, "links_already_processed");
    }

    @Override
    public void insertUnProcessedLink(String link) {
        insertIntoTable(link, "links_to_be_processed");
    }

    private void insertIntoTable(String link, String tableName) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("tableName", tableName);
        parameter.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertLink", parameter);
        }
    }


    @Override
    public void insertNewsIntoDataBase(String url, String title, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertNews", new News(url, title, content));
        }
    }

    @Override
    public boolean isLinkProcessed(String link) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            int count = session.selectOne("com.github.hcsp.MyMapper.selectProcessedLink", link);
            return count != 0;
        }
    }
}
