package com.stoplight.classroom.config;

import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedConfig.class);

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                @Value("${admin.default-username}") String username,
                                @Value("${admin.default-password}") String password) {
        return args -> {
            if (!userRepository.existsByUsername(username)) {
                userRepository.save(new User(username, passwordEncoder.encode(password), Role.ADMIN));
                log.info("Default admin user '{}' created", username);
            }
        };
    }
}
