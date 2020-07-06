package com.github.hcsp;

public class CrawlerClient {
    public static void main(String[] args) {
//        CrawlerDao dao = new MyBatisCrawlerDao();
//        for (int i = 0; i < 8; i++) {
//            new Thread(new Crawler(dao)).start();
//        }

        MockDataGenerator mdg = new MockDataGenerator(100_0000);
        mdg.insertNews();
    }
}
