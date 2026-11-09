package com.taller.registro_login.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class FreightRequestForm {

    @NotNull(message = "Selecciona una ruta disponible.")
    private Long laneId;

    @NotBlank(message = "Describe la carga.")
    @Size(max = 240)
    private String cargoDescription;

    @NotNull(message = "Indica el peso aproximado.")
    @DecimalMin(value = "0.10", message = "El peso mínimo es 0,10 kg.")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal weightKg;

    @NotBlank(message = "Indica quién recibirá la carga.")
    @Size(max = 100)
    private String recipientName;

    @NotBlank(message = "Indica un teléfono de contacto.")
    @Size(max = 30)
    private String recipientPhone;

    @NotBlank(message = "Indica la dirección de recogida.")
    @Size(max = 180)
    private String originAddress;

    @NotBlank(message = "Indica la dirección de entrega.")
    @Size(max = 180)
    private String destinationAddress;

    public Long getLaneId() { return laneId; }
    public void setLaneId(Long laneId) { this.laneId = laneId; }
    public String getCargoDescription() { return cargoDescription; }
    public void setCargoDescription(String cargoDescription) { this.cargoDescription = cargoDescription; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getOriginAddress() { return originAddress; }
    public void setOriginAddress(String originAddress) { this.originAddress = originAddress; }
    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }
}
