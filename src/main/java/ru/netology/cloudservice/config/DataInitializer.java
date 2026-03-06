package ru.netology.cloudservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.netology.cloudservice.entity.UserEntity;
import ru.netology.cloudservice.repository.UserRepository;

/**
 * Инициализация тестовых данных при первом запуске.
 *
 * <p>Создаёт пользователя с логином "user" и паролем "password", если в БД
 * ещё нет ни одного пользователя. Пароль хэшируется через BCrypt.
 *
 * <p>В продакшене лучше использовать миграции (Flyway/Liquibase) или
 * отдельный админ-интерфейс для создания пользователей.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_LOGIN = "user";
    private static final String DEFAULT_PASSWORD = "password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            UserEntity user = new UserEntity();
            user.setLogin(DEFAULT_LOGIN);
            user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userRepository.save(user);
        }
    }
}
