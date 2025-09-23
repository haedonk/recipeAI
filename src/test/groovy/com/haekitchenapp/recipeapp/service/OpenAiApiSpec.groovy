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
    private def chatResource = Mock()
    private def completionsResource = Mock()

    private OpenAiApi api

    void setup() {
        openAIClient.chat() >> chatResource
        chatResource.completions() >> completionsResource
        api = new OpenAiApi(openAIClient, config, llmLoggingService, unitService)
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
        def completion = completionWithContent("cmpl-agg", "{}")
        ChatCompletionCreateParams captured

        when:
        def result = api.chat(systemPrompt, messages)

        then:
        1 * completionsResource.create(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params ->
            captured = params
            completion
        }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", systemPrompt, messages, completion)
        result.is(completion)

        def roleMessages = extractRoleMessages(captured)
        roleMessages["system"] == [systemPrompt]
        roleMessages["user"] == ["First requirement\nSecond requirement"]
        captureModelId(captured) == "gpt-4o-mini"
    }

    def "chatWithModel uses provided model and prompt"() {
        given:
        def customModel = "gpt-custom"
        def systemPrompt = "Custom system"
        def messages = [RoleContent.getUserRole("Only message")]
        def completion = completionWithContent("cmpl-chat-model", "{}")
        ChatCompletionCreateParams captured

        when:
        def result = api.chatWithModel(customModel, systemPrompt, messages)

        then:
        1 * completionsResource.create(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params ->
            captured = params
            completion
        }
        1 * llmLoggingService.saveQueryLog(customModel, systemPrompt, messages, completion)
        result.is(completion)

        captureModelId(captured) == customModel
        extractRoleMessages(captured) == [system: [systemPrompt], user: ["Only message"]]
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
        ChatCompletionCreateParams captured

        when:
        def skeleton = api.buildRecipe(systemPrompt, messages)

        then:
        1 * completionsResource.create(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params ->
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
        String capturedSystemPrompt
        List<RoleContent> capturedMessages

        when:
        def skeleton = api.buildRecipe("Please craft recipe")

        then:
        1 * completionsResource.create(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params -> completion }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", { String systemPrompt ->
            capturedSystemPrompt = systemPrompt
            true
        }, { List<RoleContent> msgs ->
            capturedMessages = msgs
            true
        }, completion)

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
        ChatCompletionCreateParams captured
        String capturedSystemPrompt

        when:
        def result = api.correctRecipe(skeleton, userPrompt)

        then:
        1 * completionsResource.create(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params ->
            captured = params
            completion
        }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", { String sys ->
            capturedSystemPrompt = sys
            true
        }, _ as List<RoleContent>, completion)

        result.title == "Title"
        capturedSystemPrompt.contains("tablespoon")

        def roleMessages = extractRoleMessages(captured)
        roleMessages["system"].size() == 1
        roleMessages["system"].first().contains("tablespoon")
        roleMessages["user"].first().contains("\"title\"")
        roleMessages["user"].first().contains(userPrompt)
    }

    def "buildRecipe returns null when completion has no choices"() {
        given:
        config.getChatModel() >> "gpt-4o-mini"
        def systemPrompt = "Empty choices"
        def messages = [RoleContent.getUserRole("test")]
        def completion = Stub(ChatCompletion) {
            choices() >> []
            id() >> "cmpl-empty"
            usage() >> Optional.empty()
        }

        when:
        def result = api.buildRecipe(systemPrompt, messages)

        then:
        1 * completionsResource.create(_ as ChatCompletionCreateParams) >> { ChatCompletionCreateParams params -> completion }
        1 * llmLoggingService.saveQueryLog("gpt-4o-mini", systemPrompt, messages, completion)
        result == null
    }

    private static ChatCompletion completionWithContent(String id, String content) {
        def message = Stub() {
            content() >> Optional.ofNullable(content)
        }
        def choice = Stub() {
            message() >> message
        }
        Stub(ChatCompletion) {
            id() >> id
            choices() >> [choice]
            usage() >> Optional.empty()
        }
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
            String role = methodValue(message, "role")?.toString()
            String content = resolveContent(methodValue(message, "content"))
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
        directId ? resolveContent(directId) : resolveContent(model)
    }

    private static Object methodValue(Object target, String methodName) {
        if (target == null) {
            return null
        }
        def method = target.class.methods.find { it.name == methodName && it.parameterCount == 0 }
        method ? method.invoke(target) : null
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

        def text = methodValue(value, "text")
        if (text != null) {
            def resolved = resolveContent(text)
            if (resolved != null) {
                return resolved
            }
        }

        def innerValue = methodValue(value, "value")
        if (innerValue != null && innerValue != value) {
            def resolved = resolveContent(innerValue)
            if (resolved != null) {
                return resolved
            }
        }

        def innerContent = methodValue(value, "content")
        if (innerContent != null && innerContent != value) {
            def resolved = resolveContent(innerContent)
            if (resolved != null) {
                return resolved
            }
        }

        value.toString()
    }
}
