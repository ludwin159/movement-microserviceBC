package com.movements.movementsmicroservice.repository;

import com.movements.movementsmicroservice.model.Movement;
import com.movements.movementsmicroservice.model.Payment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface PaymentRepository extends ReactiveMongoRepository<Payment, String> {
    Flux<Payment> findAllByIdProductCredit(String idProductCredit);
    Flux<Payment> findByIdProductCreditInOrderByCreatedAtDesc(List<String> idCreditCards);
}
