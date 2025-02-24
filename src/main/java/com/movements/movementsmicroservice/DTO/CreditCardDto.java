package com.movements.movementsmicroservice.DTO;

import com.movements.movementsmicroservice.model.Payment;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class CreditCardDto {
    private String id;
    private String idClient;
    private Double limitCredit;
    private Double availableBalance;
    private Double interestRate;
    private List<Payment> payments;

    public CreditCardDto() {
        this.payments = new ArrayList<>();
    }
}
