package com.taller.registro_login.dto;

import java.time.LocalDateTime;

public class NetworkServiceStatus {

    private String name;
    private String protocol;
    private String host;
    private int port;
    private boolean reachable;
    private long latencyMs;
    private String statusLabel; // "OPERACIONAL", "FALLO", "ADVERTENCIA"
    private String details;
    private LocalDateTime checkedAt;

    public NetworkServiceStatus() {
    }

    public NetworkServiceStatus(String name, String protocol, String host, int port, boolean reachable, long latencyMs, String statusLabel, String details) {
        this.name = name;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.reachable = reachable;
        this.latencyMs = latencyMs;
        this.statusLabel = statusLabel;
        this.details = details;
        this.checkedAt = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
