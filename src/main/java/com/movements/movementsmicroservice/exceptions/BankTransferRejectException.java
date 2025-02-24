package com.movements.movementsmicroservice.exceptions;

public class BankTransferRejectException extends RuntimeException {
    public BankTransferRejectException(String message) {
        super(message);
    }
}
