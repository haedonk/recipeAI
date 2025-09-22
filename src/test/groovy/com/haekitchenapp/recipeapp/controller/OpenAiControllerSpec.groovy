package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.model.request.openAi.RecipeAiRequest
import com.haekitchenapp.recipeapp.model.request.togetherAi.RoleContent
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton
import com.haekitchenapp.recipeapp.service.OpenAiApi
import com.openai.models.chat.completions.ChatCompletion
import spock.lang.Specification

class OpenAiControllerSpec extends Specification {

    OpenAiApi openAiApi
    OpenAiController controller

    def setup() {
        openAiApi = Mock(OpenAiApi)
        controller = new OpenAiController(openAiApi)
    }

    def "simple chat forwards payload to service"() {
        given:
        def request = [systemPrompt: 'system', userMessage: 'user']
        def chatCompletion = Mock(ChatCompletion)

        when:
        def response = controller.simpleChat(request)

        then:
        1 * openAiApi.chat('system', 'user') >> chatCompletion
        response.statusCode.value() == 200
        response.body.is(chatCompletion)
    }

    def "recipe chat delegates to recipe builder"() {
        given:
        def request = [systemPrompt: 'chef', userMessage: 'make pasta']
        def recipeSkeleton = new RecipeAISkeleton()

        when:
        def response = controller.recipeChat(request)

        then:
        1 * openAiApi.buildRecipe('chef', 'make pasta') >> recipeSkeleton
        response.statusCode.value() == 200
        response.body.is(recipeSkeleton)
    }

    def "recipe correction forwards skeleton to service"() {
        given:
        def skeleton = new RecipeAISkeleton(title: 'Fix me')
        def request = new RecipeAiRequest('ignored', skeleton)
        def corrected = new RecipeAISkeleton(title: 'Fixed')

        when:
        def response = controller.recipeCorrectionChat(request)

        then:
        1 * openAiApi.correctRecipe(skeleton, null) >> corrected
        response.statusCode.value() == 200
        response.body.is(corrected)
    }

    def "conversation chat builds role content list when system prompt provided"() {
        given:
        def request = [
                systemPrompt: 'system',
                messages    : [
                        [role: 'user', content: 'Hi'],
                        [role: 'assistant', content: 'Hello']
                ]
        ]
        def completion = Mock(ChatCompletion)

        when:
        def response = controller.conversationChat(request)

        then:
        1 * openAiApi.chat('system', { List<RoleContent> roles ->
            roles == [new RoleContent('user', 'Hi'), new RoleContent('assistant', 'Hello')]
        }) >> completion
        response.statusCode.value() == 200
        response.body.is(completion)
    }

    def "conversation chat without system prompt uses two argument overload"() {
        given:
        def request = [
                messages: [[role: 'user', content: 'Only user']]
        ]
        def completion = Mock(ChatCompletion)

        when:
        def response = controller.conversationChat(request)

        then:
        1 * openAiApi.chat({ List<RoleContent> roles ->
            roles == [new RoleContent('user', 'Only user')]
        }) >> completion
        response.statusCode.value() == 200
        response.body.is(completion)
    }

    def "chat with model sends model, system prompt, and messages"() {
        given:
        def request = [
                model       : 'gpt-test',
                systemPrompt: 'system',
                messages    : [[role: 'user', content: 'Hello model']]
        ]
        def completion = Mock(ChatCompletion)

        when:
        def response = controller.chatWithModel(request)

        then:
        1 * openAiApi.chatWithModel('gpt-test', 'system', { List<RoleContent> roles ->
            roles == [new RoleContent('user', 'Hello model')]
        }) >> completion
        response.statusCode.value() == 200
        response.body.is(completion)
    }

    def "health endpoint returns expected message"() {
        when:
        def response = controller.test()

        then:
        response.statusCode.value() == 200
        response.body == 'OpenAI Controller is working!'
    }
}
