package com.movements.movementsmicroservice.exceptions;

public class UnsupportedMovementException extends RuntimeException {
    public UnsupportedMovementException(String message) {
        super(message);
    }
}
