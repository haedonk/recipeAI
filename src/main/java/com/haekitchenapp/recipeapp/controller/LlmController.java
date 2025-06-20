package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequestEmbedDto;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequestSummarizeDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import com.haekitchenapp.recipeapp.service.TogetherAiApi;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/llm")
@Slf4j
public class LlmController {

//    @Autowired
//    private TogetherAiApi togetherAiApi;
//
//    @PostMapping("/rewrite-instructions")
//    public ResponseEntity<ApiResponse<LlmResponse>> rewriteInstructions(@RequestBody @Valid LLMRequestSummarizeDto llmRequest) {
//        log.info("Received request to rewrite instructions: {}", llmRequest);
//        LlmResponse rewrittenInstructions = togetherAiApi.callLLMRewrite(llmRequest);
//        return ResponseEntity.ok(ApiResponse.success("Rewrite completed successfully", rewrittenInstructions));
//    }
//
//    @PostMapping("/embed-instructions")
//    public ResponseEntity<ApiResponse<LlmResponse>> rewriteInstructions(@RequestBody @Valid LLMRequestEmbedDto llmRequest) {
//        log.info("Received request to embed instructions: {}", llmRequest);
//        LlmResponse rewrittenInstructions = togetherAiApi.embed(llmRequest);
//        return ResponseEntity.ok(ApiResponse.success("Rewrite completed successfully", rewrittenInstructions));
//    }
}
