package com.github.hcsp;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchDataGenerator {

    public static void main(String[] args) {
        List<News> newsData = getNewsFromMysql();
        System.out.println(newsData.size());
//        insertDataIntoElasticsearch(newsData);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> blockInsertionElasticsearch(newsData)).start();
        }
    }

    //通过mybatis获取mysql数据
    private static List<News> getNewsFromMysql() {

        String resource = "db/mybatis/config.xml";
        SqlSessionFactory sqlSessionFactory;
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            return sqlSession.selectList("com.github.hcsp.MockMapper.selectNews");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void blockInsertionElasticsearch(List<News> newsData) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
            for (int i = 0; i < 1000; i++) {
                BulkRequest bulkRequest = new BulkRequest();
                newsData.forEach(news -> bulkRequest.add(getIndexRequest(news)));
                BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                System.out.println(bulk.status() + Thread.currentThread().getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertDataIntoElasticsearch(List<News> newsData) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {
            for (int i = 0; i < 1000; i++) {
                newsData.parallelStream().forEach(news -> {
                    try {
                        client.index(getIndexRequest(news), RequestOptions.DEFAULT);
                        System.out.println("当前线程:" + Thread.currentThread().getName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static IndexRequest getIndexRequest(News news) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("url", news.getUrl());
        jsonMap.put("title", news.getTitle());
        jsonMap.put("content", news.getContent().substring(0, 5));
        jsonMap.put("createdAt", news.getCreatedAt());
        jsonMap.put("modifiedAt", news.getModifiedAt());
        IndexRequest request = new IndexRequest("news");
        request.source(jsonMap);
        return request;
    }
}
