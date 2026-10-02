package com.fairshare.auth;

import com.fairshare.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService; private final UserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) { this.jwtService = jwtService; this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                users.findByEmailIgnoreCase(claims.getSubject()).ifPresent(user ->
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES)));
            } catch (RuntimeException ignored) { /* invalid tokens remain unauthenticated */ }
        }
        chain.doFilter(request, response);
    }
}
