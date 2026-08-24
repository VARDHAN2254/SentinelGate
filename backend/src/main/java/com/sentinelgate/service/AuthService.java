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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        RoleType targetRoleType = request.getRole() != null ? request.getRole() : RoleType.VIEWER;
        Role role = roleRepository.findByName(targetRoleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + targetRoleType));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        return UserProfileResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().getName().name())
                .enabled(savedUser.getEnabled())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public JwtAuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!user.getEnabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String roleName = user.getRole().getName().name();
        String accessToken = jwtTokenProvider.generateToken(user.getUsername(), roleName);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return JwtAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtTokenProvider.getJwtExpirationMs())
                .username(user.getUsername())
                .role(roleName)
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
