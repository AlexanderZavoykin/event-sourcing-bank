package ru.quipy.bank.accounts.api

import ru.quipy.core.annotations.DomainEvent
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.UUID

const val ACCOUNT_CREATED = "ACCOUNT_CREATED_EVENT"
const val BANK_ACCOUNT_CREATED = "BANK_ACCOUNT_CREATED_EVENT"
const val BANK_ACCOUNT_DEPOSIT = "BANK_ACCOUNT_DEPOSIT_EVENT"
const val BANK_ACCOUNT_WITHDRAWAL = "BANK_ACCOUNT_WITHDRAWAL_EVENT"
const val INTERNAL_ACCOUNT_TRANSFER = "INTERNAL_ACCOUNT_TRANSFER_EVENT"

const val TRANSFER_DEPOSIT_PERFORMED = "TRANSFER_DEPOSIT_PERFORMED"
const val TRANSFER_WITHDRAW_PERFORMED = "TRANSFER_WITHDRAW_PERFORMED"
const val TRANSFER_DEPOSIT_REJECTED = "TRANSFER_DEPOSIT_REJECTED"
const val TRANSFER_WITHDRAW_REJECTED = "TRANSFER_WITHDRAW_REJECTED"
const val TRANSFER_DEPOSIT_ROLLBACKED = "TRANSFER_DEPOSIT_ROLLBACKED"
const val TRANSFER_WITHDRAW_ROLLBACKED = "TRANSFER_WITHDRAW_ROLLBACKED"

const val NOOP = "NOOP"


@DomainEvent(name = ACCOUNT_CREATED)
data class AccountCreatedEvent(
    val accountId: UUID,
    val userId: UUID,
) : Event<AccountAggregate>(
    name = ACCOUNT_CREATED,
)

@DomainEvent(name = BANK_ACCOUNT_CREATED)
data class BankAccountCreatedEvent(
    val accountId: UUID,
    val bankAccountId: UUID,
) : Event<AccountAggregate>(
    name = BANK_ACCOUNT_CREATED,
)

@DomainEvent(name = BANK_ACCOUNT_DEPOSIT)
data class BankAccountDepositEvent(
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = BANK_ACCOUNT_DEPOSIT,
)

@DomainEvent(name = BANK_ACCOUNT_WITHDRAWAL)
data class BankAccountWithdrawalEvent(
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = BANK_ACCOUNT_WITHDRAWAL,
)

@DomainEvent(name = INTERNAL_ACCOUNT_TRANSFER)
data class InternalAccountTransferEvent(
    val accountId: UUID,
    val bankAccountIdFrom: UUID,
    val bankAccountIdTo: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = INTERNAL_ACCOUNT_TRANSFER,
)

@DomainEvent(name = TRANSFER_DEPOSIT_PERFORMED)
data class TransferDepositPerformedEvent(
    val transferId: UUID,
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = TRANSFER_DEPOSIT_PERFORMED,
)

@DomainEvent(name = TRANSFER_WITHDRAW_PERFORMED)
data class TransferWithdrawPerformedEvent(
    val transferId: UUID,
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = TRANSFER_WITHDRAW_PERFORMED,
)

@DomainEvent(name = TRANSFER_DEPOSIT_REJECTED)
data class TransferDepositRejectedEvent(
    val transferId: UUID,
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
    val reason: String,
) : Event<AccountAggregate>(
    name = TRANSFER_DEPOSIT_REJECTED,
)

@DomainEvent(name = TRANSFER_WITHDRAW_REJECTED)
data class TransferWithdrawRejectedEvent(
    val transferId: UUID,
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
    val reason: String,
) : Event<AccountAggregate>(
    name = TRANSFER_WITHDRAW_REJECTED,
)

@DomainEvent(name = TRANSFER_DEPOSIT_ROLLBACKED)
data class TransferDepositRollbackedEvent(
    val transferId: UUID,
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = TRANSFER_DEPOSIT_ROLLBACKED,
)

@DomainEvent(name = TRANSFER_WITHDRAW_ROLLBACKED)
data class TransferWithdrawRollbackedEvent(
    val transferId: UUID,
    val accountId: UUID,
    val bankAccountId: UUID,
    val amount: BigDecimal,
) : Event<AccountAggregate>(
    name = TRANSFER_WITHDRAW_ROLLBACKED,
)

@DomainEvent(name = NOOP)
data class NoopEvent(
    val transferId: UUID,
) : Event<AccountAggregate>(
    name = NOOP,
)