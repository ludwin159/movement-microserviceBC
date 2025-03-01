package com.movements.movementsmicroservice.repository;

import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.model.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ConsumptionRepository extends ReactiveMongoRepository<Consumption, String> {
    Flux<Consumption> findAllByIdCreditCard(String idCreditCard);
    Flux<Consumption> findByIdCreditCardAndBilledFalse(String idCreditCard);
    Flux<Consumption> findByIdCreditCardInOrderByCreatedAtDesc(List<String> idCreditCards);
}
