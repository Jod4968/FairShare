package com.fairshare.config;

import com.fairshare.auth.*;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(
            x -> x.getField(), x -> x.getDefaultMessage() == null ? "Invalid value" : x.getDefaultMessage(), (a,b) -> a));
        return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "fields", errors));
    }
    @ExceptionHandler(ConflictException.class) ResponseEntity<?> conflict(ConflictException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(UnauthorizedException.class) ResponseEntity<?> unauthorized(UnauthorizedException e) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(NotFoundException.class) ResponseEntity<?> notFound(NotFoundException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(InvalidRequestException.class) ResponseEntity<?> invalidRequest(InvalidRequestException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
}
