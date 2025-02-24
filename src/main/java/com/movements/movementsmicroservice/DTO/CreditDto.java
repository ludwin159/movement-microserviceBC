package com.movements.movementsmicroservice.DTO;

import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.model.Payment;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class CreditDto {
    private String id;
    private String idClient;
    private Double totalAmount;
    private Double pendingBalance;
    private Double interestRate;
    private TypeCredit typeCredit;
    private List<Consumption> consumptions;
    private List<Payment> payments;

    public enum TypeCredit {
        PERSONAL_CREDIT, BUSINESS_CREDIT
    }

    public CreditDto() {
        this.payments = new ArrayList<>();
        this.consumptions = new ArrayList<>();
    }
    public CreditDto(String id, String idClient,
                     Double totalAmount,
                     Double pendingBalance,
                     Double interestRate,
                     TypeCredit typeCredit) {
        this.id = id;
        this.idClient = idClient;
        this.totalAmount = totalAmount;
        this.pendingBalance = pendingBalance;
        this.interestRate = interestRate;
        this.typeCredit = typeCredit;
        this.payments = new ArrayList<>();
        this.consumptions = new ArrayList<>();
    }
}
