package kluniversity.com.expense.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kluniversity.com.expense.config.JwtService;
import kluniversity.com.expense.dto.LoginRequest;
import kluniversity.com.expense.entity.User;
import kluniversity.com.expense.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Register user
    public User register(User user) {

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Name is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password is required");
        }

        String email = normalizeEmail(user.getEmail());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An account with this email already exists");
        }

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    // Login user
    public String login(LoginRequest request) {
        if (request == null
                || request.getEmail() == null
                || request.getEmail().trim().isEmpty()
                || request.getPassword() == null
                || request.getPassword().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> invalidCredentials());

        // Check normal BCrypt password
        if (user.getPassword() != null
                && passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            return jwtService.generateToken(user.getEmail());
        }

        // Temporary migration for old plain-text passwords
        if (request.getPassword().equals(user.getPassword())) {

            user.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );

            userRepository.save(user);

            return jwtService.generateToken(user.getEmail());
        }

        throw invalidCredentials();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}