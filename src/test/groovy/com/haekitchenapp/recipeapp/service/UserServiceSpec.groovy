package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.User
import com.haekitchenapp.recipeapp.exception.InvalidCredentialsException
import com.haekitchenapp.recipeapp.exception.UserNotFoundException
import com.haekitchenapp.recipeapp.model.request.user.LoginRequestDto
import com.haekitchenapp.recipeapp.repository.EmailVerificationRepository
import com.haekitchenapp.recipeapp.repository.UserRepository
import com.haekitchenapp.recipeapp.utility.UserMapper
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCrypt
import spock.lang.Specification

class UserServiceSpec extends Specification {
    def userRepo = Mock(UserRepository)
    def emailRepo = Mock(EmailVerificationRepository)
    def emailService = Mock(EmailService)
    def mapper = new UserMapper()
    def service = new UserService(userRepository: userRepo,
            emailVerificationRepository: emailRepo,
            emailService: emailService,
            userMapper: mapper)

    def "generateVerificationCode returns six digit numeric string"() {
        expect:
        service.generateVerificationCode() =~ /^\d{6}$/
    }

    def "login throws InvalidCredentialsException for blank password"() {
        when:
        service.login(new LoginRequestDto(email:"a@b.com", password:""))

        then:
        thrown(InvalidCredentialsException)
    }

    def "login throws UserNotFoundException when user absent"() {
        given:
        userRepo.findByEmail("a@b.com") >> Optional.empty()

        when:
        service.login(new LoginRequestDto(email:"a@b.com", password:"x"))

        then:
        thrown(UserNotFoundException)
    }

    def "login returns response entity for valid credentials"() {
        given:
        def user = new User(id:1L, email:"a@b.com", passwordHash:BCrypt.hashpw("x", BCrypt.gensalt()))
        userRepo.findByEmail("a@b.com") >> Optional.of(user)

        when:
        def resp = service.login(new LoginRequestDto(email:"a@b.com", password:"x"))

        then:
        resp instanceof ResponseEntity
        resp.body.data.email == "a@b.com"
    }
}

