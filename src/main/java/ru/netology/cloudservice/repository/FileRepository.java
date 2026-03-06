package ru.netology.cloudservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudservice.entity.FileEntity;
import ru.netology.cloudservice.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findAllByUser(UserEntity user);

    Optional<FileEntity> findByUserAndFilename(UserEntity user, String filename);
}
