package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.DebitCardDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DebitCardService {
    private final WebClient webClient;
    public DebitCardService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice").build();
    }

    public Mono<DebitCardDto> findByIdWithBankAccountsOrderByCreatedAt(String idDebitCard) {
        return webClient.get()
                .uri("/findByIdWithBankAccountsOrderByCreatedAt/{idDebitCard}", idDebitCard)
                .retrieve()
                .bodyToMono(DebitCardDto.class);
    }
}
