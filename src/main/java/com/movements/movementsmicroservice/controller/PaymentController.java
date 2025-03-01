package com.movements.movementsmicroservice.controller;

import com.movements.movementsmicroservice.model.Payment;
import com.movements.movementsmicroservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/payments")
@Tag(name = "Pagos de Productos de Crédito",
        description = "Gestiona los pagos de productos de crédito (tarjetas de crédito y créditos).")
public class PaymentController {

    private final PaymentService paymentService;
    private final static Logger log = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los pagos", description = "Retorna todos los pagos registrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Payment.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Flux<Payment> getAll() {
        return paymentService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pago por ID", description = "Retorna un pago específico por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Payment.class))),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Payment> findById(@Parameter(description = "ID del pago") @PathVariable String id) {
        return paymentService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo pago", description = "Crea un nuevo pago.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pago creado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Payment.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Payment> create(@Valid @RequestBody Payment payment) {
        return paymentService.create(payment);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un pago por ID", description = "Elimina un pago por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pago eliminado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Void> deleteById(@Parameter(description = "ID del pago") @PathVariable String id) {
        return paymentService.deleteById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un pago por ID", description = "Actualiza un pago existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago actualizado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Payment.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Payment> update(
            @Parameter(description = "ID del pago") @PathVariable String id, @Valid @RequestBody Payment payment) {
        return paymentService.update(id, payment);
    }

    @GetMapping("/getAllByIdProductCredit/{idProductCredit}")
    @Operation(summary = "Obtener pagos por ID de producto de crédito",
            description = "Retorna todos los pagos asociados a un producto de crédito, ordenados por fecha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Payment.class))),
            @ApiResponse(responseCode = "404", description = "Producto de crédito o pagos no encontrados"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Flux<Payment> getAllByIdProductCredit(
            @Parameter(description = "ID del producto de crédito") @PathVariable String idProductCredit) {
        return paymentService.findAllPaymentByIdProductCreditAndSortByDate(idProductCredit);
    }

    @PostMapping("/get-last-ten-payments")
    public Mono<List<Payment>> lastTenPaymentsByIdCreditCard(@RequestBody List<String> idCreditCards) {
        log.info("Ingresa a obtener los ultimos 10 movimientos de pagos");
        return paymentService.findLastTenPaymentsByIdCredit(idCreditCards)
                .map(response -> {
                    log.info("Obteniendo respuesta para devolver de pagos: " + response.size());
                    log.info(response.toString());
                    return response;
                });
    }
}
