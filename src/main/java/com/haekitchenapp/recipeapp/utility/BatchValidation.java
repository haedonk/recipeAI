package com.haekitchenapp.recipeapp.utility;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
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
        if (summary.length() > 700) {
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

    public static String validateRewrittenInstructions(String text, String org) throws IllegalArgumentException {
        if (text == null) {
            throw new IllegalArgumentException("Rewritten instructions are null");
        }
        int originalLength = org != null ? org.length() : 0;
        if (originalLength > 300 && text.length() <= 100) {
            throw new IllegalArgumentException("Rewritten instructions too short: " + text.length() + " chars");
        }
        String[] sentences = text.split("\\.");
        if (originalLength > 300 && sentences.length < 3) {
            throw new IllegalArgumentException("Rewritten instructions have too few sentences: " + sentences.length);
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
        title = cleanTitle(title);
        if (title.length() < 4) {
            throw new IllegalArgumentException("Title is too short: " + title);
        }

        if (title.length() > 100) {
            throw new IllegalArgumentException("Title is too long: " + title);
        }

        String trimmedTitle = title.trim();
        char firstChar = trimmedTitle.charAt(0);

        if (!Character.isLetterOrDigit(firstChar)) {
            throw new IllegalArgumentException("Title must start with a letter or number: " + title);
        }

        if (title.matches(".*[^a-zA-Z0-9\\s\\-,'&\\.].*")) {
            throw new IllegalArgumentException("Title contains disallowed characters: " + title);
        }

        if (title.toLowerCase().contains("lorem") || title.toLowerCase().contains("null")) {
            throw new IllegalArgumentException("Title contains placeholder or invalid terms: " + title);
        }
        return title;
    }

    public static String cleanTitle(String title) {
        // Remove content in parentheses
        title = title.replaceAll("\\s*\\([^)]*\\)", "");

        // Replace colons, semicolons, pipes, arrows with dash or space
        title = title.replaceAll("[:;|=>]+", " -");

        // Remove quotation marks
        title = title.replace("\"", "").replace("'", "");

        // Normalize accented characters
        title = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");

        // Collapse whitespace and trim
        title = title.trim().replaceAll("\\s{2,}", " ");

        return title;
    }

    public static String sanitizeTitle(String title) {
        // Strip characters not allowed by your regex
        return title.replaceAll("[^a-zA-Z0-9\\s\\-,'&]", "").trim().replaceAll("\\s+", " ");
    }
}
