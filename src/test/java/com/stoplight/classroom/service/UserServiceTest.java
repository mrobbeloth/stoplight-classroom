package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.CreateUserRequest;
import com.stoplight.classroom.dto.UpdateUserRequest;
import com.stoplight.classroom.dto.UserResponse;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUser_success() {
        var request = new CreateUserRequest("newteacher", "password1", Role.TEACHER);
        when(userRepository.existsByUsername("newteacher")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.createUser(request);

        assertEquals("newteacher", response.username());
        assertEquals(Role.TEACHER, response.role());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed", captor.getValue().getPasswordHash());
    }

    @Test
    void createUser_duplicateUsername_throws() {
        var request = new CreateUserRequest("existing", "password1", Role.TEACHER);
        when(userRepository.existsByUsername("existing")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
    }

    @Test
    void listUsers_returnsMappedList() {
        User u = new User("t1", "h", Role.TEACHER);
        when(userRepository.findAll()).thenReturn(List.of(u));

        List<UserResponse> result = userService.listUsers();

        assertEquals(1, result.size());
        assertEquals("t1", result.get(0).username());
    }

    @Test
    void getUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUser(99L));
    }

    @Test
    void updateUser_changesPassword() {
        User user = new User("t1", "old", Role.TEACHER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass1")).thenReturn("newhash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser(1L, new UpdateUserRequest("newpass1"));

        assertEquals("newhash", user.getPasswordHash());
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
    }

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }
}
