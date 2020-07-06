package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator implements Runnable {
    private SqlSessionFactory sqlSessionFactory;
    private final int targetRowCount;

    MockDataGenerator(int targetRowCount) {
        this.targetRowCount = targetRowCount;
    }

    void insertNews() {
        String resource = "db/mybatis/config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> newsList = session.selectList("com.github.hcsp.MockMapper.selectNews");
            int count = targetRowCount - newsList.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    int index = random.nextInt(newsList.size());
                    News news = new News(newsList.get(index));
                    Instant createdAt = news.getCreatedAt();
                    Instant randomCreatAt = createdAt.minusSeconds(random.nextInt(3600 * 24 * 31));
                    news.setCreatedAt(randomCreatAt);
                    news.setModifiedAt(randomCreatAt);
                    session.insert("com.github.hcsp.MockMapper.insertNews", news);
                    if (count % 2000 == 0) {
                        session.flushStatements();
                        System.out.println(count);
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {
        insertNews();
    }
}
