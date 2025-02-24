package com.movements.movementsmicroservice.DTO;

import com.movements.movementsmicroservice.model.Movement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class BankAccountDto {
    private String id;
    private String idClient;
    private Double balance;
    private TypeBankAccount typeBankAccount;
    private Integer limitMovements;
    private Integer expirationDate;
    private Double maintenanceCost;
    private Integer maxTransactions;
    private Double commissionPercentage;
    private Double minimumDailyAverageAmount;

    private List<String> authorizedSignatorits;
    private List<String> accountHolders;
    private List<Movement> movements;

    public BankAccountDto() {
        this.movements = new ArrayList<>();
    }
    public BankAccountDto(String idClient,
                       Double balance,
                       TypeBankAccount typeBankAccount,
                       Integer limitMovements,
                       Integer expirationDate,
                       Double maintenanceCost,
                       Double commissionPercentage,
                       Double minimumDailyAverageAmount,
                       Integer maxTransactions,
                       List<String> authorizedSignatorits,
                       List<String> accountHolders) {
        this.idClient = idClient;
        this.balance = balance;
        this.typeBankAccount = typeBankAccount;
        this.limitMovements = limitMovements;
        this.expirationDate = expirationDate;
        this.maintenanceCost = maintenanceCost;
        this.minimumDailyAverageAmount = minimumDailyAverageAmount;
        this.commissionPercentage = commissionPercentage;
        this.maxTransactions = maxTransactions;
        this.authorizedSignatorits = authorizedSignatorits;
        this.accountHolders = accountHolders;
        this.movements = new ArrayList<>();
    }

    public enum TypeBankAccount {
        SAVING_ACCOUNT, CURRENT_ACCOUNT, FIXED_TERM_ACCOUNT
    }
}
