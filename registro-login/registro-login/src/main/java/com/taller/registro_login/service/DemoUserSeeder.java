package com.taller.registro_login.service;

import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"default", "dev"})
@Order(1)
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final String userEmail;
    private final String userPassword;
    private final String adminEmail;
    private final String adminPassword;

    public DemoUserSeeder(
            UserRepository userRepository,
            UserService userService,
            @Value("${app.demo.user.email:demo.usuario@universidad.test}") String userEmail,
            @Value("${APP_DEMO_USER_PASSWORD:Demo123!}") String userPassword,
            @Value("${app.demo.admin.email:demo.admin@universidad.test}") String adminEmail,
            @Value("${APP_DEMO_ADMIN_PASSWORD:Admin123!}") String adminPassword) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existingUsers = userRepository.count();
        if (existingUsers > 0) {
            LOGGER.info("Demo accounts skipped because {} users already exist in the database.", existingUsers);
            return;
        }

        ensureUserAccount();
        ensureAdminAccount();
    }

    private void ensureUserAccount() {
        if (userRepository.findByEmail(userEmail).isPresent()) {
            return;
        }
        validatePassword(userPassword, "demo user");
        userService.register(new User(userEmail, userPassword, "Demo", "Usuario"));
        LOGGER.info("Created demo user account: {}", userEmail);
    }

    private void ensureAdminAccount() {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        validatePassword(adminPassword, "demo admin");
        userService.registerSeededAdmin(new User(adminEmail, adminPassword, "Demo", "Administrador"));
        LOGGER.info("Created demo admin account: {}", adminEmail);
    }

    private void validatePassword(String password, String label) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("The password for the " + label + " cannot be empty.");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("The password for the " + label + " must be between 8 and 72 characters.");
        }
    }
}
