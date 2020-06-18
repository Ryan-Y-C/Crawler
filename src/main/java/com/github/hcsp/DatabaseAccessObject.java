package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;


class DatabaseAccessObject {
    private String USER = "ryan";

    private String PASSWORD = "123";

    private String URL = "jdbc:h2:file:J:\\Crawler\\news";
    private Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    DatabaseAccessObject() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //从待处理的数据库中取出链接并且删除
    String getNextLinkAndDelete() throws SQLException {
        String nextLink = getNextLink("select link from links_to_be_processed limit 1");
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
        insertLinkIntoDataBase(link, "delete from LINKS_TO_BE_PROCESSED where link=?");
    }

    void insertNewsIntoDataBase(String url, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into NEWS(TITLE,CONTENT,URL,CREATED_AT,MODIFIED_AT)values ( ?,?,?,now(),now())")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, url);
            statement.executeUpdate();
        }

    }

    //将页面中的链接加入到待处理的数据表中
    void insertLinkIntoDataBase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    boolean isLinkProcessed(String link) throws SQLException {
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
