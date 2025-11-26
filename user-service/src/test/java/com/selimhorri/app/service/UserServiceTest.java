package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.User;
import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void findAll_returnsAllUsersFromRepository() {
        User user1 = buildUser(1, "John", "Doe", "john");
        User user2 = buildUser(2, "Jane", "Doe", "jane");

        given(userRepository.findAll()).willReturn(Arrays.asList(user1, user2));

        List<UserDto> result = userService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(UserDto::getUserId)
                .containsExactlyInAnyOrder(1, 2);

        verify(userRepository).findAll();
    }

    @Test
    void findById_returnsUserWhenExists() {
        Integer id = 1;
        User user = buildUser(id, "John", "Doe", "john");

        given(userRepository.findById(id)).willReturn(Optional.of(user));

        UserDto result = userService.findById(id);

        assertThat(result.getUserId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo("John");

        verify(userRepository).findById(id);
    }

    @Test
    void findById_throwsExceptionWhenUserDoesNotExist() {
        Integer id = 99;
        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> userService.findById(id));
        verify(userRepository).findById(id);
    }

    @Test
    void save_persistsAndReturnsMappedUser() {
        UserDto toSave = buildUserDto(null, "John", "Doe", "john");

        User savedEntity = buildUser(1, "John", "Doe", "john");

        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willReturn(savedEntity);

        UserDto result = userService.save(toSave);

        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getCredentialDto().getUsername()).isEqualTo("john");

        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void update_withDto_updatesAndReturnsMappedUser() {
        UserDto toUpdate = buildUserDto(1, "John", "Updated", "john");

        User updatedEntity = buildUser(1, "John", "Updated", "john");

        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willReturn(updatedEntity);

        UserDto result = userService.update(toUpdate);

        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getLastName()).isEqualTo("Updated");

        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void update_withId_usesExistingUserAndReturnsMappedUser() {
        Integer id = 1;
        User existing = buildUser(id, "John", "Doe", "john");

        given(userRepository.findById(id)).willReturn(Optional.of(existing));
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        UserDto anyDto = buildUserDto(id, "Ignored", "Ignored", "ignored");

        UserDto result = userService.update(id, anyDto);

        assertThat(result.getUserId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo("John");

        verify(userRepository).findById(id);
        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void update_withId_throwsExceptionWhenUserDoesNotExist() {
        Integer id = 99;

        given(userRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class,
                () -> userService.update(id, buildUserDto(id, "X", "Y", "user")));

        verify(userRepository).findById(id);
    }

    @Test
    void deleteById_deletesUser() {
        Integer id = 1;

        userService.deleteById(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void findByUsername_returnsUserWhenExists() {
        String username = "john";
        User user = buildUser(1, "John", "Doe", username);

        given(userRepository.findByCredentialUsername(username)).willReturn(Optional.of(user));

        UserDto result = userService.findByUsername(username);

        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getCredentialDto().getUsername()).isEqualTo(username);

        verify(userRepository).findByCredentialUsername(username);
    }

    @Test
    void findByUsername_throwsExceptionWhenUserDoesNotExist() {
        String username = "unknown";

        given(userRepository.findByCredentialUsername(username)).willReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> userService.findByUsername(username));
        verify(userRepository).findByCredentialUsername(username);
    }

    private User buildUser(Integer id, String first, String last, String username) {
        Credential credential = Credential.builder()
                .credentialId(1)
                .username(username)
                .password("secret")
                .build();

        return User.builder()
                .userId(id)
                .firstName(first)
                .lastName(last)
                .email(first.toLowerCase() + "@example.com")
                .credential(credential)
                .build();
    }

    private UserDto buildUserDto(Integer id, String first, String last, String username) {
        CredentialDto credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username(username)
                .password("secret")
                .build();

        return UserDto.builder()
                .userId(id)
                .firstName(first)
                .lastName(last)
                .email(first.toLowerCase() + "@example.com")
                .credentialDto(credentialDto)
                .build();
    }
}


