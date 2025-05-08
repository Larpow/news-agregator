package com.example;

import com.example.dto.NewsArticle;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import com.example.repository.NewsRepository;
import com.example.service.ArticleContentExtractor;
import com.example.service.NewsApiReader;
import com.example.service.NewsScheduler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String CONFIG_FILE = "/config.properties";
    private static NewsRepository repository = new NewsRepository();

    public static void main(String[] args) {
        try {
            // Загрузка конфигурации
            Properties config = loadConfig();
            String apiKey = config.getProperty("newsapi.key");
            String query = config.getProperty("newsapi.query", "technology");
            int interval = Integer.parseInt(config.getProperty("scheduler.interval", "60"));

            // Инициализация сервисов
            NewsApiReader newsReader = new NewsApiReader(apiKey);
            ArticleContentExtractor contentExtractor = new ArticleContentExtractor();

            // Создание задачи для планировщика
            JobDetail job = JobBuilder.newJob(NewsFetchJob.class)
                    .withIdentity("newsFetchJob", "group1")
                    .usingJobData("query", query)
                    .build();

            // Передача зависимостей в Job
            job.getJobDataMap().put("newsReader", newsReader);
            job.getJobDataMap().put("contentExtractor", contentExtractor);
            job.getJobDataMap().put("repository", repository);

            // Настройка триггера
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("newsTrigger", "group1")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(interval)
                            .repeatForever())
                    .build();

            // Запуск планировщика
            NewsScheduler scheduler = new NewsScheduler();
            scheduler.start(job, trigger);

            // Для демонстрации - подождем немного и выведем новости
            TimeUnit.SECONDS.sleep(10);
            printNews(repository);

            // Остановка приложения по нажатию Enter
            System.out.println("Press Enter to stop...");
            System.in.read();

            scheduler.stop();
            newsReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Properties loadConfig() throws IOException {
        Properties prop = new Properties();
        String configPath = "src/main/resources/config.properties";
        try (InputStream input = new FileInputStream(configPath)) {
            prop.load(input);
        }
        return prop;
    }

    private static void printNews(NewsRepository repository) {
        System.out.println("Latest news:");
        for (NewsArticle article : repository.getAllArticles()) {
            System.out.println("---");
            System.out.println("Title: " + article.getTitle());
            System.out.println("Source: " + article.getSourceName());
            System.out.println("URL: " + article.getUrl());
            System.out.println("Published: " + article.getPublishedAt());
            System.out.println("Image: " + article.getUrlToImage());
            System.out.println();
        }
    }

    public static class NewsFetchJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();

            NewsApiReader newsReader = (NewsApiReader) dataMap.get("newsReader");
            ArticleContentExtractor extractor = (ArticleContentExtractor) dataMap.get("contentExtractor");
            NewsRepository repository = (NewsRepository) dataMap.get("repository");
            String query = dataMap.getString("query");

            try {
                System.out.println("Fetching news for query: " + query);
                List<NewsArticle> articles = newsReader.fetchNews(query, 10);

                for (NewsArticle article : articles) {
                    try {
                        // Дополняем статью полным текстом и изображениями
                        extractor.enrichArticleContent(article);
                        repository.save(article);
                    } catch (IOException e) {
                        System.err.println("Error enriching article: " + article.getUrl());
                        e.printStackTrace();
                    }
                }

                System.out.println("Fetched " + articles.size() + " articles");
            } catch (IOException e) {
                throw new JobExecutionException("Failed to fetch news", e);
            }
        }
    }
}