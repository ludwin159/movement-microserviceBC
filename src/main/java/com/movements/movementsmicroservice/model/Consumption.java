package com.movements.movementsmicroservice.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Document(collection = "consumptions")
public class Consumption {
    @Id
    private String id;

    @NotNull
    @Indexed
    private String idCreditCard;

    @NotNull
    @Min(0)
    private Double amount;

    @NotNull
    private LocalDateTime dateConsumption;

    @NotNull
    private Integer billingMonth;

    @NotNull
    private Integer billingYear;

    @NotNull
    private Boolean billed;

    @NotNull
    private String description;
    @CreatedDate
    private LocalDateTime createdAt;

    public Consumption() {
        this.dateConsumption = LocalDateTime.now();
        this.billingMonth = dateConsumption.getMonthValue();
        this.billingYear = dateConsumption.getYear();
        this.billed = false;
    }
}
