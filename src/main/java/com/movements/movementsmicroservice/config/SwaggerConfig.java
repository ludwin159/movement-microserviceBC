package com.movements.movementsmicroservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "API's for microservice Bank Movements",
                version = "v1.0.0",
                description = "This microservice is for manage the movements",
                contact = @Contact(
                        name = "Ludwin J. Suárez",
                        email = "lsuarein@emeal.nttdata.com"
                )
        )
)
public class SwaggerConfig {}
