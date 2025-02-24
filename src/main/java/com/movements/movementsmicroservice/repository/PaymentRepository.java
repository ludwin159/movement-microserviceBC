package com.movements.movementsmicroservice.repository;

import com.movements.movementsmicroservice.model.Payment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface PaymentRepository extends ReactiveMongoRepository<Payment, String> {
    Flux<Payment> findAllByIdProductCredit(String idProductCredit);
}
