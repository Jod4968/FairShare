package com.fairshare.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @JsonAlias("name") @NotBlank @Size(max = 120) String fullName) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserResponse user) {}
    public record UserResponse(Long id, String email, String fullName) {}
}
