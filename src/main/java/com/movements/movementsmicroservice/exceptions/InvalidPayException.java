package com.movements.movementsmicroservice.exceptions;

public class InvalidPayException extends RuntimeException {
    public InvalidPayException(String message) {
        super(message);
    }
}
