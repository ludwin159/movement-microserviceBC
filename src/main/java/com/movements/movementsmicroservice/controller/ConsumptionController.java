package com.movements.movementsmicroservice.controller;

import com.movements.movementsmicroservice.model.Consumption;
import com.movements.movementsmicroservice.service.ConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/consumptions")
@Tag(name = "Consumos de Tarjeta de Crédito",
        description = "Gestiona los consumos realizados con una tarjeta de crédito.")
public class ConsumptionController {

    private final ConsumptionService consumptionService;

    public ConsumptionController(ConsumptionService consumptionService) {
        this.consumptionService = consumptionService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los consumos", description = "Retorna todos los consumos registrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Consumption.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Flux<Consumption> getAll() {
        return consumptionService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un consumo por ID", description = "Retorna un consumo específico por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Consumption.class))),
            @ApiResponse(responseCode = "404", description = "Consumo no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Consumption> findById(@Parameter(description = "ID del consumo") @PathVariable String id) {
        return consumptionService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo consumo", description = "Crea un nuevo consumo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Consumo creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Consumption.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Consumption> create(@Valid @RequestBody Consumption consumption) {
        return consumptionService.create(consumption);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un consumo por ID", description = "Elimina un consumo por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Consumo eliminado"),
            @ApiResponse(responseCode = "404", description = "Consumo no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Void> deleteById(@Parameter(description = "ID del consumo") @PathVariable String id) {
        return consumptionService.deleteById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un consumo por ID", description = "Actualiza un consumo existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consumo actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Consumption.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Consumo no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Consumption> update(@Parameter(description = "ID del consumo") @PathVariable String id,
                                    @Valid @RequestBody Consumption consumption) {
        return consumptionService.update(id, consumption);
    }

    @GetMapping("/getAllByIdCreditCard/{idCreditCard}")
    @Operation(summary = "Obtener consumos por ID de tarjeta de crédito",
            description = "Retorna todos los consumos asociados a una tarjeta de crédito, ordenados por fecha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Consumption.class))),
            @ApiResponse(responseCode = "404", description = "Tarjeta de crédito o consumos no encontrados"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Flux<Consumption> findAllConsumptionsByIdCreditCardAndSortByDate(
            @Parameter(description = "ID de la tarjeta de crédito") @PathVariable String idCreditCard) {
        return consumptionService.findAllConsumptionsByIdCreditCardAndSortByDate(idCreditCard);
    }

    @GetMapping("/findByIdCreditCardAndBilledFalse/{idCreditCard}")
    public Flux<Consumption> findByIdCreditCardAndBilledFalse(
            @Parameter(description = "ID de la tarjeta de crédito") @PathVariable String idCreditCard) {
        return consumptionService.findByIdCreditCardAndBilledFalse(idCreditCard);
    }
    @PostMapping("/saveAll")
    public Flux<Consumption> saveAll(@Valid @RequestBody List<Consumption> consumptions) {
        return consumptionService.saveAll(consumptions);
    }
}


