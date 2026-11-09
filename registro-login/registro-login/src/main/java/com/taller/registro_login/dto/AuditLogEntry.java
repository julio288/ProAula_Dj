package com.taller.registro_login.dto;

import java.time.LocalDateTime;

public class AuditLogEntry {

    private String id;
    private LocalDateTime timestamp;
    private String level; // "INFO", "WARNING", "CRITICAL"
    private String service; // "SSH", "DNS", "FTP", "FIREWALL", "AUTH"
    private String sourceIp;
    private String eventType;
    private String message;
    private boolean anomaly;

    public AuditLogEntry() {
    }

    public AuditLogEntry(String id, LocalDateTime timestamp, String level, String service, String sourceIp, String eventType, String message, boolean anomaly) {
        this.id = id;
        this.timestamp = timestamp;
        this.level = level;
        this.service = service;
        this.sourceIp = sourceIp;
        this.eventType = eventType;
        this.message = message;
        this.anomaly = anomaly;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAnomaly() {
        return anomaly;
    }

    public void setAnomaly(boolean anomaly) {
        this.anomaly = anomaly;
    }
}
