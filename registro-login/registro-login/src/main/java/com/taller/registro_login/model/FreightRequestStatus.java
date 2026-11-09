package com.taller.registro_login.model;

public enum FreightRequestStatus {
    RECEIVED("Recibida"),
    QUOTED("Cotizada"),
    IN_TRANSIT("En tránsito"),
    DELIVERED("Entregada"),
    CANCELLED("Cancelada");

    private final String label;

    FreightRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
