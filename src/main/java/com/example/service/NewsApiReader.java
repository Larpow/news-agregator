package com.example.service;

import com.example.dto.NewsArticle;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class NewsApiReader {
    private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";
    private final String apiKey;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NewsApiReader(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    public List<NewsArticle> fetchNews(String query, int pageSize) throws IOException {
        List<NewsArticle> articles = new ArrayList<>();

        String url = String.format("%s?q=%s&pageSize=%d&apiKey=%s",
                NEWS_API_URL, query, pageSize, apiKey);

        HttpGet request = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            if (rootNode.has("articles")) {
                for (JsonNode articleNode : rootNode.get("articles")) {
                    NewsArticle article = new NewsArticle();
                    article.setSourceName(articleNode.path("source").path("name").asText());
                    article.setAuthor(articleNode.path("author").asText());
                    article.setTitle(articleNode.path("title").asText());
                    article.setDescription(articleNode.path("description").asText());
                    article.setUrl(articleNode.path("url").asText());
                    article.setUrlToImage(articleNode.path("urlToImage").asText());
//                    article.setPublishedAt(new Date(articleNode.path("publishedAt").asText()));
                    String dateStr = articleNode.path("publishedAt").asText();
                    article.setPublishedAt(java.time.ZonedDateTime.parse(dateStr).toInstant());
                    article.setContent(articleNode.path("content").asText());

                    articles.add(article);
                }
            }
        }

        return articles;
    }

    public void close() throws IOException {
        httpClient.close();
    }
}