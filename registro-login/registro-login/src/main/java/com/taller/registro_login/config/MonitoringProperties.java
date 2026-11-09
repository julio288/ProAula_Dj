package com.taller.registro_login.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    private String targets = "";
    private Duration pollInterval = Duration.ofSeconds(30);
    private Duration connectTimeout = Duration.ofSeconds(2);
    private boolean windowsEventLogEnabled = true;
    private List<String> windowsEventLogChannels = new ArrayList<>(List.of("System", "Application", "Security"));

    public String getTargets() {
        return targets;
    }

    public void setTargets(String targets) {
        this.targets = targets;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public boolean isWindowsEventLogEnabled() {
        return windowsEventLogEnabled;
    }

    public void setWindowsEventLogEnabled(boolean windowsEventLogEnabled) {
        this.windowsEventLogEnabled = windowsEventLogEnabled;
    }

    public List<String> getWindowsEventLogChannels() {
        return windowsEventLogChannels;
    }

    public void setWindowsEventLogChannels(List<String> windowsEventLogChannels) {
        this.windowsEventLogChannels = windowsEventLogChannels;
    }
}
