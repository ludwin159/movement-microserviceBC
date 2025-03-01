package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.ClientDto;
import com.movements.movementsmicroservice.DTO.CreditDto;
import com.movements.movementsmicroservice.exceptions.ServiceNotAvailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ClientService {
    private final WebClient webClient;
    private String errorMessage = "Service of Clients is not working";

    public ClientService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice").build();
    }

    @CircuitBreaker(name = "clientCircuitBreaker", fallbackMethod = "fallbackFindByIdClient")
    @TimeLimiter(name = "clientCircuitBreaker")
    public Mono<ClientDto> findById(String id) {
        return webClient.get()
                .uri("/client/{id}", id)
                .retrieve()
                .bodyToMono(ClientDto.class);
    }

    public Mono<String> fallbackFindByIdClient(String id, Throwable error) {
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }
}
