package com.movements.movementsmicroservice.repository;

import com.movements.movementsmicroservice.model.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface MovementRepository extends ReactiveMongoRepository<Movement, String> {
    Flux<Movement> findAllByIdBankAccountAndDateBetween(String bankAccountId, LocalDateTime from, LocalDateTime to);
    Flux<Movement> findAllByDateBetween(LocalDateTime from, LocalDateTime to);
    Flux<Movement> findAllByIdBankAccount(String bankAccountId);
}
