package com.fairshare.auth;

import com.fairshare.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.encoder = encoder; this.jwt = jwt; }
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("Email is already registered");
        User user = users.save(new User(email, encoder.encode(request.password()), request.fullName().trim()));
        return response(user);
    }
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new UnauthorizedException("Invalid email or password");
        return response(user);
    }
    public AuthDtos.UserResponse me(User user) { return dto(user); }
    private AuthDtos.AuthResponse response(User u) { return new AuthDtos.AuthResponse(jwt.generate(u.getId(), u.getEmail()), dto(u)); }
    private AuthDtos.UserResponse dto(User u) { return new AuthDtos.UserResponse(u.getId(), u.getEmail(), u.getFullName()); }
}
