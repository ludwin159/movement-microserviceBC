package com.movements.movementsmicroservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {
    private String id;

    @NotNull
    @Min(0)
    private Double amount;

    @NotNull
    @NotBlank
    @Size(min = 8)
    private String idProductCredit;

    @NotNull
    private LocalDateTime datePayment;

    @NotNull
    private TypeCreditProduct typeCreditProduct;

    private Integer monthCorresponding;

    private Integer yearCorresponding;

    private Double penaltyFee;

    @NotNull
    @Size(min = 8)
    private String idPayer;
    @NotNull
    private TypePayer typePayer;

    public enum TypeCreditProduct {
        CREDIT_CARD, CREDIT, UNSUPPORTED
    }
    public enum TypePayer {
        EXTERNAL, CLIENT, DEBIT_CARD
    }
    public Payment() {
        this.datePayment = LocalDateTime.now();
    }
}
