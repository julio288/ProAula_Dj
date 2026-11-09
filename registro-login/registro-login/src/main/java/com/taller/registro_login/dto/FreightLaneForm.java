package com.taller.registro_login.dto;

import com.taller.registro_login.model.FreightLane;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class FreightLaneForm {

    @NotBlank
    @Size(max = 80)
    private String origin;

    @NotBlank
    @Size(max = 80)
    private String destination;

    @NotBlank
    @Size(max = 100)
    private String schedule;

    @Positive
    private int transitDays = 1;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal pricePerKg;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal minimumPrice;

    public static FreightLaneForm from(FreightLane lane) {
        FreightLaneForm form = new FreightLaneForm();
        form.setOrigin(lane.getOrigin());
        form.setDestination(lane.getDestination());
        form.setSchedule(lane.getSchedule());
        form.setTransitDays(lane.getTransitDays());
        form.setPricePerKg(lane.getPricePerKg());
        form.setMinimumPrice(lane.getMinimumPrice());
        return form;
    }

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
}
