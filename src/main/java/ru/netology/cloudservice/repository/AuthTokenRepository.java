package ru.netology.cloudservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudservice.entity.AuthTokenEntity;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthTokenEntity, Long> {

    Optional<AuthTokenEntity> findByTokenAndRevokedFalse(String token);
}
