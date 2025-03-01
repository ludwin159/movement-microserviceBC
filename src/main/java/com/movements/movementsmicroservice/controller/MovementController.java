package com.movements.movementsmicroservice.controller;

import com.movements.movementsmicroservice.model.Movement;
import com.movements.movementsmicroservice.service.MovementService;
import com.movements.movementsmicroservice.utils.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
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
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movements")
@Tag(name = "Movimientos",
        description = "Gestiona los movimientos de las cuentas bancarias como: Depósito(DEPOSIT) o Retiro(WITHDRAWAL)")
public class MovementController {

    private final static Logger log = LoggerFactory.getLogger(MovementController.class);
    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }


    @Operation(
        summary = "Obtener todos los movimientos",
        description = "Devuelve una lista de todos los movimientos realizados en el sistema en las cuentas bancarias")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de movimientos obtenida exitosamente",
                            content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Movement.class))}),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada")
    })
    @GetMapping
    public Flux<Movement> getAll() {
        return movementService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un movimiento por su ID",
            description = "Devuelve un movimiento específico por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Movement.class))}),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
    })
    public Mono<Movement> findById(@PathVariable String id) {
        return movementService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo movimiento",
            description = "Crea un nuevo movimiento en la base de datos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Movimiento creado",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Movement.class))}),
            @ApiResponse(responseCode = "400", description = "Error en la solicitud")
    })
    public Mono<Movement> create(@Valid @RequestBody Movement consumption) {
        return movementService.create(consumption);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un movimiento por su ID",
            description = "Elimina un movimiento de la base de datos por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Movimiento eliminado"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
    })
    public Mono<Void> deleteById(@PathVariable String id) {
        return movementService.deleteById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un movimiento por su ID",
            description = "Actualiza un movimiento existente en la base de datos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movimiento actualizado",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Movement.class))}),
            @ApiResponse(responseCode = "400", description = "Error en la solicitud"),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado")
    })
    public Mono<Movement> update(@PathVariable String id, @Valid @RequestBody Movement currentAccount) {
        return movementService.update(id, currentAccount);
    }

    @GetMapping("/bank-products-by-client/{idClient}")
    @Operation(summary = "Obtiene los productos bancarios de un cliente",
            description = "Devuelve una lista de los productos bancarios asociados a un cliente por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))}),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public Mono<Map<String, List<?>>> getBankProductsByIdClient(@PathVariable String idClient) {
        return movementService.getBankProductsByIdClient(idClient);
    }

    @GetMapping("/getAllByIdBankAccountInPresentMonth/{idBankAccount}")
    @Operation(summary = "Obtiene los movimientos de una cuenta bancaria en el mes actual",
            description = "Devuelve una lista de los movimientos de una cuenta bancaria en el mes actual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Movement.class))}),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada")
    })
    public Flux<Movement> getMovementsByBankAccountIdInPresentMonth(@PathVariable String idBankAccount) {
        return movementService.getMovementsByBankAccountIdInPresentMonth(idBankAccount);
    }

    @GetMapping("/getAllByIdBankAccount/{idBankAccount}")
    @Operation(summary = "Obtiene todos los movimientos de una cuenta bancaria, ordenados por fecha",
            description = "Devuelve una lista de los movimientos de una cuenta bancaria, ordenados por fecha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Movement.class))}),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada")
    })
    public Flux<Movement> getAllMovementsByIdBankAccountAndSortByDate(@PathVariable String idBankAccount) {
        return movementService.getAllMovementsByIdBankAccountAndSortByDate(idBankAccount);
    }

    @GetMapping("/getAllByRangeDate")
    @Operation(summary = "Obtiene los movimientos dentro de un rango de fechas",
            description = "Devuelve una lista de los movimientos dentro de un rango de fechas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Movement.class))}),
            @ApiResponse(responseCode = "400", description = "Error en la solicitud")
    })
    public Flux<Movement> getAllMovementsByRangeDate(@QueryParam("from") String from,
                                                     @QueryParam("to") String to) {
        return movementService.findAllByDateBetween(
                DateUtil.parseDateStringToUtc(from, true),
                DateUtil.parseDateStringToUtc(to, false));
    }

    @PostMapping("/last-ten-by-bank-accounts")
    public Mono<List<Movement>> reportLastTenMovements(@RequestBody List<String> idBankAccounts) {
        log.info("Ingresa a obtener los ultimos 10 movimientos de movimientos");
        return movementService.getDebitMovementsTopTenByBankAccountIds(idBankAccounts);
    }
}
