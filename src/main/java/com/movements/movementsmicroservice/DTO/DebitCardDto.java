package com.movements.movementsmicroservice.DTO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DebitCardDto {
    private String id;
    private String idClient;
    private String idPrincipalAccount;
    private List<BankAccountDto> bankAccounts;
    public DebitCardDto() {
        this.bankAccounts = new ArrayList<>();
    }
}
