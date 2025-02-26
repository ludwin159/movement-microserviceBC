package com.movements.movementsmicroservice.service;

import com.movements.movementsmicroservice.model.Consumption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ConsumptionService {
    public Flux<Consumption> getAll();
    public Mono<Consumption> findById(String id);
    public Mono<Consumption> create(Consumption document);
    public Mono<Void> deleteById(String id);
    Mono<Consumption> update(String id, Consumption consumption);
    Flux<Consumption> findAllConsumptionsByIdCreditCardAndSortByDate(String id);
    Flux<Consumption> findByIdCreditCardAndBilledFalse(String idCreditCard);
    Flux<Consumption> saveAll(List<Consumption> consumptions);
}
