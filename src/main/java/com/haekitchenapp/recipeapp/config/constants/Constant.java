package com.haekitchenapp.recipeapp.config.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Constant {

    public static final String REWRITE_PROMPT =
            "Clean up and rewrite the following cooking instructions to make them easy to read and follow. " +
                    "Use clear, natural phrasing without abbreviations or shorthand. " +
                    "Keep the steps in logical order, and format the entire response as a single comma-separated line " +
                    "with no numbering, no line breaks, and no extra punctuation.";

    public static final String EMBED_PROMPT =
            "Generate a semantic embedding vector for the following text, focusing on meaning and overall context: ";

    public static final String SUMMARIZE_PROMPT =
            "Summarize the following cooking instructions into 1–2 concise, complete sentences, capturing the core steps" +
                    " and overall purpose for embedding: ";




    public static final String RECIPE_SYSTEM_PROMPT =
            "You are a helpful assistant that rewrites cooking instructions to " +
                    "be clearer and easier to read.";

    public static final String STRICT_RECIPE_REVIEWER_SYSTEM_PROMPT =
            "You are a strict recipe reviewer. Only suggest rewriting when the instructions are unclear, poorly " +
                    "formatted, or missing key information. Respond with only 'true' or 'false', and nothing else.";




    public static final String TITLE_SYSTEM_PROMPT =
            "You are a title formatter. Your job is to rewrite recipe titles to be clean, properly capitalized, " +
                    "and user-friendly. Remove unnecessary labels, versions, or formatting artifacts. Do not include " +
                    "explanations — only return the fixed title.";

    public static final String TITLE_PROMPT =
            "Format the following recipe title into a clean, properly capitalized, user-friendly " +
                    "title without extra labels or formatting: ";

}
