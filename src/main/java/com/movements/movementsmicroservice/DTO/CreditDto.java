package com.movements.movementsmicroservice.DTO;

import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.model.Payment;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
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

    private LocalDate disbursementDate;
    private LocalDate firstDatePay;
    private Integer totalMonths;
    private Double monthlyFee;

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
                     TypeCredit typeCredit,
                     LocalDate disbursementDate,
                     LocalDate firstDatePay,
                     Integer totalMonths,
                     Double monthlyFee) {
        this.id = id;
        this.idClient = idClient;
        this.totalAmount = totalAmount;
        this.pendingBalance = pendingBalance;
        this.interestRate = interestRate;
        this.typeCredit = typeCredit;
        this.disbursementDate = disbursementDate;
        this.firstDatePay = firstDatePay;
        this.totalMonths = totalMonths;
        this.monthlyFee = monthlyFee;
        this.payments = new ArrayList<>();
    }
}
