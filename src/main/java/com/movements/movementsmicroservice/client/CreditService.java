package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.DTO.CreditDto;
import com.movements.movementsmicroservice.exceptions.ServiceNotAvailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreditService {
    private final WebClient webClient;
    private final String errorMessage = "Service of Credits is not working";

    public CreditService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice").build();
    }

    @CircuitBreaker(name = "creditCircuitBreaker", fallbackMethod = "fallbackFindById")
    @TimeLimiter(name = "creditCircuitBreaker")
    public Mono<CreditDto> findById(String id) {
        return webClient.get()
                .uri("/credits/{id}", id)
                .retrieve()
                .bodyToMono(CreditDto.class);
    }

    @CircuitBreaker(name = "creditCircuitBreaker", fallbackMethod = "fallbackUpdateCredit")
    @TimeLimiter(name = "creditCircuitBreaker")
    public Mono<CreditDto> update(String id, CreditDto creditCardDto) {
        return webClient.put()
                .uri("/credits/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(creditCardDto)
                .retrieve()
                .bodyToMono(CreditDto.class);
    }

    @CircuitBreaker(name = "creditCircuitBreaker", fallbackMethod = "fallbackAllCreditsByIdClient")
    @TimeLimiter(name = "creditCircuitBreaker")
    public Flux<CreditDto> allCreditsByIdClientWithAllPaymentsSortedByDatePayment(String idClient) {
        return webClient.get()
                .uri("/credits/findAllByClientWithPayments/{idClient}", idClient)
                .retrieve()
                .bodyToFlux(CreditDto.class);
    }

    public Mono<CreditDto> fallbackFindById(String id, Throwable error) {
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }
    public Mono<CreditDto> fallbackUpdateCredit(String id, CreditDto creditCardDto, Throwable error) {
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }
    public Flux<CreditDto> fallbackAllCreditsByIdClient(String idClient, Throwable error) {
        return Flux.error(new ServiceNotAvailableException(errorMessage));
    }

}
