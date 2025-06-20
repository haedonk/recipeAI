package com.haekitchenapp.recipeapp.config.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RecipeJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job recipeUpdateJob;

    @Autowired
    public RecipeJobLauncher(JobLauncher jobLauncher, Job recipeUpdateJob) {
        this.jobLauncher = jobLauncher;
        this.recipeUpdateJob = recipeUpdateJob;
    }

    public void launchWithModValues(String modValues) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("modValues", modValues)
                    .addLong("runId", System.currentTimeMillis()) // ensure unique execution
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(recipeUpdateJob, params);
            System.out.println("Job Status: " + execution.getStatus());
        } catch (Exception e) {
            System.err.println("Job failed to start: " + e.getMessage());
        }
    }
}
