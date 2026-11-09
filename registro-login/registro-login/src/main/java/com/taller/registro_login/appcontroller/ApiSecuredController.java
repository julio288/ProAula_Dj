package com.taller.registro_login.appcontroller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiSecuredController {

    @GetMapping("/api/secured")
    public Map<String, String> secured(Authentication authentication) {
        return Map.of(
                "message", "Acceso autorizado",
                "username", authentication.getName());
    }
}
