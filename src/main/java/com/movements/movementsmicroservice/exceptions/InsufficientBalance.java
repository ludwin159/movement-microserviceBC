package com.movements.movementsmicroservice.exceptions;

public class InsufficientBalance extends RuntimeException {
    public InsufficientBalance(String message) {
        super(message);
    }
}
