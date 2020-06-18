package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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


public class Crawler {
    private static final String USER = "ryan";

    private static final String PASSWORD = "123";

    //https://news.sina.cn/?wm=1871
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        String url = "jdbc:h2:file:J:\\Crawler\\news";
        Connection connection = DriverManager.getConnection(url, USER, PASSWORD);

        String link;
        while ((link = getNextLinkAndDelete(connection)) != null) {

            if (isLinkProcessed(connection, link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                //将该页面的所有链接放入待处理的数据库中
                parseUrlsFromPageAndStoreIntoDataBase(connection, doc);

                //将当前新闻页面存储在news数据表中
                storeIntoDataBaseIfIsNewsPage(connection, link, doc);

                //将当前链接放入已处理的数据库中
                System.out.println(link);
                insertLinkIntoDataBase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED (LINK)VALUES ( ? )");
            }
        }
    }

    private static String getNextLinkAndDelete(Connection connection) throws SQLException {
        String nextLink = getNextLink("select link from links_to_be_processed limit 1", connection);
        if (nextLink != null) {
            removeLinkFromDataBase(connection, nextLink);
        }
        return nextLink;
    }

    //将该页面的所有链接放入待处理的数据库中
    private static void parseUrlsFromPageAndStoreIntoDataBase(Connection connection, Document doc) throws SQLException, UnsupportedEncodingException {
        for (Element aTag : doc.select("a[href]")) {
            String href = URLDecoder.decode(aTag.attr("href"), "UTF-8");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if (isInterestedPage(href)) {
                insertLinkIntoDataBase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED (LINK)VALUES ( ? )");
            }

        }
    }

    private static boolean isInterestedPage(String href) {
        return !href.toLowerCase().startsWith("javascript") && !href.contains("my") && !href.contains("lives") &&
                !href.contains("zhongce") && !href.contains("so.sina.cn") && !href.contains("weather1");
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        boolean flag = false;
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM LINKS_ALREADY_PROCESSED WHERE LINK=?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                flag = true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return flag;
    }

    private static void insertLinkIntoDataBase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static void removeLinkFromDataBase(Connection connection, String link) throws SQLException {
        insertLinkIntoDataBase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link=?");
    }

    private static String getNextLink(String sql, Connection connection) throws SQLException {
        String link;
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                link = resultSet.getString(1);
                removeLinkFromDataBase(connection, link);
                return link;
            }
        }
        return null;
    }

    //将新闻页面数据存储在news数据表中
    private static void storeIntoDataBaseIfIsNewsPage(Connection connection, String link, Document doc) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.select("h1.art_tit_h1").text();
                String content = doc.select("section.art_pic_card").text();
                try (PreparedStatement statement = connection.prepareStatement("insert into NEWS(TITLE,CONTENT,URL,CREATED_AT,MODIFIED_AT)values ( ?,?,?,now(),now())")) {
                    statement.setString(1, title);
                    statement.setString(2, content);
                    statement.setString(3, link);
                    statement.executeUpdate();
                }
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
