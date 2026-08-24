package com.sentinelgate.service;

import com.sentinelgate.domain.Role;
import com.sentinelgate.domain.User;
import com.sentinelgate.domain.enums.RoleType;
import com.sentinelgate.dto.JwtAuthResponse;
import com.sentinelgate.dto.LoginRequest;
import com.sentinelgate.dto.RegisterRequest;
import com.sentinelgate.dto.UserProfileResponse;
import com.sentinelgate.repository.RoleRepository;
import com.sentinelgate.repository.UserRepository;
import com.sentinelgate.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private Role adminRole;
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().id(1L).name(RoleType.ADMIN).description("Admin").build();
        adminUser = User.builder()
                .id(1L)
                .username("secadmin")
                .email("secadmin@sentinelgate.io")
                .passwordHash("hashed_password")
                .enabled(true)
                .role(adminRole)
                .build();
    }

    @Test
    @DisplayName("Should successfully register new user with BCrypt password")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@sentinelgate.io")
                .password("Password123!")
                .role(RoleType.DEVELOPER)
                .build();

        Role devRole = Role.builder().id(2L).name(RoleType.DEVELOPER).build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@sentinelgate.io")).thenReturn(false);
        when(roleRepository.findByName(RoleType.DEVELOPER)).thenReturn(Optional.of(devRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserProfileResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("DEVELOPER", response.getRole());
        verify(passwordEncoder).encode("Password123!");
    }

    @Test
    @DisplayName("Should reject registration when username exists")
    void register_DuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existing")
                .email("new@sentinelgate.io")
                .password("Password123!")
                .build();

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate and return JWT token on valid credentials")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .username("secadmin")
                .password("secret_pass")
                .build();

        when(userRepository.findByUsername("secadmin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("secret_pass", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken("secadmin", "ADMIN")).thenReturn("mocked_jwt_access");
        when(jwtTokenProvider.generateRefreshToken("secadmin")).thenReturn("mocked_jwt_refresh");

        JwtAuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked_jwt_access", response.getAccessToken());
        assertEquals("mocked_jwt_refresh", response.getRefreshToken());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    @DisplayName("Should reject login on invalid password")
    void login_InvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .username("secadmin")
                .password("wrong_pass")
                .build();

        when(userRepository.findByUsername("secadmin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrong_pass", "hashed_password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
}
