package com.taller.registro_login.service;

import com.taller.registro_login.config.MonitoringProperties;
import com.taller.registro_login.dto.AuditLogEntry;
import com.taller.registro_login.dto.MonitoringSummary;
import com.taller.registro_login.dto.NetworkServiceStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NetworkMonitoringService {

    private static final int MAX_AUDIT_ENTRIES = 500;
    private final MonitoringProperties properties;
    private final WindowsEventLogReader eventLogReader;
    private final List<MonitoringTarget> targets;
    private final Map<String, NetworkServiceStatus> latestStatuses = new ConcurrentHashMap<>();
    private final Map<String, Boolean> previousReachability = new ConcurrentHashMap<>();
    private final Set<String> importedEventIds = new LinkedHashSet<>();
    private final Deque<AuditLogEntry> auditLogs = new ArrayDeque<>();
    private volatile WindowsEventLogReader.ReadResult eventLogStatus =
            new WindowsEventLogReader.ReadResult(List.of(), "PENDIENTE", "La primera lectura aún no se ha ejecutado.");

    public NetworkMonitoringService(MonitoringProperties properties, WindowsEventLogReader eventLogReader) {
        this.properties = properties;
        this.eventLogReader = eventLogReader;
        validateIntervals(properties);
        this.targets = parseTargets(properties.getTargets());
    }

    @Scheduled(
            fixedDelayString = "${monitoring.poll-interval:30s}",
            initialDelayString = "${monitoring.initial-delay:1s}")
    public void poll() {
        pollConfiguredServices();
        pollWindowsEventLogs();
    }

    private void pollConfiguredServices() {
        for (MonitoringTarget target : targets) {
            long startedAt = System.nanoTime();
            boolean reachable = false;
            String details = target.details();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(target.host(), target.port()),
                        Math.toIntExact(properties.getConnectTimeout().toMillis()));
                reachable = true;
            } catch (IOException | SecurityException | IllegalArgumentException exception) {
                details = "No se pudo establecer conexión: " + safeMessage(exception);
            }

            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            String label = reachable ? "OPERACIONAL" : "SIN CONEXIÓN";
            NetworkServiceStatus status = new NetworkServiceStatus(
                    target.name(), target.protocol(), target.host(), target.port(),
                    reachable, latencyMs, label, details);
            latestStatuses.put(target.name(), status);
            recordAvailabilityTransition(target, reachable);
        }
    }

    private void recordAvailabilityTransition(MonitoringTarget target, boolean reachable) {
        Boolean previous = previousReachability.put(target.name(), reachable);
        if (previous != null && previous == reachable) {
            return;
        }
        if (!reachable) {
            addAuditLog(new AuditLogEntry(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now(),
                    "WARNING",
                    target.name(),
                    target.host(),
                    "SERVICE_UNAVAILABLE",
                    "No se pudo establecer conexión TCP con " + target.host() + ":" + target.port() + ".",
                    true));
        } else if (Boolean.FALSE.equals(previous)) {
            addAuditLog(new AuditLogEntry(
                    UUID.randomUUID().toString(),
                    LocalDateTime.now(),
                    "INFO",
                    target.name(),
                    target.host(),
                    "SERVICE_RESTORED",
                    "La conexión TCP con " + target.host() + ":" + target.port() + " se restableció.",
                    false));
        }
    }

    private void pollWindowsEventLogs() {
        WindowsEventLogReader.ReadResult result = eventLogReader.read(
                properties.getWindowsEventLogChannels(), properties.isWindowsEventLogEnabled());
        eventLogStatus = result;
        for (AuditLogEntry entry : result.entries()) {
            synchronized (importedEventIds) {
                if (importedEventIds.add(entry.getId())) {
                    addAuditLog(entry);
                    while (importedEventIds.size() > MAX_AUDIT_ENTRIES * 2) {
                        importedEventIds.remove(importedEventIds.iterator().next());
                    }
                }
            }
        }
    }

    private synchronized void addAuditLog(AuditLogEntry entry) {
        auditLogs.addFirst(entry);
        while (auditLogs.size() > MAX_AUDIT_ENTRIES) {
            auditLogs.removeLast();
        }
    }

    public List<NetworkServiceStatus> getNetworkServices() {
        return latestStatuses.values().stream()
                .sorted(Comparator.comparing(NetworkServiceStatus::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized List<AuditLogEntry> getAuditLogs() {
        return List.copyOf(auditLogs);
    }

    public synchronized List<AuditLogEntry> getAnomaliesOnly() {
        return auditLogs.stream().filter(AuditLogEntry::isAnomaly).toList();
    }

    public MonitoringSummary getSummary() {
        List<NetworkServiceStatus> statuses = getNetworkServices();
        int available = (int) statuses.stream().filter(NetworkServiceStatus::isReachable).count();
        int unavailable = statuses.size() - available;
        LocalDateTime lastChecked = statuses.stream()
                .map(NetworkServiceStatus::getCheckedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        String label;
        if (targets.isEmpty()) {
            label = "SIN SERVICIOS CONFIGURADOS";
        } else if (statuses.size() < targets.size()) {
            label = "ESPERANDO PRIMER SONDEO";
        } else if (unavailable > 0) {
            label = "REVISAR SERVICIOS";
        } else {
            label = "OPERATIVO";
        }
        return new MonitoringSummary(targets.size(), available, unavailable, lastChecked, label);
    }

    public WindowsEventLogReader.ReadResult getEventLogStatus() {
        return eventLogStatus;
    }

    public long getRefreshIntervalSeconds() {
        return Math.max(10, properties.getPollInterval().toSeconds());
    }

    static List<MonitoringTarget> parseTargets(String definitions) {
        if (definitions == null || definitions.isBlank()) {
            return List.of();
        }

        List<MonitoringTarget> parsed = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (String definition : definitions.split(";")) {
            String[] fields = definition.strip().split("\\|", -1);
            if (fields.length != 5) {
                throw new IllegalStateException(
                        "Cada destino de MONITORING_TARGETS debe tener el formato nombre|host|puerto|TCP|descripción.");
            }
            String name = fields[0].strip();
            String host = fields[1].strip();
            String protocol = fields[3].strip().toUpperCase(Locale.ROOT);
            String details = fields[4].strip();
            int port;
            try {
                port = Integer.parseInt(fields[2].strip());
            } catch (NumberFormatException exception) {
                throw new IllegalStateException("El puerto de cada destino debe ser un número entre 1 y 65535.", exception);
            }
            if (name.isBlank() || host.isBlank() || details.isBlank() || name.length() > 80 || host.length() > 253
                    || !host.matches("[A-Za-z0-9._:%-]+") || port < 1 || port > 65535 || !"TCP".equals(protocol)) {
                throw new IllegalStateException(
                        "Destino inválido en MONITORING_TARGETS: use nombre, host, puerto válido, protocolo TCP y descripción.");
            }
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("Los nombres de destinos de MONITORING_TARGETS deben ser únicos.");
            }
            parsed.add(new MonitoringTarget(name, host, port, protocol, details));
        }
        if (parsed.size() > 50) {
            throw new IllegalStateException("MONITORING_TARGETS admite un máximo de 50 destinos.");
        }
        return List.copyOf(parsed);
    }

    private void validateIntervals(MonitoringProperties configuration) {
        Duration pollInterval = configuration.getPollInterval();
        Duration timeout = configuration.getConnectTimeout();
        if (pollInterval == null || pollInterval.compareTo(Duration.ofSeconds(10)) < 0
                || pollInterval.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalStateException("MONITORING_POLL_INTERVAL debe estar entre 10 segundos y 1 hora.");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()
                || timeout.compareTo(Duration.ofSeconds(10)) > 0) {
            throw new IllegalStateException("MONITORING_CONNECT_TIMEOUT debe estar entre 1 milisegundo y 10 segundos.");
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    record MonitoringTarget(String name, String host, int port, String protocol, String details) {
    }
}
