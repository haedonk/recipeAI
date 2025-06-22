package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.model.response.batch.Status;
import com.haekitchenapp.recipeapp.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/batch")
@Slf4j
public class RecipeBatchController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job recipeUpdateJob;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private RecipeService  recipeService;

    @PostMapping("/start")
    public String launch(@RequestParam String modValues) {
        CompletableFuture.runAsync(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("modValues", modValues)
                        .addLong("runId", System.currentTimeMillis())
                        .toJobParameters();

                jobLauncher.run(recipeUpdateJob, jobParameters);
            } catch (Exception e) {
                System.err.println("Job failed to start: " + e.getMessage());
            }
        });
        return "Job launched successfully. Check the status later.";
    }

    @PostMapping("/start-all")
    public ResponseEntity<String> launchAll(@RequestParam String modValues) {
        String[] modValuesArray = modValues.split(",");
        List<String> jobIds = new ArrayList<>();

        for (String modValue : modValuesArray) {
            String trimmedMod = modValue.trim();
            long runId = System.currentTimeMillis();

            CompletableFuture.runAsync(() -> {
                try {
                    JobParameters jobParameters = new JobParametersBuilder()
                            .addString("modValues", trimmedMod)
                            .addLong("runId", runId)
                            .toJobParameters();

                    JobExecution execution = jobLauncher.run(recipeUpdateJob, jobParameters);
                    log.info("Job started with modValue: {}, execution ID: {}",
                            trimmedMod, execution.getId());
                } catch (Exception e) {
                    log.error("Failed to start job with modValue {}: {}",
                            trimmedMod, e.getMessage(), e);
                }
            });

            jobIds.add(trimmedMod);

            // Small delay to avoid identical timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String message = "Launched " + jobIds.size() + " jobs with modValues: " + String.join(", ", jobIds);
        log.info(message);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/stop-all")
    public ResponseEntity<String> stopAllJobs() {
        List<String> stoppedJobs = new ArrayList<>();
        List<String> failedToStop = new ArrayList<>();

        // Get all running job executions
        Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions("");

        for (JobExecution execution : runningExecutions) {
            try {
                boolean stopped = jobOperator.stop(execution.getId());
                if (stopped) {
                    stoppedJobs.add(String.valueOf(execution.getId()));
                } else {
                    failedToStop.add(String.valueOf(execution.getId()));
                }
            } catch (Exception e) {
                log.error("Failed to stop job {}: {}", execution.getId(), e.getMessage());
                failedToStop.add(String.valueOf(execution.getId()));
            }
        }

        String message = String.format("Stopped %d jobs: %s. Failed to stop %d jobs: %s",
                stoppedJobs.size(), String.join(", ", stoppedJobs),
                failedToStop.size(), String.join(", ", failedToStop));

        log.info(message);
        return ResponseEntity.ok(message);
    }


    @PostMapping("/stop/{executionId}")
    public String stopJob(@PathVariable Long executionId) {
        try {
            boolean stopped = jobOperator.stop(executionId);
            return stopped ? "Job stopped." : "Could not stop job.";
        } catch (Exception e) {
            return "Failed to stop job: " + e.getMessage();
        }
    }

    @GetMapping("/status/{executionId}")
    public String checkStatus(@PathVariable Long executionId) {
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
            return "Status: " + jobExecution.getStatus().toString();
        } catch (Exception e) {
            return "Failed to get job status: " + e.getMessage();
        }
    }

    @GetMapping("/status")
    public Status getAllJobStatuses() {
        return recipeService.getRecipeMassageDetails();
    }
}
