package com.example.repository;

import com.example.dto.NewsArticle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NewsRepository {
    private final ConcurrentMap<String, NewsArticle> articles = new ConcurrentHashMap<>();

    public void save(NewsArticle article) {
        articles.putIfAbsent(article.getUrl(), article);
    }

    public List<NewsArticle> getAllArticles() {
        return new ArrayList<>(articles.values());
    }

    public NewsArticle getByUrl(String url) {
        return articles.get(url);
    }

    public void clear() {
        articles.clear();
    }
}