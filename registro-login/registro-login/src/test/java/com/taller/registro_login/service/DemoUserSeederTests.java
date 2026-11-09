package com.taller.registro_login.service;

import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoUserSeederTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);

    @Test
    void createsDemoUserAndAdminWhenDatabaseIsEmpty() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findByEmail("demo.usuario@universidad.test")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("demo.admin@universidad.test")).thenReturn(Optional.empty());

        new DemoUserSeeder(
                userRepository,
                userService,
                "demo.usuario@universidad.test",
                "Demo123!",
                "demo.admin@universidad.test",
                "Admin123!").run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).register(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("demo.usuario@universidad.test");

        ArgumentCaptor<User> adminCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).registerSeededAdmin(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getEmail()).isEqualTo("demo.admin@universidad.test");
    }

    @Test
    void skipsSeedWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(7L);

        new DemoUserSeeder(
                userRepository,
                userService,
                "demo.usuario@universidad.test",
                "Demo123!",
                "demo.admin@universidad.test",
                "Admin123!").run(new DefaultApplicationArguments(new String[0]));

        verify(userService, never()).register(org.mockito.ArgumentMatchers.any(User.class));
        verify(userService, never()).registerSeededAdmin(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void failsWhenDemoPasswordIsBlank() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findByEmail("demo.usuario@universidad.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DemoUserSeeder(
                userRepository,
                userService,
                "demo.usuario@universidad.test",
                "",
                "demo.admin@universidad.test",
                "Admin123!").run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo user");
    }
}
