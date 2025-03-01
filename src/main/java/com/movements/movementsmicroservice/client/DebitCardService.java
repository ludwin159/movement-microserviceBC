package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.DebitCardDto;
import com.movements.movementsmicroservice.exceptions.DebitCardProblemException;
import com.movements.movementsmicroservice.exceptions.ServiceNotAvailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DebitCardService {
    private final WebClient webClient;
    private final static Logger log = LoggerFactory.getLogger(DebitCardService.class);

    public DebitCardService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice/debit-cards").build();
    }

    @CircuitBreaker(name = "debitCardCircuitBreaker", fallbackMethod = "fallbackFindByIdWithBankAccounts")
    public Mono<DebitCardDto> findByIdWithBankAccountsOrderByCreatedAt(String idDebitCard) {
        return webClient.get()
                .uri("/findByIdWithBankAccountsOrderByCreatedAt/{idDebitCard}", idDebitCard)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        response.bodyToMono(Error.class)
                                .flatMap(error -> Mono.error(new DebitCardProblemException(error.getMessage())))
                )
                .bodyToMono(DebitCardDto.class);
    }

    public Mono<DebitCardDto> fallbackFindByIdWithBankAccounts(String idDebitCard, Throwable error) {
        String errorMessage = "Service of Debit cards is not working";
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }
}
