package com.taller.registro_login.service;

import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FakeUserDataSeederTests {

    private static final String TEST_PASSWORD = "SeedSecret-12345";
    private static final String USER_EMAIL = "prueba.usuario@universidad.test";
    private static final String ADMIN_EMAIL = "prueba.admin@universidad.test";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);

    @Test
    void generatesUsersOnlyWhenUniversidadHasNoUsers() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("universidad");
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());

        seeder(3, TEST_PASSWORD).run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService, times(4)).register(userCaptor.capture());
        List<User> generatedUsers = userCaptor.getAllValues();

        assertThat(generatedUsers).hasSize(4);
        assertThat(generatedUsers.subList(0, 3)).allSatisfy(user -> {
            assertThat(user.getEmail()).contains("@");
            assertThat(user.getPassword()).hasSize(32);
            assertThat(user.getFirstName()).isNotBlank();
            assertThat(user.getLastName()).isNotBlank();
        });
        assertThat(generatedUsers).extracting(User::getEmail).doesNotHaveDuplicates();
        assertThat(generatedUsers).extracting(User::getEmail).contains(USER_EMAIL);
        ArgumentCaptor<User> adminCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).registerSeededAdmin(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getEmail()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void createsKnownAccountsEvenWhenOtherUsersAlreadyExist() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("universidad");
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());

        seeder(3, TEST_PASSWORD).run(new DefaultApplicationArguments(new String[0]));

        verify(userService).register(org.mockito.ArgumentMatchers.argThat(user -> USER_EMAIL.equals(user.getEmail())));
        verify(userService).registerSeededAdmin(org.mockito.ArgumentMatchers.argThat(user -> ADMIN_EMAIL.equals(user.getEmail())));
        verify(userService, times(1)).register(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void failsWhenConnectedToAnotherDatabase() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("registro_login");

        assertThatThrownBy(() -> seeder(3, TEST_PASSWORD).run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires the 'universidad' database");

        verify(userRepository, never()).count();
        verify(userService, never()).register(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void seedsOnlyWhenUsersTableIsEmpty() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("universidad");
        when(userRepository.count()).thenReturn(1L);

        seeder(3, "").run(new DefaultApplicationArguments(new String[0]));

        verify(userService, never()).register(org.mockito.ArgumentMatchers.any(User.class));
        verify(userService, never()).registerSeededAdmin(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void failsClearlyWhenDatabaseCannotBeVerified() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection unavailable"));

        assertThatThrownBy(() -> seeder(3, TEST_PASSWORD).run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not verify the database before Faker seeding.")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void createsRandomUsersButNotKnownAccountsWithoutConfiguredPassword() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("universidad");

        seeder(3, "").run(new DefaultApplicationArguments(new String[0]));

        verify(userService, times(3)).register(org.mockito.ArgumentMatchers.any(User.class));
        verify(userService, never()).registerSeededAdmin(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void doesNotDuplicateKnownAccounts() throws Exception {
        User existingUser = new User(USER_EMAIL, "encoded", "Usuario", "Prueba");
        User existingAdmin = new User(ADMIN_EMAIL, "encoded", "Admin", "Prueba");
        existingAdmin.setRole(User.ROLE_ADMIN);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("universidad");
        when(userRepository.count()).thenReturn(22L);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingAdmin));

        seeder(3, TEST_PASSWORD).run(new DefaultApplicationArguments(new String[0]));

        verify(userService, never()).register(org.mockito.ArgumentMatchers.any(User.class));
        verify(userService, never()).registerSeededAdmin(org.mockito.ArgumentMatchers.any(User.class));
    }

    private FakeUserDataSeeder seeder(int userCount, String testPassword) {
        return new FakeUserDataSeeder(
                userRepository,
                userService,
                dataSource,
                userCount,
                testPassword,
                USER_EMAIL,
                ADMIN_EMAIL);
    }
}