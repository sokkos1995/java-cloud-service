package ru.netology.cloudservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservice.dto.FileResponse;
import ru.netology.cloudservice.entity.FileEntity;
import ru.netology.cloudservice.entity.UserEntity;
import ru.netology.cloudservice.repository.FileRepository;
import ru.netology.cloudservice.repository.UserRepository;
import ru.netology.cloudservice.storage.FileStorageService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileServiceImpl fileService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setLogin("user");
    }

    @Test
    void listFiles_returnsFiles() {
        FileEntity entity = new FileEntity();
        entity.setFilename("test.pdf");
        entity.setSize(1024);
        entity.setUploadTime(Instant.now());

        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(fileRepository.findAllByUser(testUser)).thenReturn(List.of(entity));

        List<FileResponse> result = fileService.listFiles("user", null);

        assertEquals(1, result.size());
        assertEquals("test.pdf", result.get(0).getFilename());
        assertEquals(1024, result.get(0).getSize());
    }

    @Test
    void listFiles_withLimit_respectsLimit() {
        FileEntity e1 = new FileEntity();
        e1.setFilename("a.txt");
        e1.setSize(1);
        e1.setUploadTime(Instant.now());
        FileEntity e2 = new FileEntity();
        e2.setFilename("b.txt");
        e2.setSize(2);
        e2.setUploadTime(Instant.now());

        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(fileRepository.findAllByUser(testUser)).thenReturn(List.of(e1, e2));

        List<FileResponse> result = fileService.listFiles("user", 1);

        assertEquals(1, result.size());
    }

    @Test
    void uploadFile_savesFile() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getSize()).thenReturn(100L);
        when(mockFile.getContentType()).thenReturn("text/plain");
        when(mockFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("content".getBytes()));

        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "doc.txt")).thenReturn(Optional.empty());
        when(fileStorageService.save(eq("user"), eq("doc.txt"), any())).thenReturn("uuid_doc.txt");

        fileService.uploadFile("user", "doc.txt", mockFile);

        verify(fileRepository).save(argThat(e -> e.getFilename().equals("doc.txt") && e.getStorageFilename().equals("uuid_doc.txt")));
    }

    @Test
    void deleteFile_removesFile() throws IOException {
        FileEntity entity = new FileEntity();
        entity.setFilename("old.txt");
        entity.setStorageFilename("uuid_old.txt");

        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(Optional.of(entity));

        fileService.deleteFile("user", "old.txt");

        verify(fileStorageService).delete("user", "uuid_old.txt");
        verify(fileRepository).delete(entity);
    }

    @Test
    void renameFile_updatesFilename() {
        FileEntity entity = new FileEntity();
        entity.setFilename("old.txt");
        entity.setStorageFilename("uuid_old.txt");

        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(Optional.of(entity));
        when(fileRepository.findByUserAndFilename(testUser, "new.txt")).thenReturn(Optional.empty());
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fileService.renameFile("user", "old.txt", "new.txt");

        verify(fileRepository).save(argThat(e -> e.getFilename().equals("new.txt")));
    }

    @Test
    void renameFile_newNameExists_throwsException() {
        FileEntity entity = new FileEntity();
        entity.setFilename("old.txt");
        FileEntity existing = new FileEntity();
        existing.setFilename("new.txt");

        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(Optional.of(entity));
        when(fileRepository.findByUserAndFilename(testUser, "new.txt")).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class, () -> fileService.renameFile("user", "old.txt", "new.txt"));
    }

    @Test
    void deleteFile_blankFilename_throwsException() {
        assertThrows(RuntimeException.class, () -> fileService.deleteFile("user", "  "));
    }

    @Test
    void renameFile_blankNewName_throwsException() {
        assertThrows(RuntimeException.class, () -> fileService.renameFile("user", "a.txt", ""));
    }
}
