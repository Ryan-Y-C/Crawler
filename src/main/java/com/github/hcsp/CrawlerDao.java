package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkAndDelete() throws SQLException;
    void insertNewsIntoDataBase(String url, String title, String content) throws SQLException;
    void insertLinkIntoDataBase(String link, String sql) throws SQLException;
    boolean isLinkProcessed(String link) throws SQLException;
}
