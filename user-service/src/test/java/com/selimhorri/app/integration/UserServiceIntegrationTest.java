package com.selimhorri.app.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void createAndVerifyUserPersistence() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString();
        UserDto request = UserDto.builder()
                .firstName("Integration")
                .lastName("User")
                .imageUrl("https://cdn.example.com/avatar.png")
                .email("integration-" + uniqueSuffix + "@example.com")
                .phone("7000" + uniqueSuffix.substring(0, 4))
                .credentialDto(
                        CredentialDto.builder()
                                .username("integration-" + uniqueSuffix)
                                .password("Pass1234!")
                                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                                .isEnabled(true)
                                .isAccountNonExpired(true)
                                .isAccountNonLocked(true)
                                .isCredentialsNonExpired(true)
                                .build()
                )
                .build();

        String payload = objectMapper.writeValueAsString(request);

        JsonNode creationResponse = objectMapper.readTree(
                mockMvc.perform(post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.userId").isNumber())
                        .andExpect(jsonPath("$.firstName").value(request.getFirstName()))
                        .andExpect(jsonPath("$.email").value(request.getEmail()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int userId = creationResponse.get("userId").asInt();

        User persisted = userRepository.findById(userId)
                .orElseThrow(() -> new AssertionError("User should be persisted in H2"));

        assertNotNull(persisted.getUserId(), "User ID must be stored");
        assertEquals(request.getFirstName(), persisted.getFirstName());
        assertEquals(request.getEmail(), persisted.getEmail());
        assertTrue(persisted.getCredential() != null, "Credential information must be cascaded");
        assertEquals(request.getCredentialDto().getUsername(), persisted.getCredential().getUsername());
    }
}

