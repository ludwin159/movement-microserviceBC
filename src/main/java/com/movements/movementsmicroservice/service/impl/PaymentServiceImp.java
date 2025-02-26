package com.movements.movementsmicroservice.service.impl;

import com.movements.movementsmicroservice.DTO.BankAccountDto;
import com.movements.movementsmicroservice.client.BankAccountService;
import com.movements.movementsmicroservice.client.ClientService;
import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.client.CreditService;
import com.movements.movementsmicroservice.exceptions.InvalidPayException;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.DTO.CreditDto;
import com.movements.movementsmicroservice.model.Payment;
import com.movements.movementsmicroservice.repository.PaymentRepository;
import com.movements.movementsmicroservice.service.PaymentService;
import com.movements.movementsmicroservice.utils.Numbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;

import static com.movements.movementsmicroservice.model.Payment.TypePayer.*;


@Service
public class PaymentServiceImp implements PaymentService {

    private final static Logger log = LoggerFactory.getLogger(PaymentServiceImp.class);
    private final CreditCardService creditCardService;
    private final CreditService creditService;
    private final PaymentRepository paymentRepository;
    private final BankAccountService bankAccountService;

    private final ClientService clientService;
    private final Clock clock;

    public PaymentServiceImp(CreditCardService creditCardService,
                             CreditService creditService,
                             PaymentRepository paymentRepository,
                             ClientService clientService,
                             BankAccountService bankAccountService,
                             Clock clock) {
        this.creditCardService = creditCardService;
        this.creditService = creditService;
        this.paymentRepository = paymentRepository;
        this.clientService = clientService;
        this.bankAccountService = bankAccountService;
        this.clock = clock;
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
        return validPayment(payment)
                .flatMap(paymentValid -> {

                    if (isPayCreditCard(paymentValid)) {
                        return findAndPayCreditCard(paymentValid);
                    } else if (isPayCredit(paymentValid)) {
                        return findAndPayCreditOnly(paymentValid);
                    }
                    return Mono.error(new InvalidPayException(
                            "Type payment " + paymentValid.getTypeCreditProduct() + " not supported"));
                });
    }

    private Mono<Payment> validPayment(Payment payment) {
        String messageError = "";
        if (isExternalPayer(payment) && !payment.getIdPayer().isEmpty()) {
            messageError = "If your payment is external, you cannot have a payer id '" + payment.getIdPayer() + "'";
            log.error(messageError);
        }
        if (!isExternalPayer(payment) && payment.getIdPayer().isEmpty()) {
            messageError = "If the payment is internal, it must have a payer ID: '" + payment.getIdPayer() + "'";
            log.error(messageError);
        }
        if (!messageError.isEmpty())
            return Mono.error(new InvalidPayException(messageError));
        return Mono.just(payment);
    }

    private boolean isExternalPayer(Payment payment) {
        return payment.getTypePayer() == EXTERNAL;
    }
    private Mono<Payment> validClientAndBankAccount(Payment payment) {
        if (isClientPayer(payment)) {
            return clientService.findById(payment.getIdPayer())
                    .flatMap(clientFound -> Mono.just(payment))
                    .switchIfEmpty(Mono.error(
                            new InvalidPayException("The client with id: "+payment.getIdPayer()+" not exits.")));
        }
//        if (isBankAccountPayer(payment)) {
//            return bankAccountService.findByIdWithoutMovements(payment.getIdPayer())
//                    .flatMap(bankAccount -> )
//        }
        return Mono.just(payment);
    }
    private boolean isClientPayer(Payment payment) {
        return payment.getTypePayer() == CLIENT;
    }
    private boolean isBankAccountPayer(Payment payment) {
        return payment.getTypePayer() == BANK_ACCOUNT;
    }

//    private Mono<Payment> payCreditWithBankAccount() {
//
//    }

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
                .flatMap(credit -> isPayCreditValid(payment, credit)
                            .flatMap(payment1 -> payCreditOnly(payment1, credit)))
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.error(new ResourceNotFoundException(
                                "The credit card with id: " + payment.getIdProductCredit() + " not exists.")));
    }

    private Mono<Payment> isPayCreditValid(Payment payment, CreditDto credit) {
        if (credit.getPendingBalance() == 0) {
            return Mono.error(new InvalidPayException("The payment has already been canceled."));
        }

        LocalDate dateNewPayment = payment.getDatePayment().toLocalDate();
        int monthPay = dateNewPayment.getMonthValue();
        int yearPay = dateNewPayment.getYear();

        boolean paymentExisting = credit.getPayments().stream()
                .anyMatch(p -> p.getMonthCorresponding() == monthPay && p.getYearCorresponding() == yearPay);

        if (paymentExisting) {
            return Mono.error(new InvalidPayException("A payment has already been posted for this month."));
        }

        LocalDate expectedPaymentDate = getDateLimitExpected(credit, monthPay, yearPay);

        if (dateNewPayment.isBefore(expectedPaymentDate.minusDays(5))) {
            return Mono.error(new RuntimeException("You cannot pay future payments."));
        }

        double penaltyFee = 0;
        if (dateNewPayment.isAfter(expectedPaymentDate)) {
            long daysLate = ChronoUnit.DAYS.between(expectedPaymentDate, dateNewPayment);
            penaltyFee = calculatePenalty(credit, daysLate);
        }

        double totalToPay = Numbers.redondear(credit.getMonthlyFee() + penaltyFee);
        if (!Objects.equals(payment.getAmount(), totalToPay)) {
            return Mono.error(new InvalidPayException("The payment must be exactly: " + totalToPay));
        }


        payment.setPenaltyFee(penaltyFee);
        return Mono.just(payment);
    }
    private LocalDate getDateLimitExpected(CreditDto credit, int month, int year) {
        LocalDate today = LocalDate.now(clock);
        if (credit.getFirstDatePay().isAfter(today)) {
            return credit.getFirstDatePay();
        }
        LocalDate firstPaymentDate = credit.getFirstDatePay();
        return LocalDate.of(year, month, firstPaymentDate.getDayOfMonth());
    }

    private double calculatePenalty(CreditDto credit, long daysLate) {
        double dailyRate = credit.getInterestRate() / 30;
        return credit.getMonthlyFee() * dailyRate * daysLate;
    }


    private Mono<Payment> findAndPayCreditCard(Payment payment) {
        return creditCardService.findById(payment.getIdProductCredit())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("The credit card not found.")))
                .flatMap(creditCard -> {
                    if (!Objects.equals(payment.getAmount(), creditCard.getTotalDebt()))
                        return Mono.error(new InvalidPayException("The total amount is: " + creditCard.getTotalDebt()));
                    return payCreditCard(payment, creditCard);
                })
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.error(new ResourceNotFoundException(
                                "The credit card with id: " + payment.getIdProductCredit() + " not exists.")));
    }

    private Mono<Payment> payCreditCard(Payment payment, CreditCardDto creditCard) {
        creditCard.setTotalDebt(creditCard.getTotalDebt() - payment.getAmount());
        creditCard.setAvailableBalance(creditCard.getAvailableBalance() + payment.getAmount());
        return creditCardService.update(creditCard.getId(), creditCard)
                .then(paymentRepository.save(payment));
    }

    private Mono<Payment> payCreditOnly(Payment payment, CreditDto credit) {
        credit.setPendingBalance(credit.getPendingBalance() - payment.getAmount());
        payment.setMonthCorresponding(payment.getDatePayment().getMonthValue());
        payment.setYearCorresponding(payment.getDatePayment().getYear());
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
