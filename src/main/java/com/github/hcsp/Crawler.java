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
import java.net.URLDecoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Crawler {
    public static void main(String[] args) throws IOException, SQLException {
        String url = "jdbc:h2:file:J:\\Crawler\\news";
        String user = "ryan";
        String password = "123";
        Connection connection = DriverManager.getConnection(url, user, password);
        //从数据库加载即将待处理的链接的代码
        List<String> linkPool = loadUrlsFromDataBase(sql("links_to_be_processed"), connection);

        //从数据库加载已经处理的链接的代码
//        Set<String> processedLink = new HashSet<>(loadUrlsFromDataBase(sql("links_already_processed"), connection));


            while (!linkPool.isEmpty()) {
                //处理完成后更新数据库
                String link = URLDecoder.decode(linkPool.remove(linkPool.size() - 1), "UTF-8");
                removeFromDataBase(connection, link);
                boolean flag=false;
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM LINKS_ALREADY_PROCESSED WHERE LINK=?")) {
                    statement.setString(1,link);
                    ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()){
                        flag=true;
                    }
                }

                if (flag) {
                    continue;
                }
                if (isInterestingLink(link)) {

                    Document doc = httpGetAndParseHtml(link);

                    for (Element aTag : doc.select("a[href]")) {
                        String href = aTag.attr("href");
                        try (PreparedStatement statement = connection.prepareStatement("insert into LINKS_TO_BE_PROCESSED (link)values ( ? )")) {
                            statement.setString(1, href);
                            statement.executeUpdate();
                        }
                    }

                    storeIntoDataBaseIfIsNewsPage(doc);

                    try (PreparedStatement statement = connection.prepareStatement("insert into LINKS_ALREADY_PROCESSED (link)values ( ? )")) {
                        statement.setString(1, link);
                        statement.executeUpdate();
                    }

//                    processedLink.add(link);
                } else {
                    continue;
                }
            }
    }

    private static void removeFromDataBase(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("delete from LINKS_TO_BE_PROCESSED where link=?")) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static String sql(String table) {
        return "select link from " + table;
    }

    private static List<String> loadUrlsFromDataBase(String sql, Connection connection) throws SQLException {
        ArrayList<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        }
        return results;
    }

    private static void storeIntoDataBaseIfIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.select("h1.art_tit_h1").text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }

        System.out.println(link);
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
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
