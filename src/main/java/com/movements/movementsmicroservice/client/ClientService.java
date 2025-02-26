package com.movements.movementsmicroservice.client;

import com.movements.movementsmicroservice.DTO.ClientDto;
import com.movements.movementsmicroservice.DTO.CreditDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ClientService {
    private final WebClient webClient;

    public ClientService(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://appbank-microservice").build();
    }

    public Mono<ClientDto> findById(String id) {
        return webClient.get()
                .uri("/client/{id}", id)
                .retrieve()
                .bodyToMono(ClientDto.class);
    }
}
