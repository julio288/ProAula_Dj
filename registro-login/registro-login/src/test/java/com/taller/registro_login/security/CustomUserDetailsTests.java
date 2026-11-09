package com.taller.registro_login.security;

import com.taller.registro_login.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTests {

    @Test
    void exposesThePersistedRoleAsAnAuthority() {
        User admin = new User("admin@example.test", "encoded", "Admin", "Prueba");
        admin.setRole(User.ROLE_ADMIN);

        CustomUserDetails userDetails = new CustomUserDetails(admin);

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly(User.ROLE_ADMIN);
    }

    @Test
    void defaultsExistingUsersWithoutARoleToUserAuthority() {
        User user = new User("user@example.test", "encoded", "Usuario", "Prueba");
        user.setRole(null);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly(User.ROLE_USER);
    }
}