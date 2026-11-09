package com.taller.registro_login.service;

import com.taller.registro_login.config.MonitoringProperties;
import com.taller.registro_login.dto.AuditLogEntry;
import com.taller.registro_login.dto.NetworkServiceStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkMonitoringServiceTests {

    @Test
    void pollsConfiguredTcpServiceAndReportsObservedAvailability() throws IOException {
        try (ServerSocket server = new ServerSocket(0)) {
            MonitoringProperties properties = properties(
                    "Test web|127.0.0.1|" + server.getLocalPort() + "|TCP|Local test service");
            NetworkMonitoringService service = new NetworkMonitoringService(properties, new WindowsEventLogReader());

            service.poll();

            List<NetworkServiceStatus> statuses = service.getNetworkServices();
            assertEquals(1, statuses.size());
            assertTrue(statuses.getFirst().isReachable());
            assertEquals("OPERACIONAL", statuses.getFirst().getStatusLabel());
            assertNotNull(statuses.getFirst().getCheckedAt());
            assertEquals(1, service.getSummary().availableServices());
            assertEquals("OPERATIVO", service.getSummary().statusLabel());
            assertTrue(service.getAuditLogs().isEmpty());
        }
    }

    @Test
    void reportsUnavailableConfiguredServiceAsAnomaly() throws IOException {
        int closedPort;
        try (ServerSocket server = new ServerSocket(0)) {
            closedPort = server.getLocalPort();
        }
        NetworkMonitoringService service = new NetworkMonitoringService(
                properties("Test web|127.0.0.1|" + closedPort + "|TCP|Local test service"),
                new WindowsEventLogReader());

        service.poll();

        assertFalse(service.getNetworkServices().getFirst().isReachable());
        assertEquals("REVISAR SERVICIOS", service.getSummary().statusLabel());
        assertEquals(1, service.getAnomaliesOnly().size());
        assertEquals("SERVICE_UNAVAILABLE", service.getAnomaliesOnly().getFirst().getEventType());
    }

    @Test
    void rejectsMalformedOrUnsupportedMonitoringTargets() {
        assertThrows(IllegalStateException.class, () -> NetworkMonitoringService.parseTargets("invalid"));
        assertThrows(IllegalStateException.class,
                () -> NetworkMonitoringService.parseTargets("DNS|server|53|UDP|DNS service"));
        assertThrows(IllegalStateException.class,
                () -> NetworkMonitoringService.parseTargets("Web|server|8080|TCP|Web;web|server|8081|TCP|Duplicate"));
    }

    @Test
    void parsesWindowsEventAndMarksKnownAnomaly() throws IOException {
        String xml = """
                <Event xmlns="http://schemas.microsoft.com/win/2004/08/events/event">
                  <System>
                    <Provider Name="Microsoft-Windows-Security-Auditing"/>
                    <EventID>4625</EventID><Level>0</Level>
                    <TimeCreated SystemTime="2026-11-09T20:00:00.000Z"/>
                    <EventRecordID>105</EventRecordID>
                  </System>
                  <EventData>
                    <Data Name="IpAddress">203.0.113.7</Data>
                    <Data Name="TargetUserName">academic-test</Data>
                  </EventData>
                  <RenderingInfo><Message>An account failed to log on.</Message></RenderingInfo>
                </Event>
                """;

        List<AuditLogEntry> events = new WindowsEventLogReader().parseEvents("Security", xml);

        assertEquals(1, events.size());
        assertTrue(events.getFirst().isAnomaly());
        assertEquals("203.0.113.7", events.getFirst().getSourceIp());
        assertEquals("INFO", events.getFirst().getLevel());
        assertEquals("Security:105:2026-11-09T20:00:00.000Z", events.getFirst().getId());
    }

    @Test
    void rejectsXmlWithDocumentTypeDeclarations() {
        String xml = """
                <!DOCTYPE Event [<!ENTITY xxe SYSTEM "file:///windows/win.ini">]>
                <Event><System><EventID>4625</EventID></System><Message>&xxe;</Message></Event>
                """;

        assertThrows(IOException.class, () -> new WindowsEventLogReader().parseEvents("Security", xml));
    }

    private MonitoringProperties properties(String targets) {
        MonitoringProperties properties = new MonitoringProperties();
        properties.setTargets(targets);
        properties.setConnectTimeout(Duration.ofMillis(500));
        properties.setPollInterval(Duration.ofSeconds(30));
        properties.setWindowsEventLogEnabled(false);
        return properties;
    }
}
