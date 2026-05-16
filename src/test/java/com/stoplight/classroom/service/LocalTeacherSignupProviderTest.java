package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.TeacherSignupRequest;
import com.stoplight.classroom.dto.TeacherSignupResponse;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.model.UserStatus;
import com.stoplight.classroom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalTeacherSignupProviderTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private LocalTeacherSignupProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalTeacherSignupProvider(userRepository, passwordEncoder, ".edu");
    }

    @Test
    void requestSignup_validEduEmail_createsPendingTeacher() {
        var req = new TeacherSignupRequest("prof_smith", "smith@example.edu", "password1");
        when(userRepository.existsByUsername("prof_smith")).thenReturn(false);
        when(userRepository.existsByEmail("smith@example.edu")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherSignupResponse response = provider.requestSignup(req);

        assertEquals("prof_smith", response.username());
        assertEquals("smith@example.edu", response.email());
        assertEquals(UserStatus.PENDING, response.status());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(Role.TEACHER, saved.getRole());
        assertEquals(UserStatus.PENDING, saved.getStatus());
        assertEquals("hashed", saved.getPasswordHash());
    }

    @Test
    void requestSignup_normalizesEmailToLowercase() {
        var req = new TeacherSignupRequest("prof_smith", "Smith@Example.EDU", "password1");
        when(userRepository.existsByUsername("prof_smith")).thenReturn(false);
        when(userRepository.existsByEmail("smith@example.edu")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherSignupResponse response = provider.requestSignup(req);

        assertEquals("smith@example.edu", response.email());
    }

    @Test
    void requestSignup_nonEduEmail_throws() {
        var req = new TeacherSignupRequest("prof_smith", "smith@example.com", "password1");
        var ex = assertThrows(IllegalArgumentException.class, () -> provider.requestSignup(req));
        assertTrue(ex.getMessage().contains(".edu"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void requestSignup_duplicateUsername_throws() {
        var req = new TeacherSignupRequest("prof_smith", "smith@example.edu", "password1");
        when(userRepository.existsByUsername("prof_smith")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> provider.requestSignup(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void requestSignup_duplicateEmail_throws() {
        var req = new TeacherSignupRequest("prof_smith", "smith@example.edu", "password1");
        when(userRepository.existsByUsername("prof_smith")).thenReturn(false);
        when(userRepository.existsByEmail("smith@example.edu")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> provider.requestSignup(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void requestSignup_multipleAllowedSuffixes_acceptsAny() {
        provider = new LocalTeacherSignupProvider(userRepository, passwordEncoder, ".edu, .ac.uk");
        var req = new TeacherSignupRequest("prof_smith", "smith@cam.ac.uk", "password1");
        when(userRepository.existsByUsername("prof_smith")).thenReturn(false);
        when(userRepository.existsByEmail("smith@cam.ac.uk")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> provider.requestSignup(req));
    }

    @Test
    void approve_pendingTeacher_setsApproved() throws Exception {
        User user = pendingTeacher(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherSignupResponse response = provider.approve(7L);

        assertEquals(UserStatus.APPROVED, response.status());
        assertEquals(UserStatus.APPROVED, user.getStatus());
    }

    @Test
    void approve_alreadyApproved_throws() throws Exception {
        User user = pendingTeacher(7L);
        user.setStatus(UserStatus.APPROVED);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> provider.approve(7L));
    }

    @Test
    void approve_unknownId_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> provider.approve(99L));
    }

    @Test
    void approve_adminUser_throws() throws Exception {
        User admin = pendingTeacher(7L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        assertThrows(IllegalArgumentException.class, () -> provider.approve(7L));
    }

    @Test
    void reject_pendingTeacher_setsRejected() throws Exception {
        User user = pendingTeacher(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherSignupResponse response = provider.reject(7L);

        assertEquals(UserStatus.REJECTED, response.status());
    }

    @Test
    void listByStatus_filtersOutNonTeachers() throws Exception {
        User teacher = pendingTeacher(1L);
        User admin = pendingTeacher(2L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findByStatus(UserStatus.PENDING)).thenReturn(List.of(teacher, admin));

        List<TeacherSignupResponse> result = provider.listByStatus(UserStatus.PENDING);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    private User pendingTeacher(long id) throws Exception {
        User user = new User("prof_smith", "hash", Role.TEACHER);
        user.setEmail("smith@example.edu");
        user.setStatus(UserStatus.PENDING);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}
