package com.movements.movementsmicroservice.service.impl;

import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.client.CreditService;
import com.movements.movementsmicroservice.exceptions.InvalidPayException;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.DTO.CreditDto;
import com.movements.movementsmicroservice.model.Payment;
import com.movements.movementsmicroservice.repository.PaymentRepository;
import com.movements.movementsmicroservice.service.PaymentService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;


@Service
public class PaymentServiceImp implements PaymentService {
    private final CreditCardService creditCardService;
    private final CreditService creditService;
    private final PaymentRepository paymentRepository;

    public PaymentServiceImp(CreditCardService creditCardService,
                             CreditService creditService,
                             PaymentRepository paymentRepository) {
        this.creditCardService = creditCardService;
        this.creditService = creditService;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Flux<Payment> getAll() {
        return paymentRepository.findAll();
    }

    @Override
    public Mono<Payment> findById(String id) {
        return paymentRepository.findById(id);
    }

    @Override
    public Mono<Payment> create(Payment payment) {
        if (isPayCreditCard(payment)) {
            return findAndPayCreditCard(payment);
        } else if (isPayCredit(payment)) {
            return findAndPayCreditOnly(payment);
        }
        return Mono.error(new InvalidPayException(
                "Type payment " + payment.getTypeCreditProduct() + " not supported"));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment not found.")))
                .then(paymentRepository.deleteById(id));
    }

    private boolean isPayCreditCard(Payment payment) {
        return payment.getTypeCreditProduct() == Payment.TypeCreditProduct.CREDIT_CARD;
    }

    private boolean isPayCredit(Payment payment) {
        return payment.getTypeCreditProduct() == Payment.TypeCreditProduct.CREDIT;
    }

    private Mono<Payment> findAndPayCreditOnly(Payment payment) {
        return creditService.findById(payment.getIdProductCredit())
                .flatMap(credit -> {
                    if (!isPayCreditValid(payment, credit))
                        return Mono.error(new InvalidPayException("You cannot pay more than the debt"));
                    return payCard(payment, credit);
                })
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.error(new ResourceNotFoundException(
                                "The credit card with id: " + payment.getIdProductCredit() + " not exists.")));
    }

    private Mono<Payment> findAndPayCreditCard(Payment payment) {
        return creditCardService.findById(payment.getIdProductCredit())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("The credit card not found.")))
                .flatMap(creditCard -> {
                    if (!isPayValid(payment, creditCard))
                        return Mono.error(new InvalidPayException("You cannot pay more than the debt"));
                    return payCreditCard(payment, creditCard);
                })
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.error(new ResourceNotFoundException(
                                "The credit card with id: " + payment.getIdProductCredit() + " not exists.")));
    }

    private boolean isPayValid(Payment payment, CreditCardDto creditCard) {
        double debtAmount = creditCard.getLimitCredit() - creditCard.getAvailableBalance();
        return payment.getAmount() <= debtAmount;
    }

    private boolean isPayCreditValid(Payment payment, CreditDto credit) {
        return payment.getAmount() <= credit.getPendingBalance();
    }

    private Mono<Payment> payCreditCard(Payment payment, CreditCardDto creditCard) {
        creditCard.setAvailableBalance(creditCard.getAvailableBalance() + payment.getAmount());
        return creditCardService.update(creditCard.getId(), creditCard)
                .then(paymentRepository.save(payment));
    }

    private Mono<Payment>payCard(Payment payment, CreditDto credit) {
        credit.setPendingBalance(credit.getPendingBalance() - payment.getAmount());
        return creditService.update(credit.getId(), credit)
                .then(paymentRepository.save(payment));
    }

    @Override
    public Mono<Payment> update(String id, Payment payment) {
        return paymentRepository.findById(id)
                .flatMap(paymentFound -> {
                    paymentFound.setAmount(payment.getAmount());
                    paymentFound.setDatePayment(payment.getDatePayment());
                    return paymentRepository.save(paymentFound);
                });
    }

    @Override
    public Flux<Payment> findAllPaymentByIdProductCreditAndSortByDate(String idProductCredit) {
        return paymentRepository.findAllByIdProductCredit(idProductCredit)
                .sort(Comparator.comparing(Payment::getDatePayment).reversed());
    }

}
