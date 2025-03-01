package com.movements.movementsmicroservice.impl;

import com.movements.movementsmicroservice.DTO.BankAccountDto;
import com.movements.movementsmicroservice.DTO.ClientDto;
import com.movements.movementsmicroservice.DTO.DebitCardDto;
import com.movements.movementsmicroservice.client.BankAccountService;
import com.movements.movementsmicroservice.client.CreditCardService;
import com.movements.movementsmicroservice.client.CreditService;
import com.movements.movementsmicroservice.client.DebitCardService;
import com.movements.movementsmicroservice.exceptions.InsufficientBalance;
import com.movements.movementsmicroservice.exceptions.LimitMovementsExceeded;
import com.movements.movementsmicroservice.exceptions.ResourceNotFoundException;
import com.movements.movementsmicroservice.exceptions.UnsupportedMovementException;
import com.movements.movementsmicroservice.model.Movement;
import com.movements.movementsmicroservice.repository.MovementRepository;
import com.movements.movementsmicroservice.service.PaymentMovementService;
import com.movements.movementsmicroservice.service.impl.MovementServiceImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.movements.movementsmicroservice.model.Movement.TypeMovement.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MovementServiceImpTest {

    @InjectMocks
    private MovementServiceImp movementService;
    @Mock
    private CreditService creditService;
    @Mock
    private CreditCardService creditCardService;
    @Mock
    private BankAccountService bankAccountService;
    @Mock
    private MovementRepository movementRepository;
    @Mock
    private DebitCardService debitCardService;
    @Mock
    private PaymentMovementService paymentMovementService;
    @Mock
    private Clock clock;
    private Movement movement1, movement2;
    private BankAccountDto bankAccount1, bankAccount2, bankAccount3;
    private ClientDto personalClient, businessClient;

    @BeforeEach
    void setUp() {
        movement1 = new Movement();
        movement1.setId("IDMOVEMENT01");
        movement1.setIdBankAccount("IDbank001");
        movement1.setAmount(20.0);
        movement1.setTypeMovement(DEPOSIT);
        movement1.setDescription("Deposit Movement");
        movement1.setIdBankAccountTransfer("");

        movement2 = new Movement();
        movement2.setId("IDMOVEMENT02");
        movement2.setIdBankAccount("IDbank002");
        movement2.setAmount(10.0);
        movement2.setTypeMovement(WITHDRAWAL);
        movement2.setDescription("Withdrawal Movement");
        movement2.setIdBankAccountTransfer("");

        bankAccount1 = new BankAccountDto("clientN001",
                1500.0,
                BankAccountDto.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount1.setId("IDbank001");

        bankAccount2 = new BankAccountDto("clientN002",
                1200.0,
                BankAccountDto.TypeBankAccount.CURRENT_ACCOUNT,
                0,
                0,
                15.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount2.setId("IDbank002");

        bankAccount3 = new BankAccountDto("clientN001",
                15.0,
                BankAccountDto.TypeBankAccount.FIXED_TERM_ACCOUNT,
                0,
                31,
                0.0,
                0.1,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount3.setId("IDbank003");

        personalClient = new ClientDto();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(ClientDto.TypeClient.PERSONAL_CLIENT);
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setFullName("Lucas Juan");
        personalClient.setIdentity("75690210");

        businessClient = new ClientDto();
        businessClient.setId("clientN002");
        businessClient.setAddress("Jr. avenida Jose Pérez");
        businessClient.setTypeClient(ClientDto.TypeClient.BUSINESS_CLIENT);
        businessClient.setEmail("business@ejemplo.com");
        businessClient.setBusinessName("Panadería Boluelos");
        businessClient.setTaxId("20756902101");
    }

    @Test
    @DisplayName("Create a Movement in saving account")
    void createSavingMovementTest() {
        String idBankAccount = "IDbank001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        // Given
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(movementRepository
                .findAllByIdBankAccountAndDateBetween(
                        eq(idBankAccount), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        when(bankAccountService.update(idBankAccount, bankAccount1)).thenReturn(Mono.just(bankAccount1));
        when(movementRepository.save(any(Movement.class))).thenReturn(Mono.just(movement1));
        // When
        Mono<Movement> movementMono = movementService.create(movement1);
        // Then
        StepVerifier.create(movementMono)
                .expectNextMatches(element -> element.getId().equals(movement1.getId()))
                .verifyComplete();
        verify(bankAccountService).findById(idBankAccount);
        verify(bankAccountService).update(idBankAccount, bankAccount1);
        verify(movementRepository).save(movement1);
    }

    @Test
    @DisplayName("Create a Movement in fixed term account when the date is not correct")
    void createFixedTermMovementInIncorrectDayTest() {

        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        String idBankAccount = "IDbank003";
        movement1.setIdBankAccount(idBankAccount);
        int numberDayIncorrect = LocalDate.now(clock).getDayOfMonth() - 1;
        bankAccount3.setExpirationDate(numberDayIncorrect);
        // Given
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount3));

        // When
        Mono<Movement> movementMono = movementService.create(movement1);
        // Then
        StepVerifier.create(movementMono)
                .expectError(UnsupportedMovementException.class)
                .verify();
        verify(bankAccountService).findById(idBankAccount);
    }

    @Test
    @DisplayName("Create a Movement in fixed term account when the date is correct")
    void createFixedTermMovementIncorrectDayTest() {
        String idBankAccount = "IDbank003";
        movement1.setIdBankAccount(idBankAccount);
        int numberDayCorrect = LocalDate.now().getDayOfMonth();
        bankAccount3.setExpirationDate(numberDayCorrect);
        // Given
        when(movementRepository
                .findAllByIdBankAccountAndDateBetween(
                        eq(idBankAccount), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount3));
        when(bankAccountService.update(idBankAccount, bankAccount3)).thenReturn(Mono.just(bankAccount3));
        when(movementRepository.save(any(Movement.class))).thenReturn(Mono.just(movement1));
        // When

        Mono<Movement> movementMono = movementService.create(movement1);
        // Then
        StepVerifier.create(movementMono)
                .expectNextMatches(element -> element.getId().equals("IDMOVEMENT01"))
                .verifyComplete();
        verify(bankAccountService).findById(idBankAccount);
        verify(bankAccountService).update(idBankAccount, bankAccount3);
        verify(movementRepository).save(any(Movement.class));
    }

    @Test
    @DisplayName("Create a Movement in current account")
    void createCurrentMovementTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        String idBankAccount = "IDbank002";
        // Given
        when(bankAccountService.findById(movement2.getIdBankAccount())).thenReturn(Mono.just(bankAccount2));
        when(bankAccountService.update(idBankAccount, bankAccount2)).thenReturn(Mono.just(bankAccount2));
        when(movementRepository.save(any(Movement.class))).thenReturn(Mono.just(movement2));
        when(movementRepository.findAllByIdBankAccountAndDateBetween(anyString(), any(), any()))
                .thenReturn(Flux.just(movement1, movement2));
        // When
        Mono<Movement> movementMono = movementService.create(movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectNextMatches(element -> element.getId().equals(movement2.getId()))
                .verifyComplete();
        verify(bankAccountService).findById(idBankAccount);
        verify(bankAccountService).update(idBankAccount, bankAccount2);
        verify(movementRepository).save(movement2);
    }

    @Test
    @DisplayName("Create a Movement in not found account")
    void createMovementInNoAccountTest() {

        String idBankAccount = "IDbankNotfound01";
        movement2.setIdBankAccount(idBankAccount);
        // Given
        when(bankAccountService.findById(movement2.getIdBankAccount()))
                .thenReturn(Mono.error(new ResourceNotFoundException("")));
        // When
        Mono<Movement> movementMono = movementService.create(movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(bankAccountService).findById(idBankAccount);
    }

    @Test
    @DisplayName("Create a Movement in saving account when that exceeded limit movements")
    void createSavingMovementExceededTest() {
        String idBankAccount = "IDbank001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        // Given
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(movementRepository
                .findAllByIdBankAccountAndDateBetween(
                        eq(idBankAccount), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.just(movement2, movement2));
        // When
        Mono<Movement> movementMono = movementService.create(movement1);
        // Then
        StepVerifier.create(movementMono)
                .expectError(LimitMovementsExceeded.class)
                .verify();
        verify(bankAccountService).findById(idBankAccount);
    }

    @Test
    @DisplayName("Create a Movement in fixed term account when that has already movements")
    void createFixedTermMovementExceededTest() {
        String idBankAccount = "IDbank003";
        movement1.setIdBankAccount(idBankAccount);
        int numberDayCorrect = LocalDate.now().getDayOfMonth();
        bankAccount3.setExpirationDate(numberDayCorrect);
        // Given
        when(bankAccountService.findById(idBankAccount)).thenReturn(Mono.just(bankAccount3));
        when(movementRepository
                .findAllByIdBankAccountAndDateBetween(
                        eq(idBankAccount), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.just(movement2));
        // When
        Mono<Movement> movementMono = movementService.create(movement1);
        // Then
        StepVerifier.create(movementMono)
                .expectError(UnsupportedMovementException.class)
                .verify();
        verify(bankAccountService).findById(idBankAccount);
    }

    @Test
    @DisplayName("Create a Movement in saving account without enough balance")
    void createSavingMovementWithoutBalanceTest() {
        String idBankAccount = "IDbank001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        movement2.setIdBankAccount(idBankAccount);
        movement2.setAmount(5000.0);
        // Given
        when(bankAccountService.findById(movement2.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(movementRepository
                .findAllByIdBankAccountAndDateBetween(
                        eq(idBankAccount), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        // When
        Mono<Movement> movementMono = movementService.create(movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectError(InsufficientBalance.class)
                .verify();
        verify(bankAccountService).findById(idBankAccount);
    }

    @Test
    @DisplayName("Create a Movement unsupported")
    void createUnsupportedMovementTest() {
        String idBankAccount = "IDbank001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        movement2.setIdBankAccount(idBankAccount);
        movement2.setTypeMovement(Movement.TypeMovement.UNSUPPORTED);
        // Given
        when(bankAccountService.findById(movement2.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(movementRepository
                .findAllByIdBankAccountAndDateBetween(
                        eq(idBankAccount), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        // When
        Mono<Movement> movementMono = movementService.create(movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectError(UnsupportedMovementException.class)
                .verify();
        verify(bankAccountService).findById(idBankAccount);
    }

    @Test
    @DisplayName("Update a movement of type withdrawal")
    void updateMovementWithdrawalTest() {
        // Given
        String idMovement = "IDMOVEMENT01";
        movement2.setId("IDMOVEMENT01");
        movement2.setIdBankAccount("IDbank001");
        movement2.setTypeMovement(WITHDRAWAL);
        movement1.setTypeMovement(WITHDRAWAL);
        when(movementRepository.findById(idMovement)).thenReturn(Mono.just(movement1));
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(bankAccountService.update(bankAccount1.getId(), bankAccount1)).thenReturn(Mono.just(bankAccount1));
        when(movementRepository.save(movement1)).thenReturn(Mono.just(movement1));
        // When
        Mono<Movement> movementMono = movementService.update(idMovement, movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectNextMatches(item -> item.getId().equals(movement1.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("updates a withdrawal type transaction with a smaller amount")
    void updateMovementWithSmallestAmount() {
        // Given
        String idMovement = "IDMOVEMENT01";
        movement2.setId("IDMOVEMENT01");
        movement2.setIdBankAccount("IDbank001");
        movement2.setTypeMovement(WITHDRAWAL);
        movement1.setTypeMovement(WITHDRAWAL);
        movement2.setAmount(movement1.getAmount() + 10);
        when(movementRepository.findById(idMovement)).thenReturn(Mono.just(movement1));
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(bankAccountService.update(bankAccount1.getId(), bankAccount1)).thenReturn(Mono.just(bankAccount1));
        when(movementRepository.save(movement1)).thenReturn(Mono.just(movement1));
        // When
        Mono<Movement> movementMono = movementService.update(idMovement, movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectNextMatches(item -> item.getId().equals(movement1.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Update a movement success")
    void updateMovementSuccess() {
        // Given
        String idMovement = "IDMOVEMENT01";
        movement2.setId("IDMOVEMENT01");
        movement2.setIdBankAccount("IDbank001");
        movement2.setTypeMovement(Movement.TypeMovement.DEPOSIT);

        when(movementRepository.findById(idMovement)).thenReturn(Mono.just(movement1));
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(bankAccountService.update(bankAccount1.getId(), bankAccount1)).thenReturn(Mono.just(bankAccount1));
        when(movementRepository.save(movement1)).thenReturn(Mono.just(movement1));
        // When
        Mono<Movement> movementMono = movementService.update(idMovement, movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectNextMatches(item -> item.getId().equals(idMovement) && item.getAmount() == 10)
                .verifyComplete();

    }

    @Test
    @DisplayName("Get all movements By Bank account id")
    void getAllMovementsByIdBankAccountAndSortByDate() {
        // Given
        String idBankAccount = "aasd23f1a5";
        when(movementRepository.findAllByIdBankAccount(idBankAccount)).thenReturn(Flux.just(movement1, movement2));
        // WHen
        Flux<Movement> movementFlux = movementService.getAllMovementsByIdBankAccountAndSortByDate(idBankAccount);
        // Then
        StepVerifier.create(movementFlux)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Get report of all movements in all accounts and credits of a customer")
    void getBankProductsByIdClient() {
        String idClient = "clientN001";
        // Given
        when(bankAccountService.findBankAccountsByIdClientWithAllMovementsSortedByDate(idClient))
                .thenReturn(Flux.just(bankAccount1));
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient))
                .thenReturn(Flux.empty());
        when(creditCardService.allCreditCardsByIdClientWithPaymentAndConsumption(idClient))
                .thenReturn(Flux.empty());
        // When
        Mono<Map<String, List<?>>> responseMono = movementService.getBankProductsByIdClient(idClient);
        // Then
        StepVerifier.create(responseMono)
                .expectNextMatches(element ->
                    element.get("credits").isEmpty() && element.get("creditCards").isEmpty()
                            && (element.get("bankAccounts").size() == 1)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Create a Movement transfer of a saving account")
    void createMovementTransferTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        Movement transfer = new Movement();
        transfer.setId("AAAAA11111");
        transfer.setIdBankAccount(bankAccount1.getId());
        transfer.setIdBankAccountTransfer(bankAccount2.getId());
        transfer.setAmount(100.0);
        transfer.setTypeMovement(TRANSFER);
        transfer.setDescription("Movement Transfer");
        // Given
        when(bankAccountService.findById(transfer.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(bankAccountService.findById(transfer.getIdBankAccountTransfer())).thenReturn(Mono.just(bankAccount2));
        // Search movements
        when(movementRepository.findAllByIdBankAccountAndDateBetween(eq(bankAccount1.getId()), any(), any()))
                .thenReturn(Flux.empty());
        when(movementRepository.save(any(Movement.class))).thenReturn(Mono.just(transfer));
        when(bankAccountService.update(bankAccount1.getId(), bankAccount1)).thenReturn(Mono.just(bankAccount1));
        when(bankAccountService.update(bankAccount2.getId(), bankAccount2)).thenReturn(Mono.just(bankAccount2));

        // When
        Mono<Movement> movementMono = movementService.create(transfer);
        // Then
        StepVerifier.create(movementMono)
                .assertNext(movement -> {
                    assertThat(movement).isNotNull();
                    assertThat(transfer.getId()).isEqualTo(movement.getId());
                    assertThat(movement.getTypeMovement()).isEqualTo(TRANSFER);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Create a Movement transfer with commission")
    void createMovementTransferWithCommissionTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        Movement transfer = new Movement();
        transfer.setId("AAAAA11111");
        transfer.setIdBankAccount(bankAccount1.getId());
        transfer.setIdBankAccountTransfer(bankAccount2.getId());
        transfer.setAmount(100.0);
        transfer.setTypeMovement(TRANSFER);
        transfer.setDescription("Movement Transfer");
        bankAccount1.setMaxTransactions(2);
        bankAccount1.setLimitMovements(5);
        // Given
        when(bankAccountService.findById(transfer.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        when(bankAccountService.findById(transfer.getIdBankAccountTransfer())).thenReturn(Mono.just(bankAccount2));
        // Search movements
        when(movementRepository.findAllByIdBankAccountAndDateBetween(eq(bankAccount1.getId()), any(), any()))
                .thenReturn(Flux.just(movement1, movement1));
        when(movementRepository.save(any(Movement.class))).thenReturn(Mono.just(transfer));
        when(bankAccountService.update(bankAccount1.getId(), bankAccount1)).thenReturn(Mono.just(bankAccount1));
        when(bankAccountService.update(bankAccount2.getId(), bankAccount2)).thenReturn(Mono.just(bankAccount2));

        // When
        Mono<Movement> movementMono = movementService.create(transfer);
        // Then
        StepVerifier.create(movementMono)
                .assertNext(movement -> {
                    assertThat(movement).isNotNull();
                    assertThat(transfer.getId()).isEqualTo(movement.getId());
                    assertThat(transfer.getCommissionAmount()).isEqualTo(20);
                    assertThat(movement.getTypeMovement()).isEqualTo(TRANSFER);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Update a movement with different type")
    void updateMovementWithDifferentType() {
        // Given
        String idMovement = "IDMOVEMENT01";
        movement2.setId("IDMOVEMENT01");
        movement2.setIdBankAccount("IDbank001");
        movement2.setTypeMovement(TRANSFER);

        when(movementRepository.findById(idMovement)).thenReturn(Mono.just(movement1));
        when(bankAccountService.findById(movement1.getIdBankAccount())).thenReturn(Mono.just(bankAccount1));
        // When
        Mono<Movement> movementMono = movementService.update(idMovement, movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectError(UnsupportedMovementException.class)
                .verify();

    }
    @Test
    @DisplayName("Update a movement with different bank id")
    void updateMovementWithDifferentBankId() {
        // Given
        String idMovement = "IDMOVEMENT01";
        movement2.setId("IDMOVEMENT01");
        movement2.setIdBankAccount("IDbank001");
        movement2.setTypeMovement(DEPOSIT);
        movement1.setIdBankAccount("IDbank002");

        when(movementRepository.findById(idMovement)).thenReturn(Mono.just(movement1));
        when(bankAccountService.findById(anyString())).thenReturn(Mono.just(bankAccount1));
        // When
        Mono<Movement> movementMono = movementService.update(idMovement, movement2);
        // Then
        StepVerifier.create(movementMono)
                .expectError(UnsupportedMovementException.class)
                .verify();

    }
    @Test
    @DisplayName("Find movement by id")
    void findByIdTest() {
        // Given
        String idMovement = "IDMOVEMENT01";

        when(movementRepository.findById(idMovement)).thenReturn(Mono.just(movement1));
        // When
        Mono<Movement> movementMono = movementService.findById(idMovement);
        // Then
        StepVerifier.create(movementMono)
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(idMovement);
                    assertThat(result.getAmount()).isEqualTo(movement1.getAmount());
                })
                .verifyComplete();

    }
    @Test
    @DisplayName("Get all movements")
    void getAllMovementsTest() {
        // Given
        when(movementRepository.findAll()).thenReturn(Flux.just(movement1));
        // When
        Flux<Movement> movementFlux = movementService.getAll();
        // Then
        StepVerifier.create(movementFlux)
                .expectNextCount(1)
                .verifyComplete();

    }
    @Test
    @DisplayName("Delete by id movement")
    void deleteByIdMovementTest() {
        // Given
        String idMovement = "IDMOVEMENT01";
        when(movementRepository.findById(idMovement)).thenReturn(Mono.just(movement1));
        when(movementRepository.deleteById(idMovement)).thenReturn(Mono.empty());
        // When
        Mono<Void> movementDeleted = movementService.deleteById(idMovement);
        // Then
        StepVerifier.create(movementDeleted)
                .verifyComplete();

    }
    @Test
    @DisplayName("Delete by id movement When not exists")
    void deleteNotExistMovementTest() {
        // Given
        String idMovement = "IDMOVEMENT01";
        when(movementRepository.findById(anyString())).thenReturn(Mono.empty());
        when(movementRepository.deleteById(idMovement)).thenReturn(Mono.empty());
        // When
        Mono<Void> movementDeleted = movementService.deleteById(idMovement);
        // Then
        StepVerifier.create(movementDeleted)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Find all by date between")
    void findAllMovementsByDateBetweenTest() {
        // Given
        String idMovement = "IDMOVEMENT01";
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now();
        when(movementRepository.findAllByDateBetween(from, to)).thenReturn(Flux.just(movement1, movement2));
        // When
        Flux<Movement> movementDeleted = movementService.findAllByDateBetween(from, to);
        // Then
        StepVerifier.create(movementDeleted)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Create a Movement type withdrawal debit card")
    void createWithdrawalDebitCardMovementTest() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        DebitCardDto debitCard1 = new DebitCardDto();
        debitCard1.setId("DEBITCARD001");
        debitCard1.setIdClient("clientN001");
        debitCard1.setIdPrincipalAccount(bankAccount1.getId());
        String idBankAccount = "IDbank002";
        movement2.setTypeMovement(WITHDRAWAL_DEBIT);
        movement2.setIdBankAccount(debitCard1.getId());
        // Given
        when(bankAccountService.findById("IDbank001")).thenReturn(Mono.just(bankAccount2));
        when(bankAccountService.update(idBankAccount, bankAccount2)).thenReturn(Mono.just(bankAccount2));
        when(movementRepository.save(any(Movement.class))).thenReturn(Mono.just(movement2));
        when(movementRepository.findAllByIdBankAccountAndDateBetween(anyString(), any(), any()))
                .thenReturn(Flux.just(movement1, movement2));
        when(debitCardService.findByIdWithBankAccountsOrderByCreatedAt(debitCard1.getId()))
                .thenReturn(Mono.just(debitCard1));
        when(paymentMovementService.getBankAccountWithBalanceAvailableForPay(debitCard1, movement2.getAmount()))
                .thenReturn(Mono.just(bankAccount1));
        // When
        Mono<Movement> movementMono = movementService.create(movement2);
        // Then
        StepVerifier.create(movementMono)
                .assertNext(element -> {
                    assertThat(element.getId()).isEqualTo(movement2.getId());
                    assertThat(element.getTypeMovement()).isEqualTo(WITHDRAWAL_DEBIT);
                })
                .verifyComplete();
        verify(bankAccountService).update(idBankAccount, bankAccount2);
        verify(movementRepository).save(movement2);
    }

}