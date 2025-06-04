package com.haekitchenapp.recipeapp.utility

import com.haekitchenapp.recipeapp.model.request.user.UserRequestDto
import spock.lang.Specification

class UserMapperSpec extends Specification {
    def mapper = new UserMapper()

    def "mapToUser hashes password"() {
        given:
        def dto = new UserRequestDto(email: "a@b.com", password: "secret")

        when:
        def user = mapper.mapToUser(dto)

        then:
        user.email == "a@b.com"
        user.passwordHash
        user.passwordHash != 'secret'
    }
}
