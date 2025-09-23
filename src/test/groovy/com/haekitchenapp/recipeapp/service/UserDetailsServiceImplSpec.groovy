package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.User
import com.haekitchenapp.recipeapp.repository.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import spock.lang.Specification

import java.util.Optional

class UserDetailsServiceImplSpec extends Specification {

    UserRepository userRepository = Mock()
    UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository)

    def "loadUserByUsername returns the existing user"() {
        given:
        String username = "existing-user"
        User user = new User(username, "existing@example.com", "password")
        userRepository.findByUsername(username) >> Optional.of(user)

        when:
        def result = service.loadUserByUsername(username)

        then:
        result.is(user)
    }

    def "loadUserByUsername throws when the user is missing"() {
        given:
        String username = "missing-user"
        userRepository.findByUsername(username) >> Optional.empty()

        when:
        service.loadUserByUsername(username)

        then:
        UsernameNotFoundException ex = thrown()
        ex.message == "User Not Found with username: ${username}"
    }
}
