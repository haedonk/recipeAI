package com.haekitchenapp.recipeapp.config.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RecipeJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job recipeUpdateJob;

    @Autowired
    public RecipeJobLauncher(JobLauncher jobLauncher, Job recipeUpdateJob) {
        this.jobLauncher = jobLauncher;
        this.recipeUpdateJob = recipeUpdateJob;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Starting recipe processing job on application startup");
        launch();
    }

    // Run every day at 8:00 PM (20:00)
    @Scheduled(cron = "0 0 20 * * ?")
    public void scheduledJobLaunch() {
        log.info("Starting scheduled evening recipe processing job");
        launch();
    }

    public void launch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runId", System.currentTimeMillis()) // ensure unique execution
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(recipeUpdateJob, params);
            log.info("Job Status: {}", execution.getStatus());
        } catch (Exception e) {
            log.error("Job failed to start: {}", e.getMessage(), e);
        }
    }
}
