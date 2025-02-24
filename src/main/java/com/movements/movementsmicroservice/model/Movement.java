package com.movements.movementsmicroservice.model;


import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Document(collection = "movements")
public class Movement {
    @Id
    private String id;

    @NotNull
    private LocalDateTime date;

    @NotNull
    private TypeMovement typeMovement;

    @NotNull
    @Min(0)
    private Double amount;

    @NotBlank
    private String description;

    @Min(0)
    @NotNull
    private Double commissionAmount;

    @NotNull
    private String idBankAccount;

    @NotNull
    private String idBankAccountTransfer;

    private String idTransfer;


    public static enum TypeMovement {
        DEPOSIT, WITHDRAWAL, TRANSFER, UNSUPPORTED
    }

    public Movement() {
        this.commissionAmount = 0.0;
        this.date = LocalDateTime.now();
    }
}
