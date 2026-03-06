package ru.netology.cloudservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Отдельная конфигурация для PasswordEncoder, чтобы избежать циклической зависимости:
 * SecurityConfig → AuthServiceImpl → PasswordEncoder (был в SecurityConfig).
 * Вынесен в отдельный класс без зависимостей.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
