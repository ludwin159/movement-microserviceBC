package com.movements.movementsmicroservice.service.impl;

import com.movements.movementsmicroservice.DTO.*;
import com.movements.movementsmicroservice.client.*;
import com.movements.movementsmicroservice.exceptions.InvalidPayException;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.model.Movement;
import com.movements.movementsmicroservice.model.Payment;
import com.movements.movementsmicroservice.repository.PaymentRepository;
import com.movements.movementsmicroservice.service.MovementService;
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
import java.util.Optional;

import static com.movements.movementsmicroservice.model.Payment.TypePayer.*;


@Service
public class PaymentServiceImp implements PaymentService {

    private final static Logger log = LoggerFactory.getLogger(PaymentServiceImp.class);
    private final CreditCardService creditCardService;
    private final CreditService creditService;
    private final PaymentRepository paymentRepository;
    private final DebitCardService debitCardService;
    private final MovementService movementService;

    private final ClientService clientService;
    private final Clock clock;

    public PaymentServiceImp(CreditCardService creditCardService,
                             CreditService creditService,
                             PaymentRepository paymentRepository,
                             ClientService clientService,
                             DebitCardService debitCardService,
                             MovementService movementService,
                             Clock clock) {
        this.creditCardService = creditCardService;
        this.creditService = creditService;
        this.paymentRepository = paymentRepository;
        this.clientService = clientService;
        this.debitCardService = debitCardService;
        this.movementService = movementService;
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
                    if (paymentValid.getTypePayer() == EXTERNAL) {
                        return payTypeExternal(paymentValid);
                    }
                    if (isClientPayer(payment)) {
                        return existingClient(payment)
                                .flatMap(this::payTypeExternal);
                    }
                    if (isDebitCardPayer(payment)) {
                        return payCreditWithDebitCard(payment);
                    }
                    String message = "The payment is not supported";
                    log.error(message);
                    return Mono.error(new InvalidPayException(message));
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

    private Mono<Payment> payTypeExternal(Payment payment) {
        if (isPayCreditCard(payment)) {
            return findAndPayCreditCard(payment);
        } else if (isPayCredit(payment)) {
            return findAndPayCreditOnly(payment);
        }
        return Mono.error(new InvalidPayException(
                "Type payment " + payment.getTypeCreditProduct() + " not supported"));
    }
    private boolean isExternalPayer(Payment payment) {
        return payment.getTypePayer() == EXTERNAL;
    }
    private Mono<Payment> existingClient(Payment payment) {
        return clientService.findById(payment.getIdPayer())
                .flatMap(clientFound -> Mono.just(payment))
                .switchIfEmpty(Mono.error(
                        new InvalidPayException("The client with id: "+payment.getIdPayer()+" not exits.")));
    }
    private boolean isClientPayer(Payment payment) {
        return payment.getTypePayer() == CLIENT;
    }
    private boolean isDebitCardPayer(Payment payment) {
        return payment.getTypePayer() == DEBIT_CARD;
    }

    private Mono<Payment> payCreditWithDebitCard(Payment payment) {
        if (isPayCreditCard(payment)) {
            return Mono.zip(getCreditCardById(payment.getIdProductCredit()),
                    getDebitCardWithAccounts(payment.getIdPayer()))
                    .flatMap(tuple -> {
                        CreditCardDto creditCard = tuple.getT1();
                        DebitCardDto debitCard = tuple.getT2();
                        if (!Objects.equals(creditCard.getTotalDebt(), payment.getAmount())) {
                            return Mono.error(new InvalidPayException("The amount of total debt is: "
                                    + creditCard.getTotalDebt()));
                        }
                        return getBankAccountWithBalanceAvailableForPay(debitCard, creditCard.getTotalDebt())
                                .flatMap(bankAccount ->
                                        doPayFromBankAccount(bankAccount, payment)
                                        .flatMap(movement -> payCreditCard(payment, creditCard)));

                    });
        }
        if (isPayCredit(payment)) {
            return Mono.zip(getCreditOnlyById(payment.getIdProductCredit()),
                    getDebitCardWithAccounts(payment.getIdPayer()))
                    .flatMap(tuple -> {
                        CreditDto credit = tuple.getT1();
                        DebitCardDto debitCard = tuple.getT2();
                        return isPayCreditValid(payment, credit)
                                .flatMap(payment1 ->
                                        getBankAccountWithBalanceAvailableForPay(debitCard,
                                            payment1.getAmount() + payment1.getPenaltyFee())
                                            .flatMap(bankAccount -> doPayFromBankAccount(bankAccount, payment)
                                                       .flatMap(movement -> payCreditOnly(payment1, credit))));
                    });
        }
        String message = "Payments can only be for credit products";
        log.error(message);
        return Mono.error(new InvalidPayException(message));
    }
    private Optional<BankAccountDto> getPrincipalBankAccount(DebitCardDto debitCard) {
        return debitCard.getBankAccounts().stream()
                .filter(bankAccount -> bankAccount.getId().equals(debitCard.getIdPrincipalAccount()))
                .findFirst();
    }

    private Mono<DebitCardDto> getDebitCardWithAccounts(String idDebitCard) {
        return debitCardService.findByIdWithBankAccountsOrderByCreatedAt(idDebitCard)
                .switchIfEmpty(
                        Mono.error(new InvalidPayException("Debit card with id: " + idDebitCard + " not exists.")));
    }

    private Mono<CreditCardDto> getCreditCardById(String idCreditCard) {
        return creditCardService.findById(idCreditCard)
                .onErrorResume(error -> {
                    String message = "Can't get credit card";
                    log.error(message);
                    return Mono.error(new InvalidPayException(message));
                });
    }

    private Mono<CreditDto> getCreditOnlyById(String idCredit) {
        return creditService.findById(idCredit)
                .onErrorResume(error -> {
                    String message = "Can't get credit";
                    log.error(message);
                    return Mono.error(new InvalidPayException(message));
                });
    }

    private Mono<BankAccountDto> getBankAccountWithBalanceAvailableForPay(DebitCardDto debitCard,
                                                                          Double amountDebt) {
        Optional<BankAccountDto> principalAccount = getPrincipalBankAccount(debitCard);
        return Mono.justOrEmpty(principalAccount)
                .switchIfEmpty(Mono.error(
                        new InvalidPayException("Principal bank account not exists with id: " +
                                debitCard.getIdPrincipalAccount())))
                .flatMap(principal -> {
                    if (principal.getBalance() < amountDebt) {
                        return Mono.justOrEmpty(
                                getBankAccountWithGreaterBalanceThanPay(debitCard, amountDebt))
                                .switchIfEmpty(Mono.error(
                                        new InvalidPayException(
                                                "The client does not have bank accounts with available balance")))
                                .flatMap(Mono::just);
                    }
                    return Mono.just(principal);
                });
    }
    private Optional<BankAccountDto> getBankAccountWithGreaterBalanceThanPay(DebitCardDto debitCard, Double amount) {
        return debitCard.getBankAccounts().stream()
                .filter(bankAccount -> bankAccount.getBalance() >= amount)
                .findFirst();
    }

    private Mono<Movement> doPayFromBankAccount(BankAccountDto bankAccount,
                                                Payment payment) {
        Movement withdrawal = new Movement();
        withdrawal.setTypeMovement(Movement.TypeMovement.PAY_CREDIT);
        withdrawal.setIdBankAccount(bankAccount.getId());
        withdrawal.setDescription("Pay with debit card");
        withdrawal.setAmount(payment.getAmount());
        return movementService.create(withdrawal);
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

        payment.setAmount(credit.getMonthlyFee());
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
