package ru.netology.cloudservice.service;

import org.springframework.security.core.Authentication;
import ru.netology.cloudservice.dto.LoginRequest;

public interface AuthService {

    String login(LoginRequest request);

    void logout(String token);

    Authentication authenticateByToken(String token);
}
