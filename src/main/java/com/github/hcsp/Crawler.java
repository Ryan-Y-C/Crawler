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
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Crawler {
    public static void main(String[] args) throws IOException {
        String http = "https://sina.cn";
        List<String> linkPool = new ArrayList<>();

        Set<String> processedLink = new HashSet<>();

        linkPool.add(http);

        while (!linkPool.isEmpty()) {
            //当获取到链接后将链接删除，ArrayList从尾部删除更有效率
            String link = linkPool.remove(linkPool.size() - 1);
            if (processedLink.contains(link)) {
                continue;
            }
            if (isInterestingLink(link)) {

                Document doc = httpGetAndParseHtml(link);

                doc.select("a[href]").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);

                storeIntoDataBaseIfIsNewsPage(doc);

                processedLink.add(link);

//                CloseableHttpClient httpclient = HttpClients.createDefault();
//                if (link.startsWith("//")) {
//                    link = "https:" + link;
//                }
//                System.out.println(link);
//                HttpGet httpGet = new HttpGet(link);
//                httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.113 Safari/537");

//                try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
//                    System.out.println(response1.getStatusLine());
//                    HttpEntity entity1 = response1.getEntity();
//                    String html = EntityUtils.toString(entity1);
//                    Document doc = Jsoup.parse(html);

//                }
            } else {
                continue;
            }
        }
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
        return isNotSurveyPage(link) && isNotLoginPage(link) && (isNewsPage(link) || isIndexPage(link));
    }

    private static boolean isNotSurveyPage(String link) {
        return !link.contains("survey");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport");
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina");
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }
}
