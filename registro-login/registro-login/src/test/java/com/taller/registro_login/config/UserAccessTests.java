package com.taller.registro_login.config;

import com.taller.registro_login.model.User;
import com.taller.registro_login.model.FreightLane;
import com.taller.registro_login.model.FreightRequest;
import com.taller.registro_login.model.FreightRequestStatus;
import com.taller.registro_login.dto.FreightRequestForm;
import com.taller.registro_login.repository.FreightLaneRepository;
import com.taller.registro_login.repository.FreightRequestRepository;
import com.taller.registro_login.repository.UserRepository;
import com.taller.registro_login.security.CustomUserDetails;
import com.taller.registro_login.security.ApplicationOidcUser;
import com.taller.registro_login.service.FreightService;
import com.taller.registro_login.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FreightLaneRepository freightLaneRepository;

    @Autowired
    private FreightRequestRepository freightRequestRepository;

    @Autowired
    private FreightService freightService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void publicEntryPagesRenderTheFreightBrand() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("logística inteligente y red segura")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/oauth2/authorization/google"))));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Iniciar sesión")));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear tu cuenta")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Nivel de Autorización"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("adminCode"))));
    }

    @Test
    void oidcProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/perfil"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void oidcProfileDisplaysIdentityClaims() throws Exception {
        mockMvc.perform(get("/perfil").with(oidcLogin().idToken(token -> token
                        .claim("sub", "google-subject-123")
                        .claim("name", "Ana Prueba")
                        .claim("email", "ana@example.test")
                        .claim("picture", "https://example.test/avatar.png"))))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ana Prueba")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ana@example.test")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("google-subject-123")));
    }

    @Test
    void oidcLogoutReturnsToPublicHome() throws Exception {
        mockMvc.perform(post("/logout").with(oidcLogin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void localLogoutKeepsItsExistingRedirect() throws Exception {
        User localUser = new User("local-logout@example.test", "encoded", "Usuario", "Local");

        mockMvc.perform(post("/logout")
                        .with(user(new CustomUserDetails(localUser)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    void localUserCanOpenTheirExistingProfileFromOidcProfilePath() throws Exception {
        User localUser = new User("local-profile@example.test", "encoded", "Usuario", "Local");

        mockMvc.perform(get("/perfil").with(user(new CustomUserDetails(localUser))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void regularUserCanOpenDashboard() throws Exception {
        User regularUser = new User("dashboard-user@example.test", "encoded", "Usuario", "Prueba");

        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(regularUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void googleAccountCanOpenTheCustomerDashboard() throws Exception {
        User customer = userService.findOrCreateGoogleUser(
                "google-dashboard@example.test", "Google Cliente");
        assertEquals(customer.getId(), userRepository.findByEmail(customer.getEmail()).orElseThrow().getId());
        ApplicationOidcUser principal = oidcPrincipal(customer);

        mockMvc.perform(get("/dashboard").with(oidcLogin().oidcUser(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Google Cliente")));
    }

    @Test
    void googleCustomerCanCreateAShipmentInTheirOwnAccount() throws Exception {
        User customer = userService.findOrCreateGoogleUser(
                "google-shipment@example.test", "Google Cliente");
        FreightLane lane = freightLaneRepository.save(new FreightLane(
                "Bogotá", "Medellín", "Lun-Vie · 08:00", 2,
                new BigDecimal("1850.00"), new BigDecimal("18000.00")));
        ApplicationOidcUser principal = oidcPrincipal(customer);

        mockMvc.perform(post("/shipments")
                        .with(oidcLogin().oidcUser(principal))
                        .with(csrf())
                        .param("laneId", lane.getId().toString())
                        .param("weightKg", "12.5")
                        .param("cargoDescription", "Paquete de prueba")
                        .param("recipientName", "Persona destinataria")
                        .param("recipientPhone", "3001234567")
                        .param("originAddress", "Calle 10, Bogotá")
                        .param("destinationAddress", "Carrera 20, Medellín"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        List<FreightRequest> customerRequests =
                freightRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        assertEquals(1, customerRequests.size());
        assertEquals("Paquete de prueba", customerRequests.getFirst().getCargoDescription());
    }

    @Test
    void customerDashboardShowsOnlyTheirOwnShipmentRequests() throws Exception {
        User customer = userRepository.save(
                new User("freight-customer@example.test", "encoded-password", "Cliente", "Prueba"));
        User anotherCustomer = userRepository.save(
                new User("other-freight-customer@example.test", "encoded-password", "Otra", "Persona"));
        FreightLane lane = freightLaneRepository.save(new FreightLane(
                "Bogotá", "Medellín", "Lun-Vie · 08:00", 2,
                new BigDecimal("1850.00"), new BigDecimal("18000.00")));
        FreightRequest customerRequest = freightService.createRequest(customer.getId(), requestForm(lane.getId()));
        FreightRequest otherRequest = freightService.createRequest(
                anotherCustomer.getId(), requestForm(lane.getId()));

        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(customer))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        customerRequest.getTrackingCode())))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(otherRequest.getTrackingCode()))));
    }

    private ApplicationOidcUser oidcPrincipal(User account) {
        Instant issuedAt = Instant.now();
        Map<String, Object> claims = Map.of(
                "sub", "google-sub-" + account.getId(),
                "email", account.getEmail(),
                "email_verified", true,
                "name", account.getFullName());
        OidcIdToken idToken = new OidcIdToken(
                "test-id-token", issuedAt, issuedAt.plusSeconds(300), claims);
        DefaultOidcUser googleUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(User.ROLE_USER)), idToken, "sub");
        return new ApplicationOidcUser(account, googleUser);
    }

    @Test
    void administratorCanAdvanceCustomerShipmentStatus() throws Exception {
        User customer = userRepository.save(
                new User("status-customer@example.test", "encoded-password", "Cliente", "Carga"));
        FreightLane lane = freightLaneRepository.save(new FreightLane(
                "Bogotá", "Cali", "Lun-Vie · 08:00", 2,
                new BigDecimal("1850.00"), new BigDecimal("18000.00")));
        FreightRequest request = freightService.createRequest(customer.getId(), requestForm(lane.getId()));
        User admin = new User("status-admin@example.test", "encoded", "Admin", "Carga");
        admin.setRole(User.ROLE_ADMIN);

        mockMvc.perform(post("/admin/shipments/{id}/status", request.getId())
                        .with(user(new CustomUserDetails(admin)))
                        .with(csrf())
                        .param("status", FreightRequestStatus.QUOTED.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/shipments"));

        assertEquals(FreightRequestStatus.QUOTED,
                freightRequestRepository.findById(request.getId()).orElseThrow().getStatus());
    }

    @Test
    void administratorDashboardShowsSummaryAndRecentActivity() throws Exception {
        User customer = userRepository.save(
                new User("admin-view-customer@example.test", "encoded-password", "Cliente", "Prueba"));
        User admin = new User("admin-view@example.test", "encoded-password", "Admin", "Prueba");
        admin.setRole(User.ROLE_ADMIN);
        admin = userRepository.save(admin);
        FreightLane lane = freightLaneRepository.save(new FreightLane(
                "Bogotá", "Pereira", "Lun-Vie · 08:00", 2,
                new BigDecimal("1850.00"), new BigDecimal("18000.00")));
        FreightRequest request = freightService.createRequest(customer.getId(), requestForm(lane.getId()));

        mockMvc.perform(get("/admin").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Centro de control")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(request.getTrackingCode())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reportes")));
    }

    @Test
    void administratorCanOpenEachIndependentWorkspace() throws Exception {
        User admin = new User("admin-workspaces@example.test", "encoded-password", "Admin", "Prueba");
        admin.setRole(User.ROLE_ADMIN);
        admin = userRepository.save(admin);

        mockMvc.perform(get("/admin/shipments").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_shipments"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Todos los envíos")));
        mockMvc.perform(get("/admin/routes").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_routes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Agregar ruta")));
        mockMvc.perform(get("/admin/notices").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_notices"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear aviso")));
        mockMvc.perform(get("/admin/reports").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_reports"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Flujo de solicitudes")));
    }

    @Test
    void monitoringPanelIsAdminOnlyAndExplainsUnconfiguredTargets() throws Exception {
        User admin = new User("monitor-admin@example.test", "encoded-password", "Admin", "Monitoreo");
        admin.setRole(User.ROLE_ADMIN);
        admin = userRepository.save(admin);

        mockMvc.perform(get("/admin/monitoring"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        mockMvc.perform(get("/admin/monitoring").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_monitoring"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No hay destinos configurados para monitoreo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DESACTIVADO")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MONITORING_TARGETS")));
    }

    @Test
    void reportsRenderAccessibleChartsAndAccurateActivityMetrics() throws Exception {
        User customer = userRepository.save(
                new User("report-customer@example.test", "encoded-password", "Cliente", "Reporte"));
        User admin = new User("report-admin@example.test", "encoded-password", "Admin", "Reporte");
        admin.setRole(User.ROLE_ADMIN);
        admin = userRepository.save(admin);
        FreightLane popularLane = freightLaneRepository.save(new FreightLane(
                "Bogotá", "Cali", "Lun-Vie · 08:00", 2,
                new BigDecimal("2000.00"), new BigDecimal("10000.00")));
        FreightLane otherLane = freightLaneRepository.save(new FreightLane(
                "Bogotá", "Pereira", "Lun-Vie · 08:00", 2,
                new BigDecimal("2000.00"), new BigDecimal("10000.00")));

        FreightRequest delivered = freightService.createRequest(
                customer.getId(), requestForm(popularLane.getId()));
        freightService.updateRequestStatus(delivered.getId(), FreightRequestStatus.QUOTED);
        freightService.updateRequestStatus(delivered.getId(), FreightRequestStatus.IN_TRANSIT);
        freightService.updateRequestStatus(delivered.getId(), FreightRequestStatus.DELIVERED);
        freightService.createRequest(customer.getId(), requestForm(popularLane.getId()));
        freightService.createRequest(customer.getId(), requestForm(otherLane.getId()));

        mockMvc.perform(get("/admin/reports").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Solicitudes por mes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Flujo de solicitudes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rutas más utilizadas")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Clientes con más solicitudes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entregas completadas")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("33,3%")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("$ 51.000")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bogotá → Cali")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cliente Reporte")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "La aplicación todavía no registra qué trabajador atendió cada solicitud")));
    }

    @Test
    void regularUserCannotOpenIndependentAdminWorkspaces() throws Exception {
        User regularUser = new User("regular-workspaces@example.test", "encoded", "Usuario", "Prueba");

        for (String path : List.of("/admin/shipments", "/admin/routes", "/admin/notices", "/admin/reports")) {
            mockMvc.perform(get(path).with(user(new CustomUserDetails(regularUser))))
                    .andExpect(status().isForbidden())
                    .andExpect(forwardedUrl("/access-denied"));
        }
    }

    @Test
    void customerCanSubmitFreightRequest() throws Exception {
        User customer = userRepository.save(
                new User("submit-freight@example.test", "encoded-password", "Cliente", "Solicitud"));
        FreightLane lane = freightLaneRepository.save(new FreightLane(
                "Cali", "Bogotá", "Lun-Vie · 08:00", 2,
                new BigDecimal("1800.00"), new BigDecimal("16000.00")));

        mockMvc.perform(post("/shipments")
                        .with(user(new CustomUserDetails(customer)))
                        .with(csrf())
                        .param("laneId", lane.getId().toString())
                        .param("cargoDescription", "Repuestos para vehículo")
                        .param("weightKg", "12.5")
                        .param("recipientName", "Persona destino")
                        .param("recipientPhone", "3001234567")
                        .param("originAddress", "Calle 10")
                        .param("destinationAddress", "Carrera 20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertEquals(1, freightRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).size());
    }

    @Test
    void regularUserCannotChangeShipmentStatus() throws Exception {
        User regularUser = new User("freight-user@example.test", "encoded", "Usuario", "Carga");

        mockMvc.perform(post("/admin/shipments/10/status")
                        .with(user(new CustomUserDetails(regularUser)))
                        .with(csrf())
                        .param("status", FreightRequestStatus.QUOTED.name()))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCanUpdateOwnProfile() throws Exception {
        User regularUser = userRepository.save(
                new User("profile-user@example.test", "encoded-password", "Usuario", "Prueba"));

        mockMvc.perform(post("/profile")
                        .with(user(new CustomUserDetails(regularUser)))
                        .with(csrf())
                        .param("email", "profile-updated@example.test")
                        .param("firstName", "Nombre")
                        .param("lastName", "Actualizado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals("profile-updated@example.test", updatedUser.getEmail());
        assertEquals("Nombre", updatedUser.getFirstName());
        assertEquals("Actualizado", updatedUser.getLastName());
    }

    @Test
    void regularUserCannotOpenAdminUserList() throws Exception {
        User regularUser = new User("user@example.test", "encoded", "Usuario", "Prueba");

        mockMvc.perform(get("/users").with(user(new CustomUserDetails(regularUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotOpenAdminPanel() throws Exception {
        User regularUser = new User("regular-admin-test@example.test", "encoded", "Usuario", "Prueba");

        mockMvc.perform(get("/admin").with(user(new CustomUserDetails(regularUser))))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    @Test
    void accessDeniedPageExplainsRequiredAdministratorRole() throws Exception {
        User regularUser = new User("denied-page-test@example.test", "encoded", "Usuario", "Prueba");

        mockMvc.perform(get("/access-denied").with(user(new CustomUserDetails(regularUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "exclusivamente para cuentas con rol ADMIN")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Autenticada")));
    }

    @Test
    void publicAccessDeniedPageDoesNotClaimTheVisitorIsAuthenticated() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Inicia sesión con una cuenta autorizada")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Iniciaste sesión correctamente"))));
    }

    @Test
    void administratorCanOpenAdminPanel() throws Exception {
        User admin = new User("admin-panel-test@example.test", "encoded", "Admin", "Prueba");
        admin.setRole(User.ROLE_ADMIN);

        mockMvc.perform(get("/admin").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
    }

    @Test
    void administratorCanOpenUserList() throws Exception {
        User admin = new User("admin@example.test", "encoded", "Admin", "Prueba");
        admin.setRole(User.ROLE_ADMIN);

        mockMvc.perform(get("/users").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Crear cuenta de administrador")));
    }

    @Test
    void administratorCanCreateAdminAccountFromUserManagementPage() throws Exception {
        User actor = new User("admin-create-actor@example.test", "encoded-password", "Admin", "Creador");
        actor.setRole(User.ROLE_ADMIN);
        actor = userRepository.save(actor);

        mockMvc.perform(post("/users/admin")
                        .with(user(new CustomUserDetails(actor)))
                        .with(csrf())
                        .param("firstName", "Nuevo")
                        .param("lastName", "Admin")
                        .param("email", "new-admin@example.test")
                        .param("password", "InitialPass123!")
                        .param("confirmPassword", "InitialPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        User created = userRepository.findByEmail("new-admin@example.test").orElseThrow();
        assertEquals(User.ROLE_ADMIN, created.getRole());
        assertTrue(passwordEncoder.matches("InitialPass123!", created.getPassword()));
    }

    @Test
    void regularUserCannotCreateAdminAccount() throws Exception {
        User regularUser = new User("regular-create-actor@example.test", "encoded", "Usuario", "Prueba");

        mockMvc.perform(post("/users/admin")
                        .with(user(new CustomUserDetails(regularUser)))
                        .with(csrf())
                        .param("firstName", "No")
                        .param("lastName", "Autorizado")
                        .param("email", "forbidden-admin@example.test")
                        .param("password", "InitialPass123!")
                        .param("confirmPassword", "InitialPass123!"))
                .andExpect(status().isForbidden());

        assertTrue(userRepository.findByEmail("forbidden-admin@example.test").isEmpty());
    }

    private FreightRequestForm requestForm(Long laneId) {
        FreightRequestForm form = new FreightRequestForm();
        form.setLaneId(laneId);
        form.setCargoDescription("Repuestos");
        form.setWeightKg(new BigDecimal("8.5"));
        form.setRecipientName("Persona destino");
        form.setRecipientPhone("3001234567");
        form.setOriginAddress("Calle 1");
        form.setDestinationAddress("Carrera 2");
        return form;
    }
}