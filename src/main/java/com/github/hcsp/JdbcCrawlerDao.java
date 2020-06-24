package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;


class JdbcCrawlerDao implements CrawlerDao {
    private String USER = "ryan";

    private String PASSWORD = "123";

    private String URL = "jdbc:h2:file:J:\\Crawler\\news";
    private Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //从待处理的数据库中取出链接并且删除
    public String getNextLinkAndDelete() throws SQLException {
        String nextLink = getNextLink("select link from LINKS_TO_BE_PROCESSED limit 1");
        if (nextLink != null) {
            removeLinkFromDataBase(nextLink);
        }
        return nextLink;
    }

    private String getNextLink(String sql) throws SQLException {
        String link;
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                link = resultSet.getString(1);
                removeLinkFromDataBase(link);
                return link;
            }
        }
        return null;
    }

    private void removeLinkFromDataBase(String link) throws SQLException {
        insertLinkOrDeleteLink(link, "delete from LINKS_TO_BE_PROCESSED where link=?");
    }

    public void insertNewsIntoDataBase(String url, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into NEWS(TITLE,CONTENT,URL,CREATED_AT,MODIFIED_AT)values ( ?,?,?,now(),now())")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, url);
            statement.executeUpdate();
        }

    }

    @Override
    public void insertProcessedLink(String link) throws SQLException {
        insertLinkOrDeleteLink(link, "INSERT INTO LINKS_ALREADY_PROCESSED (LINK)VALUES ( ? )");
    }

    @Override
    public void insertUnProcessedLink(String link) throws SQLException {
        insertLinkOrDeleteLink(link,"INSERT INTO LINKS_TO_BE_PROCESSED (LINK)VALUES ( ? )");
    }

    //将页面中的链接加入到待处理的数据表中
    public void insertLinkOrDeleteLink(String link,String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    public boolean isLinkProcessed(String link) throws SQLException {
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
}
