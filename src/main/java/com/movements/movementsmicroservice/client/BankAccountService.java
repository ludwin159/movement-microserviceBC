package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.BankAccountDto;
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

    public BankAccountService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://appbank-microservice").build();
    }

    public Mono<BankAccountDto> findById(String id) {
        return webClient.get()
                .uri("/bank-accounts/{id}", id)
                .retrieve()
                .bodyToMono(BankAccountDto.class);
    }

    public Mono<BankAccountDto> update(String id, BankAccountDto bankAccountDto) {
        return webClient.put()
                .uri("/bank-accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(bankAccountDto), BankAccountDto.class)
                .retrieve()
                .bodyToMono(BankAccountDto.class);
    }

    public Flux<BankAccountDto> findBankAccountsByIdClientWithAllMovementsSortedByDate(String idClient) {
        return webClient.get()
                .uri("/bank-accounts/findAllByClientWithMovements/{idClient}", idClient)
                .retrieve()
                .bodyToFlux(BankAccountDto.class);
    }

    public Mono<BankAccountDto> findByIdWithoutMovements(String idClient) {
           return webClient.get()
                .uri("/bank-accounts/findByIdWithoutMovements/{idClient}", idClient)
                .retrieve()
                .bodyToMono(BankAccountDto.class);
    }
}
