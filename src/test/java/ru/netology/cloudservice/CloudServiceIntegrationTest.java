package ru.netology.cloudservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CloudServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cloudservice")
            .withUsername("cloud")
            .withPassword("cloud");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("storage.root-dir", () -> {
            try {
                return java.nio.file.Files.createTempDirectory("cloudtest").toString();
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_success_returnsAuthToken() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").exists());
    }

    @Test
    void login_badCredentials_returns400() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullFlow_loginUploadListDeleteLogout() throws Exception {
        // 1. Login
        String loginResponse = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"user\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = new ObjectMapper().readTree(loginResponse);
        String token = json.get("auth-token").asText();

        // 2. Upload file
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "hello world".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "test.txt")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        // 3. List files
        mockMvc.perform(get("/list")
                        .header("auth-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(11));

        // 4. Download
        mockMvc.perform(get("/file").param("filename", "test.txt")
                        .header("auth-token", token))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));

        // 5. Rename
        mockMvc.perform(put("/file").param("filename", "test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed.txt\"}")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        // 6. List after rename
        mockMvc.perform(get("/list").header("auth-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("renamed.txt"));

        // 7. Delete
        mockMvc.perform(delete("/file").param("filename", "renamed.txt")
                        .header("auth-token", token))
                .andExpect(status().isOk());

        // 8. List after delete (empty)
        mockMvc.perform(get("/list").header("auth-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 9. Logout
        mockMvc.perform(post("/logout").header("auth-token", token))
                .andExpect(status().isOk());

        // 10. Token no longer valid
        mockMvc.perform(get("/list").header("auth-token", token))
                .andExpect(status().isUnauthorized());
    }
}
