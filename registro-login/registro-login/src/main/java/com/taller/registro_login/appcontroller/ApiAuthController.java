package com.taller.registro_login.appcontroller;

import com.taller.registro_login.model.User;
import com.taller.registro_login.service.TokenService;
import com.taller.registro_login.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public ApiAuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            TokenService tokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegistrationRequest request) {
        userService.register(new User(request.username(), request.password(), "API", "Usuario"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Usuario registrado."));
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> token(@Valid @RequestBody Credentials request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password()));
        return ResponseEntity.ok(Map.of(
                "access_token", tokenService.issue(username),
                "token_type", "Bearer"));
    }

    public record RegistrationRequest(
            @NotBlank @Email @Size(max = 100) String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record Credentials(
            @NotBlank @Email @Size(max = 100) String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }
}
