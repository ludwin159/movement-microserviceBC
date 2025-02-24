package com.movements.movementsmicroservice.repository;

import com.movements.movementsmicroservice.model.Consumption;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ConsumptionRepository extends ReactiveMongoRepository<Consumption, String> {
    Flux<Consumption> findAllByIdCreditCard(String idCreditCard);
}
