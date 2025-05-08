package com.example.service;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class NewsScheduler {
    private Scheduler scheduler;

    public void start(JobDetail job, Trigger trigger) throws SchedulerException {
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(job, trigger);
    }

    public void stop() throws SchedulerException {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}