package com.github.hcsp;

import java.time.Instant;

public class News {
    private Integer id;
    private String url;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant modifiedAt;

    public News() {
    }

    News(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
    }

    News(News old) {
        this.id = old.id;
        this.url = old.url;
        this.title = old.title;
        this.content = old.content;
        this.createdAt = old.createdAt;
        this.modifiedAt = old.modifiedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    Instant getModifiedAt() {
        return modifiedAt;
    }

    void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
