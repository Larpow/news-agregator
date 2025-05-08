package com.example.service;

import com.example.dto.NewsArticle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArticleContentExtractor {
    public void enrichArticleContent(NewsArticle article) throws IOException {
        if (article.getUrl() == null || article.getUrl().isEmpty()) {
            return;
        }

        Document doc = Jsoup.connect(article.getUrl())
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        // Извлекаем основной текст статьи (эвристический подход)
        String fullText = extractMainText(doc);
        article.setContent(fullText);

        // Извлекаем все изображения
        List<String> images = extractImages(doc);
        article.setImages(images);
    }

    private String extractMainText(Document doc) {
        // Эвристика: ищем элементы с наибольшим количеством текста
        // Можно улучшить с помощью более сложных алгоритмов
        Element body = doc.body();
        if (body == null) return "";

        // Удаляем ненужные элементы (скрипты, стили и т.д.)
        body.select("script, style, nav, footer").remove();

        // Возвращаем весь текст
        return body.text();
    }

    private List<String> extractImages(Document doc) {
        List<String> images = new ArrayList<>();
        Elements imgElements = doc.select("img[src]");

        for (Element img : imgElements) {
            String src = img.absUrl("src");
            if (!src.isEmpty()) {
                images.add(src);
            }
        }

        return images;
    }
}