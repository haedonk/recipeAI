package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.model.response.batch.Status;
import com.haekitchenapp.recipeapp.service.RecipeService;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/batch")
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
