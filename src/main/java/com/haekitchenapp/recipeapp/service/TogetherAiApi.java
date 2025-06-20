package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.config.api.TogetherAiConfig;
import com.haekitchenapp.recipeapp.exception.ClientSide4XXException;
import com.haekitchenapp.recipeapp.exception.HttpError5XXException;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequest;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequestEmbedDto;
import com.haekitchenapp.recipeapp.model.request.togetherAi.LLMRequestSummarizeDto;
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent;
import com.haekitchenapp.recipeapp.model.response.togetherAi.LlmResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.haekitchenapp.recipeapp.config.constants.Constant.*;


@Service
public class TogetherAiApi {

    @Autowired
    private WebClient togetherWebClient;

    @Autowired
    private TogetherAiConfig config;


    public LlmResponse callLLMRewrite(String instructions){

        String prompt = REWRITE_PROMPT + instructions;

        RoleContent systemRole = RoleContent.getUserRole(prompt);
        LLMRequest llmRequest = LLMRequest.getDefaultChatRequest(config.getChatModel());
        llmRequest.getMessages().add(systemRole);

        return getChatResponse(llmRequest);
    }

    public LlmResponse callLLMRewrite(LLMRequestSummarizeDto llmRequestSummarizeDto){
        return getChatResponse(llmRequestSummarizeDto.toLlmRequest());
    }

    public LlmResponse callLLMSummarize(String recipeDto){
        String prompt = SUMMARIZE_PROMPT + recipeDto;
        RoleContent systemRole = RoleContent.getUserRole(prompt);
        LLMRequest llmRequest = LLMRequest.getDefaultChatRequest(config.getChatModel());
        llmRequest.getMessages().add(systemRole);

        return getChatResponse(llmRequest);
    }

    public LlmResponse callLLMFormatTitle(String title){
        String prompt = RECIPE_TITLE_PROMPT + "\n\n" + title;
        RoleContent systemRole = RoleContent.getUserRole(prompt);
        LLMRequest llmRequest = LLMRequest.getDefaultChatRequest(config.getChatModel());
        llmRequest.getMessages().add(systemRole);

        return getChatResponse(llmRequest);
    }

    private LlmResponse getChatResponse(LLMRequest llmRequest){
        try{
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

    public LlmResponse embed(String input){
        LLMRequest llmRequest = LLMRequest.getDefaultEmbedRequest(config.getEmbedModel(), List.of(EMBED_PROMPT + input));
        return getEmbedResponse(llmRequest);
    }

    private LlmResponse getEmbedResponse(LLMRequest llmRequest) {
        try{
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
