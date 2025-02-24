package com.movements.movementsmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MovementsmicroserviceApplication {

    public static void main(String[] args) {
         SpringApplication.run(MovementsmicroserviceApplication.class, args);
    }

}
