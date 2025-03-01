package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.exceptions.ServiceNotAvailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreditCardService {
    private final WebClient webClient;
    private final String errorMessage = "Service of Credit card is not working";

    public CreditCardService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice").build();
    }

    @CircuitBreaker(name = "creditCardCircuitBreaker", fallbackMethod = "fallbackFindByIdCreditCard")
    @TimeLimiter(name = "creditCardCircuitBreaker")
    public Mono<CreditCardDto> findById(String id) {
        return webClient.get()
                .uri("/credit-cards/{id}", id)
                .retrieve()
                .bodyToMono(CreditCardDto.class);
    }

    @CircuitBreaker(name = "creditCardCircuitBreaker", fallbackMethod = "fallbackUpdateCreditCard")
    @TimeLimiter(name = "creditCardCircuitBreaker")
    public Mono<CreditCardDto> update(String id, CreditCardDto creditCardDto) {
        return webClient.put()
                .uri("/credit-cards/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(creditCardDto), CreditCardDto.class)
                .retrieve()
                .bodyToMono(CreditCardDto.class);
    }

    @CircuitBreaker(name = "creditCardCircuitBreaker", fallbackMethod = "fallbackAllCreditCardByIdClient")
    @TimeLimiter(name = "creditCardCircuitBreaker")
    public Flux<CreditCardDto> allCreditCardsByIdClientWithPaymentAndConsumption(String idClient) {
        return webClient.get()
                .uri("/credit-cards/findAllByIdClientWithMovements/{idClient}", idClient)
                .retrieve()
                .bodyToFlux(CreditCardDto.class);
    }

    public Mono<CreditCardDto> fallbackFindByIdCreditCard(String id, Throwable error) {
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }

    public Mono<CreditCardDto> fallbackUpdateCreditCard(String id, CreditCardDto creditCardDto, Throwable error) {
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }

    public Flux<CreditCardDto> fallbackAllCreditCardByIdClient(String idClient, Throwable error) {
        return Flux.error(new ServiceNotAvailableException(errorMessage));
    }
}
