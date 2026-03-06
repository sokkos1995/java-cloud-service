package ru.netology.cloudservice.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.cloudservice.dto.LoginRequest;
import ru.netology.cloudservice.entity.AuthTokenEntity;
import ru.netology.cloudservice.entity.UserEntity;
import ru.netology.cloudservice.repository.AuthTokenRepository;
import ru.netology.cloudservice.repository.UserRepository;
import ru.netology.cloudservice.security.AuthenticatedUser;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация сервиса авторизации.
 *
 * <p>Как работает авторизация на основе токенов (token-based auth):
 *
 * <ul>
 *   <li>1. Пользователь отправляет login + password на POST /login</li>
 *   <li>2. Сервер проверяет учётные данные. При успехе генерирует уникальный токен (UUID)</li>
 *   <li>3. Токен сохраняется в БД (таблица auth_tokens) и возвращается клиенту</li>
 *   <li>4. Клиент сохраняет токен (фронт кладёт в cookies) и отправляет его в заголовке auth-token
 *       при каждом запросе</li>
 *   <li>5. AuthTokenFilter перехватывает запросы, извлекает токен и вызывает authenticateByToken()</li>
 *   <li>6. Если токен валидный — Spring Security помещает Authentication в контекст,
 *       контроллер получает текущего пользователя через @AuthenticationPrincipal</li>
 *   <li>7. При выходе — POST /logout помечает токен как revoked, дальше он не принимается</li>
 * </ul>
 */
public class AuthServiceImpl implements AuthService {

    private static final int TOKEN_VALIDITY_HOURS = 24;

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                          AuthTokenRepository authTokenRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public String login(LoginRequest request) {
        // 1. Находим пользователя по логину
        UserEntity user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new RuntimeException("Bad credentials"));

        // 2. Проверяем пароль. BCrypt хранит хэш; passwordEncoder.matches сравнивает
        //    введённый пароль с хэшем из БД (с учётом соли — BCrypt делает это сам)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Bad credentials");
        }

        // 3. Генерируем уникальный токен
        String token = UUID.randomUUID().toString();

        // 4. Создаём запись в auth_tokens
        AuthTokenEntity authToken = new AuthTokenEntity();
        authToken.setToken(token);
        authToken.setUser(user);
        authToken.setCreatedAt(Instant.now());
        authToken.setExpiresAt(Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS));
        authToken.setRevoked(false);
        authTokenRepository.save(authToken);

        return token;
    }

    @Override
    @Transactional
    public void logout(String token) {
        // Токен может прийти с префиксом "Bearer " от фронтенда — обрезаем
        String cleanToken = stripBearerPrefix(token);

        authTokenRepository.findByTokenAndRevokedFalse(cleanToken)
                .ifPresent(authToken -> {
                    authToken.setRevoked(true);
                    authTokenRepository.save(authToken);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication authenticateByToken(String token) {
        String cleanToken = stripBearerPrefix(token);
        if (cleanToken.isBlank()) {
            return null;
        }

        Optional<AuthTokenEntity> opt = authTokenRepository.findByTokenAndRevokedFalse(cleanToken);
        if (opt.isEmpty()) {
            return null; // токен не найден или отозван
        }

        AuthTokenEntity authToken = opt.get();

        // Проверка срока действия (если задан)
        if (authToken.getExpiresAt() != null && Instant.now().isAfter(authToken.getExpiresAt())) {
            return null; // токен просрочен
        }

        UserEntity user = authToken.getUser();

        // Создаём объект, который Spring Security понимает как "аутентифицированный пользователь"
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                user.getLogin(),
                user.getPasswordHash()
        );

        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    /**
     * Фронтенд часто отправляет токен в формате "Bearer &lt;token&gt;".
     * Нам нужна только часть после "Bearer ".
     */
    private String stripBearerPrefix(String token) {
        if (token == null) {
            return "";
        }
        return token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
    }
}
