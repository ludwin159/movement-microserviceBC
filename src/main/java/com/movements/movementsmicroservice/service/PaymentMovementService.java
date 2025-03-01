package com.movements.movementsmicroservice.service;

import com.movements.movementsmicroservice.DTO.BankAccountDto;
import com.movements.movementsmicroservice.DTO.DebitCardDto;
import reactor.core.publisher.Mono;

public interface PaymentMovementService {
    Mono<BankAccountDto> getBankAccountWithBalanceAvailableForPay(DebitCardDto debitCard, Double amountDebt);
}
