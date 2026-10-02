package com.fairshare.auth;

import com.fairshare.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock JwtService jwt;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test void registersWithHashedPasswordAndToken() {
        AuthService service = new AuthService(users, encoder, jwt);
        when(users.existsByEmailIgnoreCase("a@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwt.generate(any(), eq("a@example.com"))).thenReturn("token");
        AuthDtos.AuthResponse result = service.register(new AuthDtos.RegisterRequest("A@EXAMPLE.COM", "password123", "Ada"));
        assertEquals("token", result.token());
        assertEquals("a@example.com", result.user().email());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertTrue(encoder.matches("password123", captor.getValue().getPasswordHash()));
    }

    @Test void rejectsIncorrectPassword() {
        AuthService service = new AuthService(users, encoder, jwt);
        User user = new User("a@example.com", encoder.encode("correct123"), "Ada");
        when(users.findByEmailIgnoreCase("a@example.com")).thenReturn(java.util.Optional.of(user));
        assertThrows(UnauthorizedException.class, () -> service.login(new AuthDtos.LoginRequest("a@example.com", "wrong123")));
    }
}
