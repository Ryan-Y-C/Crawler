package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


import java.sql.*;
import java.util.ArrayList;


public class Crawler implements Runnable {

    private CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    public void run() {

        try {
            String link;
            while ((link = dao.getNextLinkAndDelete()) != null) {
                if (dao.isLinkProcessed(link)) {
                    continue;
                }
                if (isInterestingLink(link)) {
                    Document doc = httpGetAndParseHtml(link);
                    //将该页面的所有链接放入待处理的数据库中
                    parseUrlsFromPageAndStoreIntoDataBase(doc);

                    //将当前新闻页面存储在news数据表中
                    storeIntoDataBaseIfIsNewsPage(link, doc);

                    //将当前链接放入已处理的数据库中
//                System.out.println(link);
                    dao.insertProcessedLink(link);
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    //将该页面的所有链接放入待处理的数据库中
    private void parseUrlsFromPageAndStoreIntoDataBase(Document doc) throws SQLException, UnsupportedEncodingException {
        for (Element aTag : doc.select("a[href]")) {
            String href = URLDecoder.decode(aTag.attr("href"), "UTF-8");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (isInterestedPage(href)) {
                dao.insertUnProcessedLink(href);
            }

        }
    }

    private boolean isInterestedPage(String href) {
        return !href.toLowerCase().startsWith("javascript") && !href.contains("my") && !href.contains("lives") &&
                !href.contains("zhongce") && !href.contains("so.sina.cn") && !href.contains("weather1")
                && !href.contains("wm");
    }


    //将新闻页面数据存储在news数据表中
    private void storeIntoDataBaseIfIsNewsPage(String link, Document doc) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.select("h1.art_tit_h1").text();
                String content = doc.select("section.art_pic_card").text();
                System.out.println(title);
                dao.insertNewsIntoDataBase(link, title, content);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1, "utf-8");
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return isNotRollPage(link) && isNotLoginPage(link) && (isNewsPage(link) || isIndexPage(link));
    }

    private static boolean isNotRollPage(String link) {
        return !link.contains("roll");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport");
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn/".equals(link);
    }
}
