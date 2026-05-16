package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.TeacherSignupRequest;
import com.stoplight.classroom.dto.TeacherSignupResponse;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.model.UserStatus;
import com.stoplight.classroom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Default {@link TeacherSignupProvider} implementation. Stores teacher accounts in the
 * application's own {@code users} table.
 *
 * <p>Activated when {@code stoplight.auth.teacher-provider=local} (the default).</p>
 */
@Service
@ConditionalOnProperty(name = "stoplight.auth.teacher-provider", havingValue = "local", matchIfMissing = true)
public class LocalTeacherSignupProvider implements TeacherSignupProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final List<String> allowedEmailSuffixes;

    public LocalTeacherSignupProvider(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${stoplight.auth.allowed-email-suffixes:.edu}") String allowedSuffixes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.allowedEmailSuffixes = Arrays.stream(allowedSuffixes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    @Override
    public TeacherSignupResponse requestSignup(TeacherSignupRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!hasAllowedSuffix(email)) {
            throw new IllegalArgumentException(
                    "Email must end with one of: " + String.join(", ", allowedEmailSuffixes));
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.TEACHER);
        user.setEmail(email);
        user.setStatus(UserStatus.PENDING);
        return TeacherSignupResponse.from(userRepository.save(user));
    }

    @Override
    public List<TeacherSignupResponse> listByStatus(UserStatus status) {
        return userRepository.findByStatus(status).stream()
                .filter(u -> u.getRole() == Role.TEACHER)
                .map(TeacherSignupResponse::from)
                .toList();
    }

    @Override
    public TeacherSignupResponse approve(Long userId) {
        return transition(userId, UserStatus.APPROVED);
    }

    @Override
    public TeacherSignupResponse reject(Long userId) {
        return transition(userId, UserStatus.REJECTED);
    }

    @Override
    public User findForLogin(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    private TeacherSignupResponse transition(Long userId, UserStatus target) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Signup not found"));
        if (user.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Not a teacher signup");
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Signup is not pending (current status: " + user.getStatus() + ")");
        }
        user.setStatus(target);
        return TeacherSignupResponse.from(userRepository.save(user));
    }

    private boolean hasAllowedSuffix(String email) {
        if (allowedEmailSuffixes.isEmpty()) {
            return true; // policy intentionally disabled
        }
        return allowedEmailSuffixes.stream().anyMatch(email::endsWith);
    }
}
