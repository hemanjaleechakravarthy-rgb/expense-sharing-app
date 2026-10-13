package kluniversity.com.expense.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigTest {

    @Test
    void corsAllowsIdempotencyAndExistingAuthorizationHeaders() {
        SecurityConfig config =
                new SecurityConfig(mock(JwtAuthenticationFilter.class));
        var cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertTrue(cors.getAllowedHeaders().contains("Idempotency-Key"));
        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
        assertTrue(cors.getAllowedHeaders().contains("Content-Type"));
    }
}
