package com.haekitchenapp.recipeapp.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification

class EmailServiceSpec extends Specification {

    JavaMailSender javaMailSender = Mock()
    EmailService emailService = new EmailService(javaMailSender)

    def "sendVerificationEmail populates and dispatches expected message"() {
        given:
        def recipient = 'user@example.com'
        def verificationCode = '123456'
        SimpleMailMessage capturedMessage = null

        when:
        emailService.sendVerificationEmail(recipient, verificationCode)

        then:
        1 * javaMailSender.send(_ as SimpleMailMessage) >> { SimpleMailMessage message ->
            capturedMessage = message
        }

        capturedMessage != null
        capturedMessage.to?.toList() == [recipient]
        capturedMessage.subject == 'Verify Your Email'
        capturedMessage.text == "Your verification code is: ${verificationCode}"
    }
}
