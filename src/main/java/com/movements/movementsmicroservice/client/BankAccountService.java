package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.BankAccountDto;
import com.movements.movementsmicroservice.exceptions.ServiceNotAvailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BankAccountService {
    private final WebClient webClient;
    private String errorMessage = "Service of Bank accounts is not working";

    public BankAccountService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://appbank-microservice").build();
    }

    @CircuitBreaker(name = "bankAccountCircuitBreaker", fallbackMethod = "fallbackFindByIdBankAccount")
    @TimeLimiter(name = "bankAccountCircuitBreaker")
    public Mono<BankAccountDto> findById(String id) {
        return webClient.get()
                .uri("/bank-accounts/{id}", id)
                .retrieve()
                .bodyToMono(BankAccountDto.class);
    }

    @CircuitBreaker(name = "bankAccountCircuitBreaker", fallbackMethod = "fallbackUpdate")
    @TimeLimiter(name = "bankAccountCircuitBreaker")
    public Mono<BankAccountDto> update(String id, BankAccountDto bankAccountDto) {
        return webClient.put()
                .uri("/bank-accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(bankAccountDto), BankAccountDto.class)
                .retrieve()
                .bodyToMono(BankAccountDto.class);
    }

    @CircuitBreaker(name = "bankAccountCircuitBreaker", fallbackMethod = "fallbackFindBankAccountsByIdClient")
    @TimeLimiter(name = "bankAccountCircuitBreaker")
    public Flux<BankAccountDto> findBankAccountsByIdClientWithAllMovementsSortedByDate(String idClient) {
        return webClient.get()
                .uri("/bank-accounts/findAllByClientWithMovements/{idClient}", idClient)
                .retrieve()
                .bodyToFlux(BankAccountDto.class);
    }

    @CircuitBreaker(name = "bankAccountCircuitBreaker", fallbackMethod = "fallbackFindByIdWithoutMovements")
    @TimeLimiter(name = "bankAccountCircuitBreaker")
    public Mono<BankAccountDto> findByIdWithoutMovements(String idClient) {
           return webClient.get()
                .uri("/bank-accounts/findByIdWithoutMovements/{idClient}", idClient)
                .retrieve()
                .bodyToMono(BankAccountDto.class);
    }
    public Mono<BankAccountDto> fallbackFindByIdBankAccount(String id, Throwable error) {
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }
    public Mono<BankAccountDto> fallbackUpdate(String id, BankAccountDto bankAccountDto, Throwable error) {
        errorMessage = "The update cannot be performed because the service of bank accounts is not available.";
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }
    public Flux<BankAccountDto> fallbackFindBankAccountsByIdClient(String id, Throwable error) {
        return Flux.error(new ServiceNotAvailableException(errorMessage));
    }
    public Mono<BankAccountDto> fallbackFindByIdWithoutMovements(String id, Throwable error) {
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }
}
