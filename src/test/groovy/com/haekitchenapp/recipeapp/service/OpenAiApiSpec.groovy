package com.haekitchenapp.recipeapp.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.haekitchenapp.recipeapp.config.api.OpenAiConfig
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeIngredientAiSkeletonResponse
import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import spock.lang.Specification

import java.util.Optional

class OpenAiApiSpec extends Specification {

    private OpenAIClient openAIClient = Mock()
    private OpenAiConfig config = Mock()
    private LlmLoggingService llmLoggingService = Mock()
    private UnitService unitService = Mock()
    private JwtTokenService jwtTokenService = Mock()

    private OpenAiApi api

    void setup() {
        api = Spy(OpenAiApi, constructorArgs: [openAIClient, config, llmLoggingService, unitService, jwtTokenService])
    }

    def "chat builds aggregated user prompt and logs query"() {
        given:
        config.getChatModel() >> "gpt-4o-mini"
        def systemPrompt = "System instructions"
        def messages = [
                RoleContent.getUserRole("First requirement"),
                new RoleContent("assistant", "irrelevant"),
                RoleContent.getUserRole("Second requirement")
        ]
        def completion = completionWithContent("cmpl-agg", "{}");
        ChatCompletionCreateParams captured = null

        when:
        def result = api.chat(systemPrompt, messages)

        then:
        1 * api.createCompletion(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params ->
            captured = params
            completion
        }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", systemPrompt, messages, completion)
        // Verify by value instead of identity
        captureModelId(captured) == "gpt-4o-mini"

        def roleMessages = extractRoleMessages(captured)
        roleMessages["system"] == [systemPrompt]
        roleMessages["user"] == ["First requirement\nSecond requirement"]
        // Result should be the same instance the seam returned
        result.id() == "cmpl-agg"
    }

    def "buildRecipe parses completion into RecipeAISkeleton"() {
        given:
        config.getChatModel() >> "gpt-4o-mini"
        def systemPrompt = "Recipe system"
        def messages = [RoleContent.getUserRole("Generate soup recipe")]
        def json = new ObjectMapper().writeValueAsString(new RecipeAISkeleton(
                "Test Soup",
                "Combine ingredients and simmer.",
                "Comforting soup",
                [new RecipeIngredientAiSkeletonResponse("Tomato", "2", "cup")] as Set,
                10,
                20,
                4
        ))
        def completion = completionWithContent("cmpl-build", json)
        ChatCompletionCreateParams captured = null

        when:
        def skeleton = api.buildRecipe(systemPrompt, messages)

        then:
        1 * api.createCompletion(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params ->
            captured = params
            completion
        }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", systemPrompt, messages, completion)

        skeleton.title == "Test Soup"
        skeleton.instructions == "Combine ingredients and simmer."
        skeleton.ingredients*.name == ["Tomato"]
        skeleton.servings == 4

        def roleMessages = extractRoleMessages(captured)
        roleMessages["system"] == [systemPrompt]
        roleMessages["user"] == ["Generate soup recipe"]
        captureModelId(captured) == "gpt-4o-mini"
    }

    def "buildRecipe helper uses recipe prompt with available units"() {
        given:
        config.getChatModel() >> "gpt-4o-mini"
        1 * unitService.getAllUnitsMap() >> [1L: "cup", 2L: "tsp"]
        def completion = completionWithContent("cmpl-helper", minimalRecipeJson())
        String capturedSystemPrompt = null
        List<RoleContent> capturedMessages = null

        when:
        def skeleton = api.buildRecipe("Please craft recipe")

        then:
        1 * api.createCompletion(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params -> completion }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", _, _, completion) >> { String model, String systemPrompt, List<RoleContent> msgs, ChatCompletion comp ->
            capturedSystemPrompt = systemPrompt
            capturedMessages = msgs
        }

        skeleton.title == "Title"
        capturedSystemPrompt.contains("cup")
        capturedSystemPrompt.contains("tsp")
        capturedMessages*.content == ["Please craft recipe"]
    }

    def "correctRecipe helper serializes skeleton and appends user prompt"() {
        given:
        config.getChatModel() >> "gpt-4o-mini"
        1 * unitService.getAllUnitsMap() >> [1L: "tablespoon"]
        def skeleton = new RecipeAISkeleton(
                "Original",
                "Mix ingredients.",
                "Summary",
                [new RecipeIngredientAiSkeletonResponse("Salt", "1", "tsp")] as Set,
                5,
                10,
                2
        )
        def userPrompt = "Make it less salty"
        def completion = completionWithContent("cmpl-correct", minimalRecipeJson())
        ChatCompletionCreateParams captured = null

        when:
        def result = api.correctRecipe(skeleton, userPrompt)

        then:
        1 * api.createCompletion(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params ->
            captured = params
            completion
        }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", _, _, completion)

        result.title == "Title"

        def roleMessages = extractRoleMessages(captured)
        roleMessages["user"].size() == 2
        roleMessages["user"][0].contains("\"title\"")
        roleMessages["user"][1] == userPrompt
    }

    def "buildRecipe returns null when completion has no choices"() {
        given:
        config.getChatModel() >> "gpt-4o-mini"
        def systemPrompt = "Empty choices"
        def messages = [RoleContent.getUserRole("test")]
        def completion = completionWithEmptyChoices("cmpl-empty")

        when:
        def result = api.buildRecipe(systemPrompt, messages)

        then:
        1 * api.createCompletion(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params -> completion }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", systemPrompt, messages, completion)
        result == null
    }

    private ChatCompletion completionWithContent(String id, String content) {
        def json = """
        {
          "id": "${id}",
          "object": "chat.completion",
          "model": "gpt-4o-mini",
          "created": 0,
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": ${content != null ? ObjectMapperHolder.MAPPER.writeValueAsString(content) : 'null'}
              },
              "finish_reason": "stop"
            }
          ]
        }
        """.stripIndent()
        ObjectMapperHolder.MAPPER.readValue(json, ChatCompletion)
    }

    private ChatCompletion completionWithEmptyChoices(String id) {
        def json = """
        {
          "id": "${id}",
          "object": "chat.completion",
          "model": "gpt-4o-mini",
          "created": 0,
          "choices": []
        }
        """.stripIndent()
        ObjectMapperHolder.MAPPER.readValue(json, ChatCompletion)
    }

    private static String minimalRecipeJson() {
        new ObjectMapper().writeValueAsString(new RecipeAISkeleton(
                "Title",
                "Do things.",
                "Summary",
                [new RecipeIngredientAiSkeletonResponse("Item", "1", "cup")] as Set,
                1,
                2,
                3
        ))
    }

    private static Map<String, List<String>> extractRoleMessages(ChatCompletionCreateParams params) {
        Map<String, List<String>> result = [:].withDefault { [] }
        params.messages().each { Object message ->
            String role
            String content

            def sys = unwrapOptional(methodValue(message, "system"))
            def usr = unwrapOptional(methodValue(message, "user"))
            if (sys != null) {
                role = "system"
                content = resolveContent(unwrapOptional(methodValue(sys, "content")))
            } else if (usr != null) {
                role = "user"
                content = resolveContent(unwrapOptional(methodValue(usr, "content")))
            } else {
                role = methodValue(message, "role")?.toString()
                content = resolveContent(methodValue(message, "content"))
            }

            result[role] << content
        }
        result
    }

    private static String captureModelId(ChatCompletionCreateParams params) {
        def model = methodValue(params, "model")
        if (model == null) {
            return null
        }
        def directId = methodValue(model, "id") ?: methodValue(model, "getId")
        def raw = directId ? resolveContent(directId) : resolveContent(model)
        return normalizeModelId(raw)
    }

    private static String normalizeModelId(String id) {
        return id == null ? null : id.toLowerCase().replace('_', '-')
    }

    private static Object methodValue(Object target, String methodName) {
        if (target == null) {
            return null
        }
        def method = target.class.methods.find { it.name == methodName && it.parameterCount == 0 }
        method ? method.invoke(target) : null
    }

    private static Object unwrapOptional(Object value) {
        if (value instanceof Optional) {
            return value.orElse(null)
        }
        return value
    }

    private static String resolveContent(Object value) {
        if (value == null) {
            return null
        }
        if (value instanceof CharSequence) {
            return value.toString()
        }
        if (value instanceof Optional) {
            return resolveContent(value.orElse(null))
        }
        if (value instanceof Collection) {
            return value.collect { resolveContent(it) }
                    .findAll { it != null }
                    .join("")
        }

        def text = methodValue(value, "text") ?: methodValue(value, "getText")
        if (text != null) {
            def resolved = resolveContent(text)
            if (resolved != null) {
                return resolved
            }
        }

        def innerValue = methodValue(value, "value") ?: methodValue(value, "getValue")
        if (innerValue != null && innerValue != value) {
            def resolved = resolveContent(innerValue)
            if (resolved != null) {
                return resolved
            }
        }

        def innerContent = methodValue(value, "content") ?: methodValue(value, "getContent")
        if (innerContent != null && innerContent != value) {
            def resolved = resolveContent(innerContent)
            if (resolved != null) {
                return resolved
            }
        }

        value.toString()
    }

    private static class ObjectMapperHolder {
        static final ObjectMapper MAPPER = new ObjectMapper()
    }
}
