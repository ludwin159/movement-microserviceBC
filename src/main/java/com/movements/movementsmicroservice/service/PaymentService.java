package com.movements.movementsmicroservice.service;

import com.movements.movementsmicroservice.model.Payment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentService {
    Flux<Payment> getAll();
    Mono<Payment> findById(String id);
    Mono<Payment> create(Payment payment);
    Mono<Void> deleteById(String id);
    Mono<Payment> update(String id, Payment payment);
    Flux<Payment> findAllPaymentByIdProductCreditAndSortByDate(String idProductCredit);
}
