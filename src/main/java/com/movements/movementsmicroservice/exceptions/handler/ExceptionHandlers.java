package com.movements.movementsmicroservice.exceptions.handler;

import com.movements.movementsmicroservice.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler({
            ResourceNotFoundException.class,
            LimitMovementsExceeded.class,
            InsufficientBalance.class,
            UnsupportedMovementException.class,
            InvalidPayException.class,
            ConsumeNotValidException.class,
            BankTransferRejectException.class,
            RuntimeException.class,
            ServiceNotAvailableException.class
    })
    public Mono<ResponseEntity<Map<String, String>>> handleExceptions(RuntimeException exception) {
        HttpStatus status = getStatus(exception);
        return Mono.just(ResponseEntity.status(status).body(
                Map.of("message", exception.getMessage())
        ));
    }

    public HttpStatus getStatus(RuntimeException exception) {
        if (exception instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (exception instanceof LimitMovementsExceeded || exception instanceof InsufficientBalance ||
                exception instanceof UnsupportedMovementException || exception instanceof InvalidPayException ||
                exception instanceof ConsumeNotValidException || exception instanceof BankTransferRejectException||
                exception instanceof DebitCardProblemException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof ServiceNotAvailableException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<Map<String, String>> HandleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errorsResponse = new HashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errorsResponse.put(error.getField(), error.getDefaultMessage()));
        return Mono.just(errorsResponse);
    }
}
