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

    public static final String RECIPE_TITLE_PROMPT = "Return only the cleaned recipe title. Do not include the word " +
            "'recipe', explanations, or formatting. If the title is already clean, return it unchanged. Respond with one line only:";

}
