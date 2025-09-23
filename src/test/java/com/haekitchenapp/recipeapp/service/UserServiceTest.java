package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.EmailVerification;
import com.haekitchenapp.recipeapp.exception.InvalidValidationCodeException;
import com.haekitchenapp.recipeapp.exception.UserNotFoundException;
import com.haekitchenapp.recipeapp.model.request.email.VerifyEmailRequestDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.repository.EmailVerificationRepository;
import com.haekitchenapp.recipeapp.repository.UserRepository;
import com.haekitchenapp.recipeapp.utility.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.spy(new UserService(userRepository, emailVerificationRepository, emailService, userMapper));
    }

    @Test
    void isUserEmailVerified_returnsVerificationStatus() {
        Long userId = 1L;
        EmailVerification verification = new EmailVerification();
        verification.setVerified(true);

        when(emailVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));

        ResponseEntity<ApiResponse<Boolean>> response = userService.isUserEmailVerified(userId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isTrue();
        verify(emailVerificationRepository).findByUserId(userId);
    }

    @Test
    void isUserEmailVerified_throwsWhenUserIdIsNull() {
        assertThatThrownBy(() -> userService.isUserEmailVerified(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID must not be null");
        verifyNoInteractions(emailVerificationRepository);
    }

    @Test
    void verifyByUserId_returnsVerificationWhenFound() {
        Long userId = 42L;
        EmailVerification verification = new EmailVerification();
        when(emailVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));

        EmailVerification result = userService.verifyByUserId(userId);

        assertThat(result).isSameAs(verification);
        verify(emailVerificationRepository).findByUserId(userId);
    }

    @Test
    void verifyByUserId_throwsWhenUserIdIsNull() {
        assertThatThrownBy(() -> userService.verifyByUserId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID must not be null");
        verifyNoInteractions(emailVerificationRepository);
    }

    @Test
    void verifyByUserId_throwsWhenVerificationMissing() {
        Long userId = 99L;
        when(emailVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyByUserId(userId))
                .isInstanceOf(InvalidValidationCodeException.class)
                .hasMessage("No verification found for this user ID");
    }

    @Test
    void verifyEmail_throwsWhenAlreadyVerified() {
        Long userId = 11L;
        String code = "ABC123";
        EmailVerification verification = new EmailVerification();
        verification.setVerified(true);
        verification.setExpiresAt(Timestamp.from(Instant.now().plusSeconds(3600)));
        when(emailVerificationRepository.findByUserIdAndVerificationCode(userId, code))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> userService.verifyEmail(new VerifyEmailRequestDto(userId, code)))
                .isInstanceOf(InvalidValidationCodeException.class)
                .hasMessage("Email already verified");

        verify(emailVerificationRepository, never()).save(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_throwsWhenCodeExpired() {
        Long userId = 12L;
        String code = "CODE";
        EmailVerification verification = new EmailVerification();
        verification.setVerified(false);
        verification.setExpiresAt(Timestamp.from(Instant.now().minusSeconds(60)));
        when(emailVerificationRepository.findByUserIdAndVerificationCode(userId, code))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> userService.verifyEmail(new VerifyEmailRequestDto(userId, code)))
                .isInstanceOf(InvalidValidationCodeException.class)
                .hasMessage("Verification code expired");

        verify(emailVerificationRepository, never()).save(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_updatesVerificationWhenValid() {
        Long userId = 13L;
        String code = "VALID";
        EmailVerification verification = new EmailVerification();
        verification.setVerified(false);
        verification.setExpiresAt(Timestamp.from(Instant.now().plusSeconds(3600)));
        when(emailVerificationRepository.findByUserIdAndVerificationCode(userId, code))
                .thenReturn(Optional.of(verification));

        userService.verifyEmail(new VerifyEmailRequestDto(userId, code));

        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(captor.capture());
        EmailVerification saved = captor.getValue();
        assertThat(saved).isSameAs(verification);
        assertThat(saved.isVerified()).isTrue();
        assertThat(saved.getVerifiedAt()).isNotNull();
    }

    @Test
    void resendVerificationEmail_returnsWhenAlreadyVerified() {
        Long userId = 14L;
        EmailVerification verification = new EmailVerification();
        verification.setVerified(true);
        when(emailVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));

        ResponseEntity<ApiResponse<Object>> response = userService.resendVerificationEmail(userId);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Email already verified");
        verify(emailVerificationRepository, never()).save(any(EmailVerification.class));
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void resendVerificationEmail_sendsNewCodeWhenNotVerified() {
        Long userId = 15L;
        String expectedEmail = "user@example.com";
        String generatedCode = "123456";
        EmailVerification verification = new EmailVerification();
        verification.setVerified(false);
        verification.setUserId(userId);

        when(emailVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(verification));
        when(userRepository.findEmailById(userId)).thenReturn(Optional.of(expectedEmail));
        doReturn(generatedCode).when(userService).generateVerificationCode();

        ResponseEntity<ApiResponse<Object>> response = userService.resendVerificationEmail(userId);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Verification email resent successfully");

        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(verificationCaptor.capture());
        EmailVerification saved = verificationCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getVerificationCode()).isEqualTo(generatedCode);
        verify(emailService).sendVerificationEmail(expectedEmail, generatedCode);
    }

    @Test
    void getUserEmailById_throwsWhenUserMissing() {
        Long userId = 16L;
        when(userRepository.findEmailById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserEmailById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }
}
