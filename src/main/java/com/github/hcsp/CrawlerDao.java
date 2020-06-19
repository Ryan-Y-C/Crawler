package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkAndDelete() throws SQLException;
    void insertNewsIntoDataBase(String url, String title, String content) throws SQLException;
    void insertProcessedLink(String link) throws SQLException;
    void insertUnProcessedLink(String link) throws SQLException;
    boolean isLinkProcessed(String link) throws SQLException;
}
