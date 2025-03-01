package com.movements.movementsmicroservice.service;

import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.model.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MovementService {
    Mono<Movement> create(Movement movement);
    Mono<Movement> update(String id, Movement client);
    Flux<Movement> getAll();
    Mono<Movement> findById(String id);
    Mono<Void> deleteById(String id);
    Flux<Movement> getMovementsByBankAccountIdInPresentMonth(String bankAccountId);
    Flux<Movement> getMovementsByBankAccountIdAndDateRangeAndSort(String id, LocalDateTime from, LocalDateTime to);
    Flux<Movement> getAllMovementsByIdBankAccountAndSortByDate(String bankAccountId);
    Flux<Movement> findAllByDateBetween(LocalDateTime from, LocalDateTime to);
    Mono<Map<String, List<?>>> getBankProductsByIdClient(String idClient);
    Mono<List<Movement>> getDebitMovementsTopTenByBankAccountIds(List<String> idBankAccounts);
}
