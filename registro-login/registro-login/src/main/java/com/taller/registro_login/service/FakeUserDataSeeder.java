package com.taller.registro_login.service;

import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.UserRepository;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
@Profile("prod")
@Order(1)
public class FakeUserDataSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeUserDataSeeder.class);
    private static final String TARGET_DATABASE = "universidad";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserService userService;
    private final DataSource dataSource;
    private final int userCount;
    private final String testPassword;
    private final String testUserEmail;
    private final String testAdminEmail;

    public FakeUserDataSeeder(
            UserRepository userRepository,
            UserService userService,
            DataSource dataSource,
            @Value("${app.seed.users.count:20}") int userCount,
            @Value("${APP_SEED_TEST_PASSWORD:}") String testPassword,
            @Value("${app.seed.test-user.email:prueba.usuario@universidad.test}") String testUserEmail,
            @Value("${app.seed.test-admin.email:prueba.admin@universidad.test}") String testAdminEmail) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.dataSource = dataSource;
        this.userCount = userCount;
        this.testPassword = testPassword;
        this.testUserEmail = testUserEmail;
        this.testAdminEmail = testAdminEmail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userCount <= 0) {
            throw new IllegalStateException("app.seed.users.count must be greater than zero.");
        }
        if (!isTargetDatabase()) {
            return;
        }

        Faker faker = new Faker(Locale.forLanguageTag("es"));
        long existingUsers = userRepository.count();
        if (existingUsers == 0) {
            seedRandomUsers(faker);
        } else {
            LOGGER.info("Skipping Faker users; {} users already exist in the universidad database.",
                    existingUsers);
        }

        ensureTestAccount(faker, testUserEmail, User.ROLE_USER);
        ensureTestAccount(faker, testAdminEmail, User.ROLE_ADMIN);
    }

    private void seedRandomUsers(Faker faker) {
        Set<String> emails = new HashSet<>();
        for (int index = 0; index < userCount; index++) {
            String email;
            do {
                email = faker.internet().safeEmailAddress();
            } while (!emails.add(email));

            userService.register(new User(
                    email,
                    generatePassword(),
                    faker.name().firstName(),
                    faker.name().lastName()));
        }
        LOGGER.info("Saved {} Faker users in the universidad database.", userCount);
    }

    private void ensureTestAccount(Faker faker, String email, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            LOGGER.info("Faker test account available: role={}, email={}", role, email);
            return;
        }

        if (testPassword.isBlank()) {
            LOGGER.warn("Cannot create Faker test account role={}; set APP_SEED_TEST_PASSWORD.", role);
            return;
        }
        if (testPassword.length() < 8 || testPassword.length() > 72) {
            throw new IllegalStateException("APP_SEED_TEST_PASSWORD must contain between 8 and 72 characters.");
        }

        User user = new User(email, testPassword, faker.name().firstName(), faker.name().lastName());
        if (User.ROLE_ADMIN.equals(role)) {
            userService.registerSeededAdmin(user);
        } else {
            userService.register(user);
        }
        LOGGER.info("Created Faker test account: role={}, email={}", role, email);
    }

    private boolean isTargetDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            String catalog = connection.getCatalog();
            if (TARGET_DATABASE.equalsIgnoreCase(catalog)) {
                return true;
            }
            throw new IllegalStateException(
                    "Faker seeding requires the 'universidad' database, but connected to '" + catalog + "'.");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not verify the database before Faker seeding.", exception);
        }
    }

    private String generatePassword() {
        byte[] passwordBytes = new byte[24];
        SECURE_RANDOM.nextBytes(passwordBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(passwordBytes);
    }
}