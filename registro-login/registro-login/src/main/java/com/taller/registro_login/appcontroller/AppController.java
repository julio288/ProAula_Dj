package com.taller.registro_login.appcontroller;

import com.taller.registro_login.dto.RegistrationForm;
import com.taller.registro_login.dto.FreightLaneForm;
import com.taller.registro_login.dto.FreightRequestForm;
import com.taller.registro_login.dto.TransportNoticeForm;
import com.taller.registro_login.dto.UserEditForm;
import com.taller.registro_login.dto.AdminActivityReport;
import com.taller.registro_login.model.FreightRequestStatus;
import com.taller.registro_login.model.User;
import com.taller.registro_login.security.ApplicationUserPrincipal;
import com.taller.registro_login.service.FreightService;
import com.taller.registro_login.service.AdminReportService;
import com.taller.registro_login.service.NetworkMonitoringService;
import com.taller.registro_login.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Objects;

@Controller
public class AppController {

    private static final String PROFILE_VIEW = "profile";
    private static final String MESSAGE_ATTRIBUTE = "message";

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    private final UserService userService;
    private final FreightService freightService;
    private final AdminReportService adminReportService;
    private final NetworkMonitoringService networkMonitoringService;

    public AppController(
            UserService userService,
            FreightService freightService,
            AdminReportService adminReportService,
            NetworkMonitoringService networkMonitoringService) {
        this.userService = userService;
        this.freightService = freightService;
        this.adminReportService = adminReportService;
        this.networkMonitoringService = networkMonitoringService;
    }

    @ModelAttribute("googleLoginEnabled")
    public boolean googleLoginEnabled() {
        return StringUtils.hasText(googleClientId);
    }

    @GetMapping("/")
    public String viewHomePage() {
        return "index";
    }

    @GetMapping("/perfil")
    public String oidcProfile(@AuthenticationPrincipal OidcUser principal, Model model) {
        if (principal == null) {
            return "redirect:/profile";
        }
        model.addAttribute("fullName", principal.getFullName());
        model.addAttribute("email", principal.getEmail());
        model.addAttribute("picture", principal.getPicture());
        model.addAttribute("subject", principal.getSubject());
        return "perfil";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal ApplicationUserPrincipal principal, Model model) {
        model.addAttribute("fullName", principal.getFullName());
        addCustomerDashboardData(principal, model);
        return "dashboard";
    }

    @GetMapping("/admin")
    public String adminPanel(@AuthenticationPrincipal ApplicationUserPrincipal principal, Model model) {
        addAdminOverviewData(principal, model);
        return "admin";
    }

    @GetMapping("/admin/shipments")
    public String adminShipments(@AuthenticationPrincipal ApplicationUserPrincipal principal, Model model) {
        addAdminShipmentsData(principal, model);
        return "admin_shipments";
    }

    @GetMapping("/admin/routes")
    public String adminRoutes(@AuthenticationPrincipal ApplicationUserPrincipal principal, Model model) {
        addAdminRoutesData(principal, model);
        return "admin_routes";
    }

    @GetMapping("/admin/notices")
    public String adminNotices(@AuthenticationPrincipal ApplicationUserPrincipal principal, Model model) {
        addAdminNoticesData(principal, model);
        return "admin_notices";
    }

    @GetMapping("/admin/reports")
    public String adminReports(@AuthenticationPrincipal ApplicationUserPrincipal principal, Model model) {
        addAdminCommonData(principal, model);
        AdminActivityReport report = adminReportService.buildReport();
        model.addAttribute("report", report);
        model.addAttribute("requestStatuses", Arrays.asList(FreightRequestStatus.values()));
        return "admin_reports";
    }

    @GetMapping("/admin/monitoring")
    public String adminMonitoring(@AuthenticationPrincipal ApplicationUserPrincipal principal,
                                  @RequestParam(value = "filter", defaultValue = "all") String filter,
                                  Model model) {
        addAdminCommonData(principal, model);
        model.addAttribute("networkServices", networkMonitoringService.getNetworkServices());
        model.addAttribute("monitoringSummary", networkMonitoringService.getSummary());
        model.addAttribute("eventLogStatus", networkMonitoringService.getEventLogStatus());
        model.addAttribute("monitoringRefreshSeconds", networkMonitoringService.getRefreshIntervalSeconds());
        if ("anomalies".equalsIgnoreCase(filter)) {
            model.addAttribute("auditLogs", networkMonitoringService.getAnomaliesOnly());
        } else {
            model.addAttribute("auditLogs", networkMonitoringService.getAuditLogs());
        }
        model.addAttribute("activeFilter", filter);
        return "admin_monitoring";
    }

    @PostMapping("/shipments")
    public String createShipment(@AuthenticationPrincipal ApplicationUserPrincipal principal,
                                 @Valid @ModelAttribute("freightRequestForm") FreightRequestForm form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addCustomerDashboardData(principal, model);
            return "dashboard";
        }
        try {
            freightService.createRequest(principal.getUserId(), form);
        } catch (IllegalArgumentException exception) {
            addCustomerDashboardData(principal, model);
            model.addAttribute("requestError", exception.getMessage());
            return "dashboard";
        }
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE,
                "Tu solicitud de carga fue creada. Puedes seguirla desde Mis envíos.");
        return "redirect:/dashboard";
    }

    @PostMapping("/admin/lanes")
    public String createLane(@AuthenticationPrincipal ApplicationUserPrincipal principal,
                             @Valid @ModelAttribute("laneForm") FreightLaneForm form,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("formError", "Revisa los campos de la ruta e inténtalo de nuevo.");
            addAdminRoutesData(principal, model);
            return "admin_routes";
        }
        try {
            freightService.createLane(form);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "La ruta quedó publicada.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/routes";
    }

    @PostMapping("/admin/lanes/{id}/edit")
    public String updateLane(@PathVariable Long id,
                             @AuthenticationPrincipal ApplicationUserPrincipal principal,
                             @Valid @ModelAttribute("laneForm") FreightLaneForm form,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("formError", "Revisa los campos de la ruta e inténtalo de nuevo.");
            model.addAttribute("editLaneId", id);
            addAdminRoutesData(principal, model);
            return "admin_routes";
        }
        try {
            freightService.updateLane(id, form);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "La ruta se actualizó.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/routes";
    }

    @PostMapping("/admin/lanes/{id}/toggle")
    public String toggleLane(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            freightService.toggleLane(id);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "La disponibilidad de la ruta se actualizó.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/routes";
    }

    @PostMapping("/admin/notices")
    public String createNotice(@Valid @ModelAttribute("noticeForm") TransportNoticeForm form,
                               BindingResult result,
                               @AuthenticationPrincipal ApplicationUserPrincipal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("formError", "Revisa los campos del aviso e inténtalo de nuevo.");
            addAdminNoticesData(principal, model);
            return "admin_notices";
        }
        freightService.createNotice(form);
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "El aviso quedó publicado.");
        return "redirect:/admin/notices";
    }

    @PostMapping("/admin/notices/{id}/delete")
    public String deleteNotice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            freightService.deleteNotice(id);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "El aviso se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/notices";
    }

    @PostMapping("/admin/shipments/{id}/status")
    public String updateShipmentStatus(@PathVariable Long id,
                                      @RequestParam FreightRequestStatus status,
                                      RedirectAttributes redirectAttributes) {
        try {
            freightService.updateRequestStatus(id, status);
            redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "El estado del envío se actualizó.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/shipments";
    }

    private void addCustomerDashboardData(ApplicationUserPrincipal principal, Model model) {
        model.addAttribute("fullName", principal.getFullName());
        if (!model.containsAttribute("freightRequestForm")) {
            model.addAttribute("freightRequestForm", new FreightRequestForm());
        }
        model.addAttribute("lanes", freightService.findAvailableLanes());
        model.addAttribute("notices", freightService.findActiveNotices());
        model.addAttribute("customerRequests", freightService.findCustomerRequests(principal.getUserId()));
    }

    private void addAdminCommonData(ApplicationUserPrincipal principal, Model model) {
        model.addAttribute("fullName", principal.getFullName());
        model.addAttribute("userCount", userService.findAll().size());
        model.addAttribute("laneCount", freightService.countLanes());
        model.addAttribute("noticeCount", freightService.countNotices());
        model.addAttribute("requestCount", freightService.countRequests());
        model.addAttribute("openRequestCount", freightService.countOpenRequests());
    }

    private void addAdminOverviewData(ApplicationUserPrincipal principal, Model model) {
        addAdminCommonData(principal, model);
        model.addAttribute("recentRequests", freightService.findRecentRequests());
    }

    private void addAdminShipmentsData(ApplicationUserPrincipal principal, Model model) {
        addAdminCommonData(principal, model);
        model.addAttribute("freightRequests", freightService.findAllRequests());
        model.addAttribute("requestStatuses", Arrays.asList(FreightRequestStatus.values()));
    }

    private void addAdminRoutesData(ApplicationUserPrincipal principal, Model model) {
        addAdminCommonData(principal, model);
        model.addAttribute("lanes", freightService.findAllLanes());
        if (!model.containsAttribute("laneForm")) {
            model.addAttribute("laneForm", new FreightLaneForm());
        }
    }

    private void addAdminNoticesData(ApplicationUserPrincipal principal, Model model) {
        addAdminCommonData(principal, model);
        model.addAttribute("notices", freightService.findAllNotices());
        if (!model.containsAttribute("noticeForm")) {
            model.addAttribute("noticeForm", new TransportNoticeForm());
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal ApplicationUserPrincipal principal, Model model) {
        model.addAttribute("profileForm", UserEditForm.from(userService.findById(principal.getUserId())));
        return PROFILE_VIEW;
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal ApplicationUserPrincipal principal,
                                @Valid @ModelAttribute("profileForm") UserEditForm profileForm,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return PROFILE_VIEW;
        }
        try {
            userService.updateProfile(principal.getUserId(), profileForm.getEmail(),
                    profileForm.getFirstName(), profileForm.getLastName());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            return PROFILE_VIEW;
        }
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Tu perfil se actualizó correctamente.");
        return "redirect:/profile";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "registro";
    }

    @PostMapping("/process_register")
    public String processRegister(@Valid @ModelAttribute("registrationForm") RegistrationForm registrationForm,
                                  BindingResult result,
                                  Model model) {
        if (!Objects.equals(registrationForm.getPassword(), registrationForm.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Las contraseñas no coinciden.");
        }
        if (result.hasErrors()) {
            return "registro";
        }
        try {
            User user = registrationForm.toUser();
            if (User.ROLE_USER.equals(registrationForm.getAccountType())) {
                userService.register(user);
            } else if (User.ROLE_ADMIN.equals(registrationForm.getAccountType())) {
                userService.registerAdmin(user, registrationForm.getAdminCode());
            } else {
                throw new IllegalArgumentException("Selecciona un tipo de cuenta válido.");
            }
            return "registro_exitoso";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "registro";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/users")
    public String listUsers(Model model, @AuthenticationPrincipal ApplicationUserPrincipal principal) {
        model.addAttribute("listUsers", userService.findAll());
        model.addAttribute("actorEmail", principal.getEmail());
        return "usuarios";
    }

    @GetMapping("/users/{id}/edit")
    public String showEditUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("userId", id);
        model.addAttribute("userForm", UserEditForm.from(user));
        return "usuario_editar";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id,
                           @Valid @ModelAttribute("userForm") UserEditForm userForm,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("userId", id);
            return "usuario_editar";
        }
        try {
            userService.updateProfile(id, userForm.getEmail(), userForm.getFirstName(), userForm.getLastName());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("userId", id);
            model.addAttribute("formError", exception.getMessage());
            return "usuario_editar";
        }
        redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "Los datos del usuario se actualizaron.");
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal ApplicationUserPrincipal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id, principal.getUserId());
            redirectAttributes.addFlashAttribute(MESSAGE_ATTRIBUTE, "El usuario se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/users";
    }
}
