package com.taller.registro_login;

import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.FreightRequestRepository;
import com.taller.registro_login.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityLoginRedirectTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FreightRequestRepository freightRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        freightRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminShouldBeRedirectedToAdminPanelAfterLogin() throws Exception {
        User admin = new User(
                "admin@demo.com",
                passwordEncoder.encode("Password123"),
                "Admin",
                "Sistema"
        );
        admin.setRole(User.ROLE_ADMIN);
        userRepository.save(admin);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "admin@demo.com")
                        .param("password", "Password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void regularUserShouldBeRedirectedToDashboardAfterLogin() throws Exception {
        User user = new User(
                "usuario@demo.com",
                passwordEncoder.encode("Password123"),
                "Usuario",
                "Demo"
        );
        userRepository.save(user);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "usuario@demo.com")
                        .param("password", "Password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
