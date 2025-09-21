package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.config.api.TogetherAiConfig;
import com.haekitchenapp.recipeapp.exception.ClientSide4XXException;
import com.haekitchenapp.recipeapp.exception.HttpError5XXException;
import com.haekitchenapp.recipeapp.exception.LlmApiException;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequest;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequestEmbedDto;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequestSummarizeDto;
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.haekitchenapp.recipeapp.config.constants.Constant.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class TogetherAiApi {

    private final WebClient togetherWebClient;
    private final TogetherAiConfig config;
    private final LlmLoggingService llmLoggingService;

    public LlmResponse callIsBadRecipe(String recipeDto, Long recipeId) {
        RoleContent systemRole = RoleContent.getUserRole(recipeDto);
        LLMRequest llmRequest = LLMRequest.getDefaultChatRequest(config.getChatSmallModel(), STRICT_RECIPE_REVIEWER_SYSTEM_PROMPT);
        llmRequest.getMessages().add(systemRole);
        LlmResponse response = getChatResponse(llmRequest);
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            llmLoggingService.saveTogetherAiQueryLog(config.getChatSmallModel(), recipeDto, response, recipeId);
        } else {
            throw new LlmApiException("Context not returned in the response");
        }
        return response;
    }

    public LlmResponse callLLMRewrite(String instructions, Long recipeId){
        String prompt = REWRITE_PROMPT + instructions;
        RoleContent systemRole = RoleContent.getUserRole(prompt);
        LLMRequest llmRequest = LLMRequest.getDefaultChatRequest(config.getChatModel(), RECIPE_SYSTEM_PROMPT);
        llmRequest.getMessages().add(systemRole);
        LlmResponse response = getChatResponse(llmRequest);
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            llmLoggingService.saveTogetherAiQueryLog(config.getChatModel(), prompt, response, recipeId);
        } else {
            throw new LlmApiException("Context not returned in the response");
        }
        return response;
    }

    public LlmResponse callLLMRewrite(LLMRequestSummarizeDto llmRequestSummarizeDto){
        return getChatResponse(llmRequestSummarizeDto.toLlmRequest());
    }

    public LlmResponse callLLMSummarize(String recipeDto, Long recipeId){
        String prompt = SUMMARIZE_PROMPT + recipeDto;
        RoleContent systemRole = RoleContent.getUserRole(prompt);
        LLMRequest llmRequest = LLMRequest.getDefaultChatRequest(config.getChatModel(), RECIPE_SYSTEM_PROMPT);
        llmRequest.getMessages().add(systemRole);
        LlmResponse response = getChatResponse(llmRequest);
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            llmLoggingService.saveTogetherAiQueryLog(config.getChatModel(), prompt, response, recipeId);
        } else {
            throw new LlmApiException("Context not returned in the response");
        }
        return response;
    }

    public LlmResponse callLLMFormatTitle(String title, Long recipeId){
        String prompt =  TITLE_PROMPT+ "\n\n" + title;
        RoleContent systemRole = RoleContent.getUserRole(prompt);
        LLMRequest llmRequest = LLMRequest.getDefaultChatRequest(config.getChatSmallModel(), TITLE_SYSTEM_PROMPT);
        llmRequest.getMessages().add(systemRole);
        LlmResponse response = getChatResponse(llmRequest);
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            llmLoggingService.saveTogetherAiQueryLog(config.getChatSmallModel(), prompt, response, recipeId);
        } else {
            throw new LlmApiException("Context not returned in the response");
        }
        return response;
    }

    private LlmResponse getChatResponse(LLMRequest llmRequest){
        try{
            log.info("Calling Together AI Chat API with request: {}", llmRequest);
            return togetherWebClient.post()
                    .uri(config.getChatEndpoint())
                    .bodyValue(llmRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new ClientSide4XXException("Client error: " + errorBody))
                            ))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new HttpError5XXException("Server error: " + errorBody))
                            ))
                    .bodyToMono(LlmResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            // API response error with status code
            System.err.println("HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            // Network error, timeout, etc.
            System.err.println("Unexpected error: " + e.getMessage());
            throw e;
        }
    }


    public LlmResponse embed(LLMRequestEmbedDto llmRequestEmbedDto){
        return getEmbedResponse(llmRequestEmbedDto.toLlmRequest());
    }

    public LlmResponse embed(List<String> inputs){
        inputs = inputs.stream()
                .map(input -> EMBED_PROMPT + input)
                .toList();
        LLMRequest llmRequest = LLMRequest.getDefaultEmbedRequest(config.getEmbedModel(), inputs);
        return getEmbedResponse(llmRequest);
    }

    public LlmResponse embed(String input, Long recipeId) {
        LLMRequest llmRequest = LLMRequest.getDefaultEmbedRequest(config.getEmbedModel(), List.of(EMBED_PROMPT + input));
        LlmResponse response = getEmbedResponse(llmRequest);
        if (response != null && response.getData() != null && !(response.getData().get(0).getEmbedding().length == 0)) {
            llmLoggingService.saveTogetherAiQueryLog(config.getEmbedModel(), llmRequest.getInput().toString(), response, recipeId);
        } else {
            throw new LlmApiException("Context not returned in the response");
        }
        return response;
    }

    public Double[] embed(String input) {
        LLMRequest llmRequest = LLMRequest.getDefaultEmbedRequest(config.getEmbedModel(), List.of(EMBED_PROMPT + input));
        LlmResponse response = getEmbedResponse(llmRequest);
        if (response != null && response.getData() != null && !(response.getData().get(0).getEmbedding().length == 0)) {
            return response.getData().get(0).getEmbedding();
        } else {
            throw new LlmApiException("Context not returned in the response");
        }
    }

    private LlmResponse getEmbedResponse(LLMRequest llmRequest) {
        try{
            log.info("Calling Together AI Embed API with request: {}", llmRequest);
            return togetherWebClient.post()
                    .uri(config.getEmbedEndpoint())
                    .bodyValue(llmRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new ClientSide4XXException("Client error: " + errorBody))
                            ))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new HttpError5XXException("Server error: " + errorBody))
                            ))
                    .bodyToMono(LlmResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            // API response error with status code
            System.err.println("HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            // Network error, timeout, etc.
            System.err.println("Unexpected error: " + e.getMessage());
            throw e;
        }
    }

}
