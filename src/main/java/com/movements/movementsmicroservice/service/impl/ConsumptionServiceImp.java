package com.movements.movementsmicroservice.service.impl;

import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.exceptions.ConsumeNotValidException;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.repository.ConsumptionRepository;
import com.movements.movementsmicroservice.service.ConsumptionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Service
public class ConsumptionServiceImp implements ConsumptionService {

    private final ConsumptionRepository consumptionRepository;
    private final CreditCardService creditCardService;
    public ConsumptionServiceImp(ConsumptionRepository consumptionRepository,
                                 CreditCardService creditCardService) {
        this.consumptionRepository = consumptionRepository;
        this.creditCardService = creditCardService;
    }


    @Override
    public Flux<Consumption> getAll() {
        return consumptionRepository.findAll();
    }

    @Override
    public Mono<Consumption> findById(String id) {
        return consumptionRepository.findById(id);
    }

    @Override
    public Mono<Consumption> create(Consumption consumption) {
        return creditCardService.findById(consumption.getIdCreditCard())
                .flatMap(creditCardFound -> {
                    if (!isConsumeCreditCardValid(consumption, creditCardFound)) {
                        return Mono.error(
                                new ConsumeNotValidException("Consumption is greater than the available balance."));
                    }
                    return loadConsumptionToCreditCard(consumption, creditCardFound);
                });
    }

    private boolean isConsumeCreditCardValid(Consumption consumption, CreditCardDto creditCard) {
        return consumption.getAmount() <= creditCard.getAvailableBalance();
    }
    private Mono<Consumption> loadConsumptionToCreditCard(Consumption consumption, CreditCardDto creditCard) {
        creditCard.setAvailableBalance(creditCard.getAvailableBalance() - consumption.getAmount());
        return creditCardService.update(creditCard.getId(), creditCard)
                .then(consumptionRepository.save(consumption));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return consumptionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Consumption not found.")))
                .then(consumptionRepository.deleteById(id));
    }

    @Override
    public Mono<Consumption> update(String id, Consumption consumption) {
        return consumptionRepository.findById(id)
                .flatMap(consumptionFound -> {
                    consumptionFound.setAmount(consumption.getAmount());
                    consumptionFound.setDateConsumption(consumption.getDateConsumption());
                    consumptionFound.setDescription(consumption.getDescription());
                    return consumptionRepository.save(consumptionFound);
                });
    }

    @Override
    public Flux<Consumption> findAllConsumptionsByIdCreditCardAndSortByDate(String id) {
        return consumptionRepository.findAllByIdCreditCard(id)
                .sort(Comparator.comparing(Consumption::getDateConsumption).reversed());
    }

    @Override
    public Flux<Consumption> findByIdCreditCardAndBilledFalse(String idCreditCard) {
        return consumptionRepository.findByIdCreditCardAndBilledFalse(idCreditCard);
    }

    @Override
    public Flux<Consumption> saveAll(List<Consumption> consumptions) {
        return consumptionRepository.saveAll(consumptions);
    }
}
