package kluniversity.com.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.config.JwtService;
import kluniversity.com.expense.dto.LoginRequest;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, jwtService);
    }

    @Test
    void registrationNormalizesEmailAndHashesPassword() {
        User request = new User();
        request.setName("Test User");
        request.setEmail("  Test@Example.com ");
        request.setPassword("password123");
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.register(request);

        assertEquals("test@example.com", registered.getEmail());
        assertNotEquals("password123", registered.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(
                "password123", registered.getPassword()));
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void loginFindsEmailRegardlessOfCaseAndWhitespace() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("password123"));
        LoginRequest request = new LoginRequest();
        request.setEmail("  TEST@Example.com ");
        request.setPassword("password123");
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken("test@example.com"))
                .thenReturn("signed-token");

        assertEquals("signed-token", userService.login(request));
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void loginUsesSameUnauthorizedErrorForUnknownEmailAndWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("password123");
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(request));

        assertEquals(401, exception.getStatusCode().value());
        assertEquals("Invalid email or password", exception.getReason());
    }

    @Test
    void loginRejectsMissingCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(request));

        assertEquals(400, exception.getStatusCode().value());
        assertFalse(exception.getReason().isBlank());
    }

    @Test
    void registrationRejectsEmailAlreadyRegisteredWithDifferentCase() {
        User request = new User();
        request.setName("Test User");
        request.setEmail("TEST@example.com");
        request.setPassword("password123");
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(new User()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.register(request));

        assertEquals(409, exception.getStatusCode().value());
    }
}
