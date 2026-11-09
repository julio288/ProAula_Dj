package com.taller.registro_login.service;

import com.taller.registro_login.dto.AuditLogEntry;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WindowsEventLogReader {

    private static final int MAX_EVENTS_PER_CHANNEL = 40;
    private static final int MAX_OUTPUT_BYTES = 4 * 1024 * 1024;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final Set<Integer> ANOMALOUS_EVENT_IDS =
            Set.of(4625, 4720, 4740, 1102, 5152, 5157, 7000, 7034, 7045, 1000);
    private static final Pattern IP_PATTERN = Pattern.compile(
            "(?<![\\w:])(?:\\d{1,3}\\.){3}\\d{1,3}(?![\\w:])|(?<![\\w:])[0-9a-fA-F:]{3,39}(?![\\w:])");
    private static final Pattern SAFE_CHANNEL = Pattern.compile("[A-Za-z0-9._/-]{1,200}");

    public ReadResult read(List<String> channels, boolean enabled) {
        if (!enabled) {
            return new ReadResult(List.of(), "DESACTIVADO", "Lectura del registro de eventos desactivada por configuración.");
        }
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            return new ReadResult(List.of(), "NO DISPONIBLE", "La lectura de Windows Event Log requiere ejecutar la aplicación en Windows Server.");
        }
        if (channels == null || channels.isEmpty()) {
            return new ReadResult(List.of(), "SIN CONFIGURAR", "No hay canales de Windows Event Log configurados.");
        }

        List<AuditLogEntry> entries = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (String channel : channels) {
            if (channel == null || !SAFE_CHANNEL.matcher(channel).matches()) {
                errors.add("Canal inválido en la configuración.");
                continue;
            }
            try {
                entries.addAll(readChannel(channel));
            } catch (IOException | InterruptedException | SecurityException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
                errors.add(channel + ": " + safeError(exception));
            }
        }

        if (errors.isEmpty()) {
            return new ReadResult(entries, "CONECTADO", "Lectura activa · " + String.join(", ", channels));
        }
        if (!entries.isEmpty()) {
            return new ReadResult(entries, "PARCIAL", "Algunos canales no se pudieron consultar: " + String.join("; ", errors));
        }
        return new ReadResult(List.of(), "ERROR", "No se pudieron consultar los canales configurados: " + String.join("; ", errors));
    }

    private List<AuditLogEntry> readChannel(String channel) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "wevtutil.exe", "qe", channel,
                "/f:RenderedXml", "/c:" + MAX_EVENTS_PER_CHANNEL, "/rd:true")
                .redirectErrorStream(true)
                .start();

        CompletableFuture<byte[]> outputFuture = CompletableFuture.supplyAsync(() -> {
            try (InputStream input = process.getInputStream()) {
                return input.readNBytes(MAX_OUTPUT_BYTES + 1);
            } catch (IOException exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        });

        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                throw new IOException("Tiempo de consulta agotado.");
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            outputFuture.cancel(true);
            throw exception;
        }

        byte[] output;
        try {
            output = outputFuture.get(2, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IOException("No se pudo leer la respuesta de Windows Event Log.", exception);
        }
        if (output.length > MAX_OUTPUT_BYTES) {
            throw new IOException("La respuesta del canal supera el límite permitido.");
        }
        if (process.exitValue() != 0) {
            String error = new String(output, StandardCharsets.UTF_8).strip();
            throw new IOException(error.isBlank() ? "wevtutil devolvió un error." : error.substring(0, Math.min(error.length(), 300)));
        }

        String xml = new String(output, StandardCharsets.UTF_8).strip();
        if (xml.isEmpty()) {
            return List.of();
        }
        return parseEvents(channel, xml);
    }

    List<AuditLogEntry> parseEvents(String channel, String eventsXml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader("<Events>" + eventsXml + "</Events>")));
            NodeList events = document.getElementsByTagNameNS("*", "Event");
            if (events.getLength() == 0) {
                events = document.getElementsByTagName("Event");
            }

            List<AuditLogEntry> result = new ArrayList<>();
            for (int index = 0; index < events.getLength(); index++) {
                Node node = events.item(index);
                if (node instanceof Element event) {
                    result.add(toEntry(channel, event));
                }
            }
            return result;
        } catch (Exception exception) {
            throw new IOException("No se pudo interpretar el XML devuelto por Windows Event Log.", exception);
        }
    }

    private AuditLogEntry toEntry(String channel, Element event) {
        String eventIdText = childText(event, "EventID");
        int eventId = parseInteger(eventIdText, -1);
        String recordId = childText(event, "EventRecordID");
        String level = severity(parseInteger(childText(event, "Level"), 4));
        String provider = childAttribute(event, "Provider", "Name");
        String timestamp = childAttribute(event, "TimeCreated", "SystemTime");
        String message = firstNonBlank(childText(event, "Message"), eventData(event));
        String sourceIp = extractSourceIp(event, message);
        boolean anomaly = ANOMALOUS_EVENT_IDS.contains(eventId);
        LocalDateTime occurredAt = parseTimestamp(timestamp);
        String id = channel + ":" + firstNonBlank(recordId, UUID.randomUUID().toString()) + ":" + timestamp;
        String eventType = (provider.isBlank() ? channel : provider) + " · ID " + (eventId < 0 ? "N/D" : eventId);
        return new AuditLogEntry(id, occurredAt, level, channel, sourceIp, eventType,
                truncate(message.isBlank() ? "Evento de Windows Event Log sin descripción." : message), anomaly);
    }

    private String extractSourceIp(Element event, String message) {
        NodeList data = event.getElementsByTagNameNS("*", "Data");
        if (data.getLength() == 0) {
            data = event.getElementsByTagName("Data");
        }
        for (int index = 0; index < data.getLength(); index++) {
            Element item = (Element) data.item(index);
            String name = item.getAttribute("Name").toLowerCase(Locale.ROOT);
            if (name.contains("ipaddress") || name.contains("sourceaddress") || name.contains("remoteaddress")) {
                String value = item.getTextContent().strip();
                if (!value.isBlank() && !value.equals("-")) {
                    return value;
                }
            }
        }
        Matcher matcher = IP_PATTERN.matcher(message);
        return matcher.find() ? matcher.group() : "";
    }

    private String eventData(Element event) {
        NodeList data = event.getElementsByTagNameNS("*", "Data");
        if (data.getLength() == 0) {
            data = event.getElementsByTagName("Data");
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < data.getLength(); index++) {
            String value = data.item(index).getTextContent().strip();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return String.join(" · ", values);
    }

    private String childText(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = element.getElementsByTagName(localName);
        }
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }

    private String childAttribute(Element element, String localName, String attribute) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = element.getElementsByTagName(localName);
        }
        return nodes.getLength() == 0 ? "" : ((Element) nodes.item(0)).getAttribute(attribute);
    }

    private String severity(int windowsLevel) {
        return switch (windowsLevel) {
            case 1, 2 -> "CRITICAL";
            case 3 -> "WARNING";
            default -> "INFO";
        };
    }

    private int parseInteger(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (Exception exception) {
            throw new IllegalArgumentException("La marca de tiempo de Windows Event Log no es válida.", exception);
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String truncate(String value) {
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    private String safeError(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record ReadResult(List<AuditLogEntry> entries, String status, String message) {
    }
}
