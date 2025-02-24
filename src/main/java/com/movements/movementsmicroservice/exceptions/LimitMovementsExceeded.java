package com.movements.movementsmicroservice.exceptions;

public class LimitMovementsExceeded extends RuntimeException {
    public LimitMovementsExceeded(String message) {
        super(message);
    }
}
