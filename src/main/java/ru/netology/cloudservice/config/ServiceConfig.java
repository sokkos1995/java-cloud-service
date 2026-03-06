package ru.netology.cloudservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.netology.cloudservice.repository.AuthTokenRepository;
import ru.netology.cloudservice.repository.FileRepository;
import ru.netology.cloudservice.repository.UserRepository;
import ru.netology.cloudservice.service.AuthService;
import ru.netology.cloudservice.service.AuthServiceImpl;
import ru.netology.cloudservice.service.FileService;
import ru.netology.cloudservice.service.FileServiceImpl;
import ru.netology.cloudservice.storage.FileStorageService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Явная регистрация сервисов — обход проблем с component scanning в Docker/JAR.
 */
@Configuration
public class ServiceConfig {

    @Bean
    public AuthService authService(UserRepository userRepository,
                                   AuthTokenRepository authTokenRepository,
                                   PasswordEncoder passwordEncoder) {
        return new AuthServiceImpl(userRepository, authTokenRepository, passwordEncoder);
    }

    @Bean
    public FileService fileService(FileRepository fileRepository,
                                   UserRepository userRepository,
                                   FileStorageService fileStorageService) {
        return new FileServiceImpl(fileRepository, userRepository, fileStorageService);
    }
}
