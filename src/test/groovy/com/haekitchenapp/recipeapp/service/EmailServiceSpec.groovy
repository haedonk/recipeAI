package com.haekitchenapp.recipeapp.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification

class EmailServiceSpec extends Specification {
    def mailSender = Mock(JavaMailSender)
    def service = new EmailService()

    def setup() {
        service.mailSender = mailSender
    }

    def "sendVerificationEmail sends email"() {
        when:
        service.sendVerificationEmail("test@example.com", "123456")

        then:
        1 * mailSender.send({ SimpleMailMessage msg ->
            msg.to == ["test@example.com"]
            msg.text.contains("123456")
        })
    }
}
