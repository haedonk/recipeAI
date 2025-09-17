// OpenAIChatRequest.java
package com.haekitchenapp.recipeapp.model.request.openAi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIChatRequest {

    public String model;
    public List<Message> messages;
    public Double temperature;

    @JsonProperty("max_tokens")
    public Integer maxTokens;

    @JsonProperty("max_completion_tokens")     // for newer models (e.g., gpt-5-*, o1/o3)
    public Integer maxCompletionTokens;

    @JsonProperty("response_format")
    public ResponseFormat responseFormat; // optional

    public OpenAIChatRequest() {}

    /** plain text reply */
    public static OpenAIChatRequest of(String model, String system, Object userContent) {
        OpenAIChatRequest r = new OpenAIChatRequest();
        r.model = model;
        r.temperature = 0.2;
        r.maxTokens = 800;
        r.messages = List.of(
            new Message("system", system),
            new Message("user",   userContent)
        );
        return r;
    }

    /** force JSON reply (assistant returns a single JSON object) */
    public static OpenAIChatRequest json(String model, String system, Object userContent) {
        OpenAIChatRequest r = of(model, system, userContent);
        r.responseFormat = new ResponseFormat("json_object");
        return r;
    }

    // ----- nested types -----
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        public String role;       // "system" | "user" | "assistant"
        public Object content;    // String or JSON-able object

        public Message() {}
        public Message(String role, Object content) {
            this.role = role;
            this.content = content;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseFormat {
        public String type; // "json_object"
        public ResponseFormat() {}
        public ResponseFormat(String type) { this.type = type; }
    }
}
