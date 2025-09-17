package com.haekitchenapp.recipeapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.model.request.openAi.RecipeAiRequest;
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import com.haekitchenapp.recipeapp.service.OpenAiApi;
import com.openai.models.chat.completions.ChatCompletion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/openai")
@Slf4j
public class OpenAiController {

    @Autowired
    private OpenAiApi openAiApi;

    @PostMapping("/chat/simple")
    public ResponseEntity<ChatCompletion> simpleChat(@RequestBody Map<String, String> request) {
        String systemPrompt = request.get("systemPrompt");
        String userMessage = request.get("userMessage");

        log.info("Simple chat request - System: {}, User: {}", systemPrompt, userMessage);

        ChatCompletion response = openAiApi.chat(systemPrompt, userMessage);
        log.info("Simple chat response: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/recipe")
    public ResponseEntity<RecipeAISkeleton> recipeChat(@RequestBody Map<String, String> request) throws JsonProcessingException {
        String systemPrompt = request.get("systemPrompt");
        String userMessage = request.get("userMessage");

        log.info("Simple chat request - System: {}, User: {}", systemPrompt, userMessage);

        RecipeAISkeleton response = openAiApi.buildRecipe(systemPrompt, userMessage);
        log.info("Simple chat response: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/recipe/correction")
    public ResponseEntity<RecipeAISkeleton> recipeCorrectionChat(@RequestBody RecipeAiRequest request) throws JsonProcessingException {
        RecipeAISkeleton userMessage = request.getUserMessage();

        log.info("Recipe correction chat request - User: {}", userMessage);

        RecipeAISkeleton response = openAiApi.correctRecipe(userMessage, null);
        log.info("Simple chat response: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/conversation")
    public ResponseEntity<ChatCompletion> conversationChat(@RequestBody Map<String, Object> request) {
        String systemPrompt = (String) request.get("systemPrompt");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messagesList = (List<Map<String, String>>) request.get("messages");

        List<RoleContent> messages = messagesList.stream()
                .map(msg -> new RoleContent(msg.get("role"), msg.get("content")))
                .toList();

        log.info("Conversation chat request - System: {}, Messages count: {}", systemPrompt, messages.size());

        ChatCompletion response = systemPrompt != null ?
                openAiApi.chat(systemPrompt, messages) :
                openAiApi.chat(messages);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat/with-model")
    public ResponseEntity<ChatCompletion> chatWithModel(@RequestBody Map<String, Object> request) {
        String model = (String) request.get("model");
        String systemPrompt = (String) request.get("systemPrompt");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messagesList = (List<Map<String, String>>) request.get("messages");

        List<RoleContent> messages = messagesList.stream()
                .map(msg -> new RoleContent(msg.get("role"), msg.get("content")))
                .toList();

        log.info("Chat with model request - Model: {}, System: {}, Messages count: {}", model, systemPrompt, messages.size());

        ChatCompletion response = openAiApi.chatWithModel(model, systemPrompt, messages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("OpenAI Controller is working!");
    }
}
