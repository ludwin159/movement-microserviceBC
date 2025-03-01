package com.movements.movementsmicroservice.service.impl;

import com.movements.movementsmicroservice.DTO.BankAccountDto;
import com.movements.movementsmicroservice.DTO.DebitCardDto;
import com.movements.movementsmicroservice.exceptions.InvalidPayException;
import com.movements.movementsmicroservice.service.PaymentMovementService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class PaymentMovementServiceImp implements PaymentMovementService {
    @Override
    public Mono<BankAccountDto> getBankAccountWithBalanceAvailableForPay(DebitCardDto debitCard, Double amountDebt) {
        Optional<BankAccountDto> principalAccount = getPrincipalBankAccount(debitCard);
        return Mono.justOrEmpty(principalAccount)
                .switchIfEmpty(Mono.error(
                        new InvalidPayException("Principal bank account not exists with id: " +
                                debitCard.getIdPrincipalAccount())))
                .flatMap(principal -> {
                    if (principal.getBalance() < amountDebt) {
                        return Mono.justOrEmpty(
                                        getBankAccountWithGreaterBalanceThanPay(debitCard, amountDebt))
                                .switchIfEmpty(Mono.error(
                                        new InvalidPayException(
                                                "The client does not have bank accounts with available balance")))
                                .flatMap(Mono::just);
                    }
                    return Mono.just(principal);
                });
    }
    private Optional<BankAccountDto> getPrincipalBankAccount(DebitCardDto debitCard) {
        return debitCard.getBankAccounts().stream()
                .filter(bankAccount -> bankAccount.getId().equals(debitCard.getIdPrincipalAccount()))
                .findFirst();
    }
    private Optional<BankAccountDto> getBankAccountWithGreaterBalanceThanPay(DebitCardDto debitCard, Double amount) {
        return debitCard.getBankAccounts().stream()
                .filter(bankAccount -> bankAccount.getBalance() >= amount)
                .findFirst();
    }
}
