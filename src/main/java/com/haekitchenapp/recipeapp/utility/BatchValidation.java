package com.haekitchenapp.recipeapp.utility;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class BatchValidation {

    public static String validateSummary(String summary) {
        if (summary == null) {
            throw new IllegalArgumentException("Summary is null");
        }
        if (summary.length() < 40) {
            throw new IllegalArgumentException("Summary is too short: " + summary.length() + " chars");
        }
        if (summary.length() > 500) {
            throw new IllegalArgumentException("Summary is too long: " + summary.length() + " chars");
        }
        if (summary.toLowerCase().contains("lorem")) {
            throw new IllegalArgumentException("Summary contains placeholder text: 'lorem'");
        }
        if (summary.split(" ").length < 8) {
            throw new IllegalArgumentException("Summary has too few words: " + summary.split(" ").length);
        }
        return summary;
    }

    public static String validateRewrittenInstructions(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Rewritten instructions are null");
        }
        if (text.length() <= 100) {
            throw new IllegalArgumentException("Rewritten instructions too short: " + text.length() + " chars");
        }
        if (text.split("\\.").length < 3) {
            throw new IllegalArgumentException("Rewritten instructions have too few sentences: " + text.split("\\.").length);
        }
        if (text.toLowerCase().contains("null")) {
            throw new IllegalArgumentException("Rewritten instructions contain 'null'");
        }
        return text;
    }

    public static List<Double> validateEmbedding(List<Double> embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException("Embedding is null");
        }
        if (embedding.size() != 768) {
            throw new IllegalArgumentException("Embedding length is incorrect: expected 768, got " + embedding.size());
        }

        for (int i = 0; i < embedding.size(); i++) {
            Double value = embedding.get(i);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("Embedding contains invalid value at index " + i + ": " + value);
            }
        }
        return embedding;
    }

    public static String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is empty or null");
        }

        if (title.length() < 4) {
            throw new IllegalArgumentException("Title is too short: " + title);
        }

        if (title.length() > 100) {
            throw new IllegalArgumentException("Title is too long: " + title);
        }

        if (!Character.isUpperCase(title.trim().charAt(0))) {
            throw new IllegalArgumentException("Title must start with a capital letter: " + title);
        }

        if (title.matches(".*[^a-zA-Z0-9\\s\\-,'&].*")) {
            throw new IllegalArgumentException("Title contains disallowed characters: " + title);
        }

        if (title.toLowerCase().contains("lorem") || title.toLowerCase().contains("null")) {
            throw new IllegalArgumentException("Title contains placeholder or invalid terms: " + title);
        }
        return title;
    }
}
