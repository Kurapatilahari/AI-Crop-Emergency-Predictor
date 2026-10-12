package com.agri.cropemergency.controller;

import com.agri.cropemergency.dto.AuthResponse;
import com.agri.cropemergency.dto.LoginRequest;
import com.agri.cropemergency.dto.RegisterRequest;
import com.agri.cropemergency.dto.UserDto;
import com.agri.cropemergency.model.Role;
import com.agri.cropemergency.model.User;
import com.agri.cropemergency.repository.UserRepository;
import com.agri.cropemergency.security.JwtService;
import com.agri.cropemergency.service.FieldService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Set<String> LANGS = Set.of("en", "hi", "te");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final FieldService fieldService;

    public AuthController(UserRepository users, PasswordEncoder encoder, AuthenticationManager authManager,
                          JwtService jwt, FieldService fieldService) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.fieldService = fieldService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(req.password()));
        u.setPhone(req.phone());
        u.setLanguage(req.language() != null && LANGS.contains(req.language()) ? req.language() : "en");
        u.setRole(Role.FARMER);
        users.save(u);
        String token = jwt.generateToken(u.getEmail(), u.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, UserDto.from(u)));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User u = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generateToken(u.getEmail(), u.getRole().name()), UserDto.from(u));
    }

    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        return UserDto.from(fieldService.currentUser(auth));
    }
}
