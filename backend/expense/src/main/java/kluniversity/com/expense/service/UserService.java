package kluniversity.com.expense.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
            throw new RuntimeException("Name is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        return userRepository.save(user);
    }

    // Login user
    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check normal BCrypt password
        if (passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

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

        throw new RuntimeException("Invalid password");
    }
}