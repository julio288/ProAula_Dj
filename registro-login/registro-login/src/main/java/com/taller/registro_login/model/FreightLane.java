package com.taller.registro_login.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Entity
@Table(name = "freight_lanes")
public class FreightLane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String origin;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String destination;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String schedule;

    @Positive
    @Column(name = "transit_days", nullable = false)
    private int transitDays;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "price_per_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerKg;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "minimum_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumPrice;

    @Column(nullable = false)
    private boolean active = true;

    public FreightLane() {
    }

    public FreightLane(String origin, String destination, String schedule, int transitDays,
                       BigDecimal pricePerKg, BigDecimal minimumPrice) {
        this.origin = origin;
        this.destination = destination;
        this.schedule = schedule;
        this.transitDays = transitDays;
        this.pricePerKg = pricePerKg;
        this.minimumPrice = minimumPrice;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public int getTransitDays() { return transitDays; }
    public void setTransitDays(int transitDays) { this.transitDays = transitDays; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(BigDecimal pricePerKg) { this.pricePerKg = pricePerKg; }
    public BigDecimal getMinimumPrice() { return minimumPrice; }
    public void setMinimumPrice(BigDecimal minimumPrice) { this.minimumPrice = minimumPrice; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
