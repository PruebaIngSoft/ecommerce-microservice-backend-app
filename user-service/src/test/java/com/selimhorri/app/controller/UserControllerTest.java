package com.selimhorri.app.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.service.UserService;

@WebMvcTest(controllers = com.selimhorri.app.resource.UserResource.class)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void getAllUsers_returnsCollectionResponse() throws Exception {
        UserDto u1 = UserDto.builder().userId(1).firstName("John").build();
        UserDto u2 = UserDto.builder().userId(2).firstName("Jane").build();
        List<UserDto> list = Arrays.asList(u1, u2);

        given(userService.findAll()).willReturn(list);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[1].userId").value(2));
    }

    @Test
    void getUserById_returnsUser() throws Exception {
        UserDto user = UserDto.builder()
                .userId(1)
                .firstName("John")
                .build();

        given(userService.findById(1)).willReturn(user);

        mockMvc.perform(get("/api/users/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getUserById_whenNotFound_returnsBadRequestWithErrorBody() throws Exception {
        given(userService.findById(99)).willThrow(new UserObjectNotFoundException("User with id: 99 not found"));

        mockMvc.perform(get("/api/users/{id}", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void saveUser_returnsSavedUser() throws Exception {
        CredentialDto credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("john")
                .build();

        UserDto request = UserDto.builder()
                .firstName("John")
                .lastName("Doe")
                .credentialDto(credentialDto)
                .build();

        UserDto response = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .credentialDto(credentialDto)
                .build();

        given(userService.save(request)).willReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void updateUser_returnsUpdatedUser() throws Exception {
        UserDto request = UserDto.builder()
                .userId(1)
                .firstName("Updated")
                .build();

        UserDto response = UserDto.builder()
                .userId(1)
                .firstName("Updated")
                .build();

        given(userService.update(request)).willReturn(response);

        mockMvc.perform(put("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void updateUserWithPathId_returnsUpdatedUser() throws Exception {
        UserDto request = UserDto.builder()
                .firstName("Updated")
                .build();

        UserDto response = UserDto.builder()
                .userId(1)
                .firstName("Updated")
                .build();

        given(userService.update(1, request)).willReturn(response);

        mockMvc.perform(put("/api/users/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void deleteUser_returnsTrue() throws Exception {
        doNothing().when(userService).deleteById(1);

        mockMvc.perform(delete("/api/users/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(userService).deleteById(1);
    }

    @Test
    void getUserByUsername_returnsUser() throws Exception {
        String username = "john";
        UserDto user = UserDto.builder()
                .userId(1)
                .firstName("John")
                .credentialDto(CredentialDto.builder().username(username).build())
                .build();

        given(userService.findByUsername(username)).willReturn(user);

        mockMvc.perform(get("/api/users/username/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.credential.username").value(username));
    }
}


