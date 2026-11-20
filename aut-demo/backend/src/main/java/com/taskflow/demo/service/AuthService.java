package com.taskflow.demo.service;

import com.taskflow.demo.dto.LoginRequest;
import com.taskflow.demo.dto.LoginResponse;
import com.taskflow.demo.dto.RegisterRequest;
import com.taskflow.demo.entity.User;
import com.taskflow.demo.exception.ConflictException;
import com.taskflow.demo.exception.UnauthorizedException;
import com.taskflow.demo.repository.UserRepository;
import com.taskflow.demo.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        LoginResponse.UserSummary summary =
                new LoginResponse.UserSummary(user.getId(), user.getName(), user.getEmail());
        return new LoginResponse(token, summary);
    }
}
