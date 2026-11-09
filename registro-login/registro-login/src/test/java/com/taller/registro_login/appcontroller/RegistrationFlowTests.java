package com.taller.registro_login.appcontroller;

import com.taller.registro_login.model.User;
import com.taller.registro_login.service.FreightService;
import com.taller.registro_login.service.AdminReportService;
import com.taller.registro_login.service.NetworkMonitoringService;
import com.taller.registro_login.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class RegistrationFlowTests {

    private final UserService userService = mock(UserService.class);
    private final FreightService freightService = mock(FreightService.class);
    private final AdminReportService adminReportService = mock(AdminReportService.class);
    private final NetworkMonitoringService networkMonitoringService = mock(NetworkMonitoringService.class);
    private final Validator validator;
    private MockMvc mockMvc;

    RegistrationFlowTests() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        validator = validatorFactory;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AppController(userService, freightService, adminReportService, networkMonitoringService))
                .setValidator(validator)
                .build();
    }

    @Test
    void registersAValidatedUser() throws Exception {
        when(userService.register(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/process_register")
                        .param("email", "person@example.test")
                        .param("password", "ValidPass123!")
                        .param("confirmPassword", "ValidPass123!")
                        .param("firstName", "Test")
                        .param("lastName", "Person")
                        .param("accountType", User.ROLE_USER))
                .andExpect(status().isOk())
                .andExpect(view().name("registro_exitoso"));

        verify(userService).register(any(User.class));
    }

    @Test
    void rejectsDifferentPasswordsWithoutCallingRegistrationService() throws Exception {
        mockMvc.perform(post("/process_register")
                        .param("email", "person@example.test")
                        .param("password", "ValidPass123!")
                        .param("confirmPassword", "DifferentPass123!")
                        .param("firstName", "Test")
                        .param("lastName", "Person")
                        .param("accountType", User.ROLE_USER))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "confirmPassword"));

        verify(userService, never()).register(any(User.class));
    }

    @Test
    void redisplaysFormWhenEmailIsAlreadyRegistered() throws Exception {
        when(userService.register(any(User.class)))
                .thenThrow(new IllegalArgumentException("El correo electrónico ya se encuentra registrado."));

        mockMvc.perform(post("/process_register")
                        .param("email", "person@example.test")
                        .param("password", "ValidPass123!")
                        .param("confirmPassword", "ValidPass123!")
                        .param("firstName", "Test")
                        .param("lastName", "Person")
                        .param("accountType", User.ROLE_USER))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attribute("error",
                        "El correo electrónico ya se encuentra registrado."));
    }

    @Test
    void registersAnAdministratorOnlyThroughTheAdminCodeFlow() throws Exception {
        when(userService.registerAdmin(any(User.class), org.mockito.ArgumentMatchers.eq("admin-code")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/process_register")
                        .param("email", "admin@example.test")
                        .param("password", "ValidPass123!")
                        .param("confirmPassword", "ValidPass123!")
                        .param("firstName", "Admin")
                        .param("lastName", "Person")
                        .param("accountType", User.ROLE_ADMIN)
                        .param("adminCode", "admin-code"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro_exitoso"));

        verify(userService).registerAdmin(any(User.class), org.mockito.ArgumentMatchers.eq("admin-code"));
        verify(userService, never()).register(any(User.class));
    }
}
