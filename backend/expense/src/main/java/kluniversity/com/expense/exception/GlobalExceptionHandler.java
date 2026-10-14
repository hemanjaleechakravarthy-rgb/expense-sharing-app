package kluniversity.com.expense.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception) {
        String message = exception.getReason() == null
                ? "Request failed"
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of("message", message));
    }
}
