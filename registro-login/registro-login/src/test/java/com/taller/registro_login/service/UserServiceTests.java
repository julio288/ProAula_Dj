package com.taller.registro_login.service;

import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.FreightRequestRepository;
import com.taller.registro_login.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class UserServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final FreightRequestRepository freightRequestRepository = mock(FreightRequestRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = new UserService(
            userRepository, freightRequestRepository, passwordEncoder, "admin2027");

    @Test
    void publicRegistrationAlwaysAssignsUserRoleAndEncodesPassword() {
        User user = user("  Regular@Example.Test  ", "plain-password");
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.register(user);

        assertThat(saved.getEmail()).isEqualTo("regular@example.test");
        assertThat(saved.getRole()).isEqualTo(User.ROLE_USER);
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).existsByEmail("regular@example.test");
    }

    @Test
    void adminRegistrationAssignsAdminRoleAndEncodesPassword() {
        User user = user("admin@example.test", "plain-password");
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        User saved = userService.registerAdmin(user, "admin2027");

        assertThat(saved.getRole()).isEqualTo(User.ROLE_ADMIN);
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void publicAdminRegistrationRequiresTheConfiguredCode() {
        User user = user("admin@example.test", "plain-password");

        assertThatThrownBy(() -> userService.registerAdmin(user, "incorrecto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El código de administrador no es válido.");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void adminRegistrationRejectsRequestsWhenNoCodeIsConfigured() {
        UserService serviceWithoutCode = new UserService(
                userRepository, freightRequestRepository, passwordEncoder, "");
        User user = user("admin@example.test", "plain-password");

        assertThatThrownBy(() -> serviceWithoutCode.registerAdmin(user, "any-code"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El código de administrador no es válido.");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void createsCustomerAccountForFirstVerifiedGoogleLogin() {
        when(userRepository.findByEmail("google@example.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("unusable-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        User account = userService.findOrCreateGoogleUser(
                " Google@Example.Test ", "  Ada   Lovelace  ");

        assertThat(account.getId()).isEqualTo(42L);
        assertThat(account.getEmail()).isEqualTo("google@example.test");
        assertThat(account.getFirstName()).isEqualTo("Ada");
        assertThat(account.getLastName()).isEqualTo("Lovelace");
        assertThat(account.getRole()).isEqualTo(User.ROLE_USER);
        assertThat(account.getPassword()).isEqualTo("unusable-encoded-password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void googleLoginReusesExistingAccountWithoutChangingItsRoleOrPassword() {
        User existing = user("owner@example.test", "existing-encoded-password");
        existing.setId(8L);
        existing.setRole(User.ROLE_ADMIN);
        when(userRepository.findByEmail("owner@example.test")).thenReturn(Optional.of(existing));

        User account = userService.findOrCreateGoogleUser(
                "OWNER@example.test", "Nombre distinto");

        assertThat(account).isSameAs(existing);
        assertThat(account.getRole()).isEqualTo(User.ROLE_ADMIN);
        assertThat(account.getPassword()).isEqualTo("existing-encoded-password");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void refusesGoogleAccountsWithoutAValidEmail() {
        assertThatThrownBy(() -> userService.findOrCreateGoogleUser(" ", "Cuenta sin correo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo válido");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfileChangesOnlyContactDetails() {
        User existing = user("before@example.test", "encoded-password");
        existing.setRole(User.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateProfile(1L, " After@Example.Test ", "Nuevo", "Nombre");

        assertThat(updated.getEmail()).isEqualTo("after@example.test");
        assertThat(updated.getFirstName()).isEqualTo("Nuevo");
        assertThat(updated.getLastName()).isEqualTo("Nombre");
        assertThat(updated.getRole()).isEqualTo(User.ROLE_ADMIN);
        assertThat(updated.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void refusesDeletingTheCurrentAdmin() {
        User admin = user("admin@example.test", "encoded-password");
        admin.setId(1L);
        admin.setRole(User.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.deleteUser(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propia cuenta");
        verify(userRepository, never()).delete(admin);
    }

    @Test
    void refusesDeletingTheLastAdmin() {
        User admin = user("other-admin@example.test", "encoded-password");
        admin.setId(2L);
        admin.setRole(User.ROLE_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(User.ROLE_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("último administrador");
        verify(userRepository, never()).delete(admin);
    }

    @Test
    void deletesCustomerRequestsWhenAnAccountIsRemoved() {
        User customer = user("customer@example.test", "encoded-password");
        customer.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));

        userService.deleteUser(2L, 1L);

        verify(freightRequestRepository).deleteByCustomerId(2L);
        verify(userRepository).delete(customer);
    }

    private User user(String email, String password) {
        return new User(email, password, "Nombre", "Apellido");
    }
}