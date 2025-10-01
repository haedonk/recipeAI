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
            "Generate a semantic embedding for the following recipe text, capturing its ingredients, cooking methods, and overall meaning:";

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

    public static final String CORRECT_INGREDIENTS_PROMPT =
            "You are a culinary normalizer. Given a recipe JSON object, return a fully updated JSON object that matches this Java model.\n" +
                    "\n" +
                    "Rules:\n" +
                    "- Keep all fields from the input; only update where needed.\n" +
                    "- Ingredients:\n" +
                    "  - Convert vulgar fractions (e.g., \"5/10\") to decimals, but still as a string (e.g., \"0.5\").\n" +
                    "  - Quantity must always be a string.\n" +
                    "  - Allowed units: %s\n" +
                    "  - Never use '0' as a quantity. If an amount is unspecified or just a pinch/dash, set quantity to \"to taste\" and unit to null.\n" +
                    "- Instructions:\n" +
                    "  - Must be a single string with clear, step-by-step directions.\n" +
                    "  - Separate each step with a period.\n" +
                    "  - Do not use numbering, bullets, or line breaks.\n" +
                    "- Times:\n" +
                    "  - prepTime and cookTime must be integers in minutes.\n" +
                    "  - If prepTime looks too large (e.g., given in seconds), convert to minutes.\n" +
                    "  - cookTime should reflect actual steps (e.g., 3.5 hours ≈ 210 minutes).\n" +
                    "- Servings:\n" +
                    "  - If null or 0, infer from total meat weight (225–300 g cooked meat per serving).\n" +
                    "  - Clamp between 2–12.\n" +
                    "  - Must be an integer.\n" +
                    "- Output:\n" +
                    "  - Return ONLY a JSON object compatible with the model.\n" +
                    "  - No prose, no code fences.\n";

    public static final String RECIPE_PARSER_GENERATOR_PROMPT =
            "You are a recipe parser and generator. Return one JSON object matching the given Java model.\n" +
                    "\n" +
                    "Rules:\n" +
                    "- Populate all required fields: title, summary, instructions, ingredients, prepTime, cookTime, servings.\n" +
                    "- If data is missing, invent realistic defaults (e.g., 4 servings, 10 min prep, 20 min cook).\n" +
                    "- Ingredients:\n" +
                    "  - Must cover every item mentioned in the instructions.\n" +
                    "  - Quantity must always be a string.\n" +
                    "  - Allowed units: %s\n" +
                    "  - Never use '0' as a quantity. If an amount is unspecified or just a pinch/dash, set quantity to \"to taste\" and unit to null.\n" +
                    "- Instructions:\n" +
                    "  - Must be a single string with clear, step-by-step directions.\n" +
                    "  - Separate each step with a period.\n" +
                    "  - Do not use numbering, bullets, or line breaks.\n" +
                    "- Times:\n" +
                    "  - prepTime and cookTime must be integers in minutes.\n" +
                    "  - If not given, invent realistic values.\n" +
                    "- Servings:\n" +
                    "  - If null or 0, invent a realistic value between 2–12.\n" +
                    "- Output:\n" +
                    "  - Return ONLY a JSON object compatible with the model.\n" +
                    "  - No prose, no code fences.\n";


}
