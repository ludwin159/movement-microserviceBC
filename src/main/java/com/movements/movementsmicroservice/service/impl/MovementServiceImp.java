package com.movements.movementsmicroservice.service.impl;

import com.movements.movementsmicroservice.client.BankAccountService;
import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.client.CreditService;
import com.movements.movementsmicroservice.exceptions.InsufficientBalance;
import com.movements.movementsmicroservice.exceptions.LimitMovementsExceeded;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.exceptions.UnsupportedMovementException;
import com.movements.movementsmicroservice.DTO.BankAccountDto;
import com.movements.movementsmicroservice.DTO.CreditCardDto;
import com.movements.movementsmicroservice.DTO.CreditDto;
import com.movements.movementsmicroservice.model.Movement;
import com.movements.movementsmicroservice.repository.MovementRepository;
import com.movements.movementsmicroservice.service.MovementService;
import com.movements.movementsmicroservice.utils.DateToPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;


import static com.movements.movementsmicroservice.model.Movement.TypeMovement.*;

@Service
public class MovementServiceImp implements MovementService {

    private static final Logger log = LoggerFactory.getLogger(MovementServiceImp.class);
    private final CreditService creditService;
    private final CreditCardService creditCardService;
    private final MovementRepository movementRepository;
    private final BankAccountService bankAccountService;
    private final Clock clock;

    public MovementServiceImp(
            CreditService creditService,
            CreditCardService creditCardService,
            MovementRepository movementRepository,
            BankAccountService bankAccountService,
            Clock clock) {
        this.creditService = creditService;
        this.creditCardService = creditCardService;
        this.movementRepository = movementRepository;
        this.bankAccountService = bankAccountService;
        this.clock = clock;
    }

    @Override
    public Mono<Movement> create(Movement movement) {
        return processBankAccountMovement(movement);
    }

    private Mono<Movement> processBankAccountMovement(Movement movement) {
        return bankAccountService.findById(movement.getIdBankAccount())
                .flatMap(bankAccount -> {
                    if (isFixedTermAccount(bankAccount)) {
                        return processFixedTermAccountMovement(movement, bankAccount);
                    }
                    return processBankAccountMovement(movement, bankAccount);
                })
                .onErrorResume(ResourceNotFoundException.class, e -> Mono.error(
                        new ResourceNotFoundException(
                                String.format("The account with id: %s doesn't exist", movement.getIdBankAccount())))
                );
    }

    private boolean isSavingAccount(BankAccountDto bankAccount) {
        return bankAccount.getTypeBankAccount() == BankAccountDto.TypeBankAccount.SAVING_ACCOUNT;
    }

    private boolean isFixedTermAccount(BankAccountDto bankAccount) {
        return bankAccount.getTypeBankAccount() == BankAccountDto.TypeBankAccount.FIXED_TERM_ACCOUNT;
    }

    private Mono<Movement> processBankAccountMovement(Movement movement, BankAccountDto bankAccount) {
        return countMovementsInPresentMonthByIdBankAccount(movement.getIdBankAccount())
                .flatMap(count -> {
                    if (count >= bankAccount.getLimitMovements() && isSavingAccount(bankAccount)) {
                        return Mono.error(new LimitMovementsExceeded(
                                "The client has reached the limit of " + count + " movements."));
                    }
                    return movementHasCommission(bankAccount)
                            .flatMap(hasCommission ->
                                    hasCommission ?
                                            applyMovementWithCommissionAndSave(movement, bankAccount) :
                                            applyMovementOrTransferAndSave(movement, bankAccount));
                });
    }

    private Mono<Long> countMovementsInPresentMonthByIdBankAccount(String idBankAccount) {
        return getMovementsByBankAccountIdInPresentMonth(idBankAccount)
                .count();
    }

    private Mono<Movement> processFixedTermAccountMovement(Movement movement, BankAccountDto bankAccount) {
        if (!isDayForPayment(bankAccount))
            return Mono.error(new UnsupportedMovementException("Your bank account not support movements today"));
        return hasOneMovementOnDayPayment(movement)
                .flatMap(hasMovement -> {
                   if (hasMovement)
                       return Mono.error(new UnsupportedMovementException(
                               "Your bank account does not support more than one transaction today"));
                   return applyMovementOrTransferAndSave(movement, bankAccount);
                });
    }

    private Mono<Boolean> hasOneMovementOnDayPayment(Movement movement) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = LocalDateTime.of(
                now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0, 0);

        return getMovementsByBankAccountIdAndDateRangeAndSort(movement.getIdBankAccount(), today, now)
                .count()
                .map(numberMovements -> (numberMovements > 0));
    }

    private boolean isDayForPayment(BankAccountDto fixedTermAccount) {
        LocalDate now = LocalDate.now();
        LocalDate correctDayPay = DateToPayUtil.calculatePaymentDate.apply(fixedTermAccount.getExpirationDate());
        return now.isEqual(correctDayPay);
    }

    private Mono<Boolean> movementHasCommission(BankAccountDto bankAccount) {
        LocalDateTime from = LocalDateTime.of(LocalDate.now(clock), LocalTime.of(0, 0));
        LocalDateTime to = LocalDateTime.now(clock);
        return movementRepository.findAllByIdBankAccountAndDateBetween(bankAccount.getId(), from, to)
                .filter(movement -> movement.getTypeMovement() != TRANSFER)
                .count()
                .flatMap(numberMovements -> Mono.just(numberMovements >= bankAccount.getMaxTransactions()));
    }

    private Mono<Movement> applyMovementOrTransferAndSave(Movement movement, BankAccountDto bankAccount) {
        if (isTransferMovement(movement))
            return applyMovementTransfer(movement, bankAccount);

        return applyMovementAndSave(movement, bankAccount);
    }
    private boolean isTransferMovement(Movement movement) {
        return movement.getTypeMovement() == TRANSFER;
    }

    private Mono<Movement> applyMovementTransfer(Movement movement, BankAccountDto bankAccountOrigin) {
        String idBankAccountDestin = movement.getIdBankAccountTransfer();
        return bankAccountService.findById(idBankAccountDestin)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Client with id: " +
                                idBankAccountDestin + " doesn't exist!")))
                .flatMap(bankAccountDestin ->
                        validateAndSaveTransaction(movement, bankAccountOrigin, bankAccountDestin));


    }

    private Mono<Movement> validateAndSaveTransaction(Movement movement,
                                                      BankAccountDto bankAccountOrigin,
                                                      BankAccountDto bankAccountDestin) {
        String transferId = UUID.randomUUID().toString();

        movement.setIdTransfer(transferId);

        Movement deposit = createMovement(
                DEPOSIT,movement,transferId,bankAccountDestin.getId(),bankAccountOrigin.getId(),0.0);

        Movement withdrawal = createMovement(
                WITHDRAWAL,movement,transferId,bankAccountOrigin.getId(),bankAccountDestin.getId(),
                movement.getCommissionAmount());

        return movementRepository.save(movement)
                .flatMap(savedMovement ->
                        applyMovementToAccount(withdrawal, bankAccountOrigin)
                                .zipWhen(originUpdated -> applyMovementToAccount(deposit, bankAccountDestin))
                                .flatMap(tuple -> {
                                    BankAccountDto updatedOrigin = tuple.getT1();
                                    BankAccountDto updatedDestin = tuple.getT2();

                                    return Mono.when(
                                            updateBankAccountAndSaveMovement(withdrawal, updatedOrigin),
                                            updateBankAccountAndSaveMovement(deposit, updatedDestin)
                                    ).thenReturn(savedMovement);
                                })
                );
    }

    private Movement createMovement(Movement.TypeMovement type,
                                    Movement baseMovement,
                                    String transferId,
                                    String accountId,
                                    String transferAccountId,
                                    double commission) {
        Movement movement = new Movement();
        movement.setTypeMovement(type);
        movement.setAmount(baseMovement.getAmount());
        movement.setDescription(baseMovement.getDescription());
        movement.setCommissionAmount(commission);
        movement.setIdBankAccount(accountId);
        movement.setIdBankAccountTransfer(transferAccountId);
        movement.setIdTransfer(transferId);
        return movement;
    }

    private Mono<Movement> applyMovementAndSave(Movement movement, BankAccountDto bankAccount) {
        return applyMovementToAccount(movement, bankAccount)
                .flatMap(accountModify -> updateBankAccountAndSaveMovement(movement, accountModify));
    }

    private Mono<Movement> updateBankAccountAndSaveMovement(Movement movement, BankAccountDto bankAccountDto) {
        return bankAccountService.update(bankAccountDto.getId(), bankAccountDto)
                .then(movementRepository.save(movement));
    }
    private Mono<Movement> applyMovementWithCommissionAndSave(Movement movement, BankAccountDto bankAccount) {
        if (applyCommissionToMovement(movement, bankAccount)) {
            return applyMovementOrTransferAndSave(movement, bankAccount);
        }
        String errorMessage = "Insufficient balance after applying commission." + movement;
        log.error(errorMessage);
        return Mono.error(new InsufficientBalance(errorMessage));

    }

    private boolean applyCommissionToMovement(Movement movement, BankAccountDto bankAccount) {
        double commissionPercentage = bankAccount.getCommissionPercentage();
        double amountMovement = movement.getAmount();
        double commission = amountMovement * commissionPercentage;
        double balanceActual = bankAccount.getBalance();

        if (isWithdrawalOrPaymentMovement(movement) && (balanceActual - (amountMovement + commission)) < 0 ) {
            return false;
        }
        movement.setCommissionAmount(commission);
        bankAccount.setBalance(balanceActual - commission);
        return true;
    }

    private boolean isWithdrawalOrPaymentMovement(Movement movement) {
        return (movement.getTypeMovement() == WITHDRAWAL) || (movement.getTypeMovement() == PAY_CREDIT);
    }

    @Override
    public Mono<Movement> update(String id, Movement movement) {
        return movementRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("The movement with id: " + id + " doesn't exist!")))
                .flatMap(movementFound -> {
                    if (isMovementWithTransfer(movementFound))
                        return Mono.error(
                                new UnsupportedMovementException("Transfer update is not supported yet."));
                    if (isMovementPayCredit(movementFound))
                        return Mono.error(
                                new UnsupportedMovementException("Payments update is not supported yet."));
                    return updateMovementWithBankAccount(movementFound, movement);
                });
    }

    private boolean isMovementWithTransfer(Movement movement) {
        return movement.getIdTransfer() != null && !movement.getIdTransfer().isEmpty();
    }

    private boolean isMovementPayCredit(Movement movement) {
        return movement.getTypeMovement() == PAY_CREDIT;
    }

    @Override
    public Flux<Movement> getAll() {
        return movementRepository.findAll();
    }

    @Override
    public Mono<Movement> findById(String id) {
        return movementRepository.findById(id);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return movementRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Movement not found.")))
                .then(movementRepository.deleteById(id));
    }

    private Mono<Movement> updateMovementWithBankAccount(Movement movementOld, Movement movementNew) {
        return bankAccountService.findById(movementNew.getIdBankAccount())
                .flatMap(bankAccount ->
                    updateMovementAccordType(movementOld, movementNew)
                            .flatMap(movement -> applyMovementToAccount(movement, bankAccount))
                )
                .flatMap(bankAccount -> updateBankAccountWithMovement(bankAccount, movementOld, movementNew));

    }
    private Mono<Movement> updateMovementAccordType(Movement movementOld,
                                                    Movement movementNew) {
        Movement movement = new Movement();
        movement.setTypeMovement(movementNew.getTypeMovement());
        movement.setDate(movementNew.getDate());
        movement.setAmount(movementNew.getAmount());
        movement.setDescription(movementNew.getDescription());
        if (movementOld.getTypeMovement() != movementNew.getTypeMovement()) {
            return Mono.error(new UnsupportedMovementException("Change type movements is not supported."));
        }
        if (!Objects.equals(movementOld.getIdBankAccount(), movementNew.getIdBankAccount())) {
            return Mono.error(new UnsupportedMovementException("Change idBankAccount is not supported."));
        }

        movement.setTypeMovement(DEPOSIT);
        if (movementNew.getTypeMovement() == WITHDRAWAL) {
            double result = movementOld.getAmount() - movement.getAmount();
            if (result < 0)
                movement.setTypeMovement(WITHDRAWAL);
            movement.setAmount(Math.abs(result));
        }
        if (movementNew.getTypeMovement() == DEPOSIT) {
            double result = movement.getAmount() - movementOld.getAmount();
            movement.setAmount(result);
        }
        return Mono.just(movement);
    }

    private Mono<Movement> updateBankAccountWithMovement(BankAccountDto bankAccount,
                                                         Movement movementOld,
                                                         Movement movementNew) {
        return bankAccountService.update(bankAccount.getId(), bankAccount)
                .then(saveUpdateMovement(movementOld, movementNew));
    }

    private Mono<Movement> saveUpdateMovement(Movement movementOld, Movement movementNew) {
        movementOld.setDate(movementNew.getDate());
        movementOld.setAmount(movementNew.getAmount());
        movementOld.setDescription(movementNew.getDescription());
        return movementRepository.save(movementOld);
    }

    @Override
    public Flux<Movement> getMovementsByBankAccountIdInPresentMonth(String bankAccountId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime from = LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0);
        return getMovementsByBankAccountIdAndDateRangeAndSort(bankAccountId, from, now);
    }

    @Override
    public Flux<Movement> getMovementsByBankAccountIdAndDateRangeAndSort(String id,
                                                                         LocalDateTime from,
                                                                         LocalDateTime to) {
        return movementRepository.findAllByIdBankAccountAndDateBetween(id, from, to)
                .filter(movement -> movement.getTypeMovement() != TRANSFER)
                .sort(Comparator.comparing(Movement::getDate).reversed());
    }

    @Override
    public Flux<Movement> getAllMovementsByIdBankAccountAndSortByDate(String idBankAccount) {
        return movementRepository.findAllByIdBankAccount(idBankAccount)
                .filter(movement -> movement.getTypeMovement() != TRANSFER)
                .sort(Comparator.comparing(Movement::getDate).reversed());
    }

    @Override
    public Flux<Movement> findAllByDateBetween(LocalDateTime from, LocalDateTime to) {
        return movementRepository.findAllByDateBetween(from, to)
                .filter(movement -> movement.getTypeMovement() != TRANSFER);
    }

    private Mono<BankAccountDto> applyMovementToAccount(Movement movement, BankAccountDto bankAccount) {
        if (isWithdrawalOrPaymentMovement(movement)) {
            if (bankAccount.getBalance() < movement.getAmount()) {
                return Mono.error(new InsufficientBalance("There is not enough balance in your account."));
            }
            bankAccount.setBalance(bankAccount.getBalance() - movement.getAmount());
            return Mono.just(bankAccount);
        }
        else if (movement.getTypeMovement() == DEPOSIT) {
            bankAccount.setBalance(bankAccount.getBalance() + movement.getAmount());
            return Mono.just(bankAccount);
        }
        return Mono.error(new UnsupportedMovementException("The movement is not supported."));
    }

    @Override
    public Mono<Map<String, List<?>>> getBankProductsByIdClient(String idClient) {
        Flux<BankAccountDto> allBankAccounts =
                bankAccountService.findBankAccountsByIdClientWithAllMovementsSortedByDate(idClient);
        Flux<CreditDto> allCredits =
                creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient);
        Flux<CreditCardDto> allCreditCards =
                creditCardService.allCreditCardsByIdClientWithPaymentAndConsumption(idClient);

        return Flux.zip(allBankAccounts.collectList(), allCredits.collectList(), allCreditCards.collectList())
                .flatMap(allData -> {
                    List<BankAccountDto> bankAccounts = allData.getT1();
                    List<CreditDto> credits = allData.getT2();
                    List<CreditCardDto> creditCards = allData.getT3();
                    Map<String, List<?>> bankProducts = new HashMap<>();
                    bankProducts.put("bankAccounts", bankAccounts);
                    bankProducts.put("credits", credits);
                    bankProducts.put("creditCards", creditCards);
                    return Mono.just(bankProducts);
                })
                .collectList()
                .flatMap(products -> Mono.just(products.get(0)));
    }
}
