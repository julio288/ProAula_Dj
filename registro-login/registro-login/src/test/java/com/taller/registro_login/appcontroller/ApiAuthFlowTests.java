package com.taller.registro_login.appcontroller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiAuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void registersPersistsAuthenticatesAndUsesBearerToken() throws Exception {
        String email = "jwt-" + UUID.randomUUID() + "@example.test";
        String password = "SecurePass123!";
        String registrationBody = objectMapper.writeValueAsString(
                new ApiAuthController.RegistrationRequest(email, password));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Usuario registrado."));

        User registered = userRepository.findByEmail(email).orElseThrow();
        assertThat(passwordEncoder.matches(password, registered.getPassword())).isTrue();
        assertThat(registered.getRole()).isEqualTo(User.ROLE_USER);

        String tokenBody = objectMapper.writeValueAsString(
                new ApiAuthController.Credentials(email, password));
        MvcResult tokenResponse = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(tokenResponse.getResponse().getContentAsString());
        String token = responseJson.path("access_token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/secured")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.message").value("Acceso autorizado"));
    }

    @Test
    void rejectsMissingTokenInvalidCredentialsAndDuplicateRegistration() throws Exception {
        String email = "jwt-" + UUID.randomUUID() + "@example.test";
        String registrationBody = objectMapper.writeValueAsString(
                new ApiAuthController.RegistrationRequest(email, "SecurePass123!"));

        mockMvc.perform(get("/api/secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Autenticación requerida."));

        mockMvc.perform(get("/api/secured").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(status().isConflict());

        String invalidCredentials = objectMapper.writeValueAsString(
                new ApiAuthController.Credentials(email, "WrongPass123!"));
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCredentials))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales no válidas."));
    }

    @Test
    void rejectsMalformedRegistrationWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void rejectsTokenWhenItsSignatureIsChanged() throws Exception {
        String email = "jwt-" + UUID.randomUUID() + "@example.test";
        String password = "SecurePass123!";
        String registrationBody = objectMapper.writeValueAsString(
                new ApiAuthController.RegistrationRequest(email, password));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(status().isCreated());

        String credentials = objectMapper.writeValueAsString(
                new ApiAuthController.Credentials(email, password));
        MvcResult tokenResponse = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(tokenResponse.getResponse().getContentAsString())
                .path("access_token").asText();

        String[] parts = token.split("\\.", -1);
        assertThat(parts).hasSize(3);
        parts[2] = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);

        mockMvc.perform(get("/api/secured")
                        .header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        Instant now = Instant.now();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer("registro-login")
                        .issuedAt(now.minusSeconds(120))
                        .expiresAt(now.minusSeconds(60))
                        .subject("expired@example.test")
                        .build()))
                .getTokenValue();

        mockMvc.perform(get("/api/secured").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
