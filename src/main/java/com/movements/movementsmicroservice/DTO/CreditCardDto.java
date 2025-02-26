package com.movements.movementsmicroservice.DTO;

import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.model.Payment;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
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
    private Double totalDebt;
    private LocalDate billingDate;
    private Integer numberBillingDate;
    private Integer numberDueDate;
    private LocalDate dueDate;
    private List<Consumption> consumptions;
    private List<Payment> payments;
//    @Transient
//    private List<Billing> billings;

    public CreditCardDto() {
        this.payments = new ArrayList<>();
        this.consumptions = new ArrayList<>();
    }
}
