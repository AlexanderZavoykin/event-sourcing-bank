package ru.quipy.bank.accounts.logic

import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.AccountCreatedEvent
import ru.quipy.bank.accounts.api.BankAccountCreatedEvent
import ru.quipy.bank.accounts.api.BankAccountDepositEvent
import ru.quipy.bank.accounts.api.BankAccountWithdrawalEvent
import ru.quipy.bank.accounts.api.InternalAccountTransferEvent
import ru.quipy.bank.accounts.api.NoopEvent
import ru.quipy.bank.accounts.api.TransferDepositPerformedEvent
import ru.quipy.bank.accounts.api.TransferDepositRejectedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawPerformedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawRejectedEvent
import ru.quipy.bank.accounts.api.TransferWithdrawRollbackedEvent
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.UUID

private const val BANK_ACCOUNT_MAX_BALANCE = 10_000_000
private const val BANK_ACCOUNTS_MAX_SUM_BALANCE = 25_000_000
private const val BANK_ACCOUNTS_MAX_COUNT = 5

class Account : AggregateState<UUID, AccountAggregate> {
    private lateinit var accountId: UUID
    private lateinit var holderId: UUID
    var bankAccounts: MutableMap<UUID, BankAccount> = mutableMapOf()

    override fun getId(): UUID = accountId

    fun createNewAccount(id: UUID = UUID.randomUUID(), holderId: UUID): AccountCreatedEvent {
        return AccountCreatedEvent(id, holderId)
    }

    fun createNewBankAccount(): BankAccountCreatedEvent {
        if (bankAccounts.size >= BANK_ACCOUNTS_MAX_COUNT)
            throw IllegalStateException("Account $accountId already has ${bankAccounts.size} bank accounts")

        return BankAccountCreatedEvent(accountId = accountId, bankAccountId = UUID.randomUUID())
    }

    fun deposit(toBankAccountId: UUID, amount: BigDecimal): BankAccountDepositEvent {
        val bankAccount = (bankAccounts[toBankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $toBankAccountId"))

        if (bankAccount.balance + amount > BigDecimal(BANK_ACCOUNT_MAX_BALANCE))
            throw IllegalStateException("You can't store more than 10.000.000 on account ${bankAccount.id}")

        if (bankAccounts.values.sumOf { it.balance } + amount > BigDecimal(BANK_ACCOUNTS_MAX_SUM_BALANCE))
            throw IllegalStateException("You can't store more than 25.000.000 in total")


        return BankAccountDepositEvent(
            accountId = accountId,
            bankAccountId = toBankAccountId,
            amount = amount
        )
    }

    fun withdraw(fromBankAccountId: UUID, amount: BigDecimal): BankAccountWithdrawalEvent {
        val fromBankAccount = bankAccounts[fromBankAccountId]
            ?: throw IllegalArgumentException("No such account to withdraw from: $fromBankAccountId")

        if (amount > fromBankAccount.balance) {
            throw IllegalArgumentException("Cannot withdraw $amount. Not enough money: ${fromBankAccount.balance}")
        }

        return BankAccountWithdrawalEvent(
            accountId = accountId,
            bankAccountId = fromBankAccountId,
            amount = amount
        )
    }

    fun transferBetweenInternalAccounts(
        fromBankAccountId: UUID,
        toBankAccountId: UUID,
        transferAmount: BigDecimal
    ): InternalAccountTransferEvent {
        val bankAccountFrom = bankAccounts[fromBankAccountId]
            ?: throw IllegalArgumentException("No such account to withdraw from: $fromBankAccountId")

        if (transferAmount > bankAccountFrom.balance) {
            throw IllegalArgumentException("Cannot withdraw $transferAmount. Not enough money: ${bankAccountFrom.balance}")
        }

        val bankAccountTo = (bankAccounts[toBankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $toBankAccountId"))


        if (bankAccountTo.balance + transferAmount > BigDecimal(BANK_ACCOUNT_MAX_BALANCE))
            throw IllegalStateException("You can't store more than 10.000.000 on account ${bankAccountTo.id}")

        return InternalAccountTransferEvent(
            accountId = accountId,
            bankAccountIdFrom = fromBankAccountId,
            bankAccountIdTo = toBankAccountId,
            amount = transferAmount
        )
    }

    fun performTransferDeposit(
        transferId: UUID,
        bankAccountId: UUID,
        amount: BigDecimal,
    ): Event<AccountAggregate> {
        val bankAccount = bankAccounts[bankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $bankAccountId")

        if (bankAccount.balance + amount > BigDecimal(BANK_ACCOUNT_MAX_BALANCE)) {
            return TransferDepositRejectedEvent(
                transferId = transferId,
                accountId = accountId,
                bankAccountId = bankAccountId,
                amount = amount,
                reason = "User can't store more than 10.000.000 on account: ${bankAccount.id}"
            )
        }

        if (bankAccounts.values.sumOf { it.balance } + amount > BigDecimal(BANK_ACCOUNTS_MAX_SUM_BALANCE)) {
            return TransferDepositRejectedEvent(
                transferId = transferId,
                accountId = accountId,
                bankAccountId = bankAccountId,
                amount = amount,
                reason = "User can't store more than 25.000.000 in total on account: ${bankAccount.id}"
            )
        }

        return TransferDepositPerformedEvent(
            transferId = transferId,
            accountId = accountId,
            bankAccountId = bankAccountId,
            amount = amount,
        )
    }

    fun performTransferWithdraw(
        transferId: UUID,
        bankAccountId: UUID,
        amount: BigDecimal,
    ): Event<AccountAggregate> {
        val bankAccount = bankAccounts[bankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer from: $bankAccountId")

        if (amount > bankAccount.balance) {
            return TransferWithdrawRejectedEvent(
                transferId = transferId,
                accountId = accountId,
                bankAccountId = bankAccountId,
                amount = amount,
                reason = "Cannot withdraw $amount. Not enough money: ${bankAccount.balance}"
            )
        }

        return TransferWithdrawPerformedEvent(
            transferId = transferId,
            accountId = accountId,
            bankAccountId = bankAccountId,
            amount = amount,
        )
    }

    fun rollbackTransferWithdraw(
        transferId: UUID,
        bankAccountId: UUID,
        amount: BigDecimal,
    ): TransferWithdrawRollbackedEvent {
        val bankAccount = bankAccounts[bankAccountId]
            ?: throw IllegalArgumentException("No such account to rollback transfer deposit: $bankAccountId")

        if (!bankAccount.checkTransferWithdraw(transferId, amount)) {
            throw IllegalArgumentException("Transfer withdraw $transferId was never made from bank account $bankAccountId")
        }

        return TransferWithdrawRollbackedEvent(
            transferId = transferId,
            accountId = accountId,
            bankAccountId = bankAccountId,
            amount = amount,
        )
    }

    @StateTransitionFunc
    fun createNewBankAccount(event: AccountCreatedEvent) {
        accountId = event.accountId
        holderId = event.userId
    }

    @StateTransitionFunc
    fun createNewBankAccount(event: BankAccountCreatedEvent) {
        bankAccounts[event.bankAccountId] = BankAccount(event.bankAccountId)
    }

    @StateTransitionFunc
    fun deposit(event: BankAccountDepositEvent) {
        bankAccounts[event.bankAccountId]!!.deposit(event.amount)
    }

    @StateTransitionFunc
    fun withdraw(event: BankAccountWithdrawalEvent) {
        bankAccounts[event.bankAccountId]!!.withdraw(event.amount)
    }

    @StateTransitionFunc
    fun internalAccountTransfer(event: InternalAccountTransferEvent) {
        bankAccounts[event.bankAccountIdFrom]!!.withdraw(event.amount)
        bankAccounts[event.bankAccountIdTo]!!.deposit(event.amount)
    }

    @StateTransitionFunc
    fun transferDeposit(event: TransferDepositPerformedEvent) {
        val bankAccount = bankAccounts[event.bankAccountId]!!
        bankAccount.deposit(event.amount)
    }

    @StateTransitionFunc
    fun transferWithdraw(event: TransferWithdrawPerformedEvent) {
        val bankAccount = bankAccounts[event.bankAccountId]!!
        bankAccount.withdraw(event.amount)
        bankAccount.addTransferWithdraw(event.transferId, event.amount)
    }

    @StateTransitionFunc
    fun rollbackTransferWithdraw(event: TransferWithdrawRollbackedEvent) {
        val bankAccount = bankAccounts[event.bankAccountId]!!
        bankAccount.deposit(event.amount)
        bankAccount.removeTransferWithdraw(event.transferId)
    }

    @StateTransitionFunc
    fun noop(event: NoopEvent) {
        // do nothing
    }

    @StateTransitionFunc
    fun rejectTransferDeposit(event: TransferDepositRejectedEvent) {
        // do nothing
    }

    @StateTransitionFunc
    fun rejectTransferWithdraw(event: TransferWithdrawRejectedEvent) {
        // do nothing
    }

}


data class BankAccount(
    val id: UUID,
    internal var balance: BigDecimal = BigDecimal.ZERO,

    ) {
    private var transferWithdraws: MutableMap<UUID, TransferWithdraw> = mutableMapOf()

    fun deposit(amount: BigDecimal) {
        this.balance = this.balance.add(amount)
    }

    fun withdraw(amount: BigDecimal) {
        this.balance = this.balance.subtract(amount)
    }

    fun addTransferWithdraw(transferId: UUID, amount: BigDecimal) {
        transferWithdraws[transferId] = TransferWithdraw(transferId, amount)
    }

    fun removeTransferWithdraw(transferId: UUID) {
        transferWithdraws.remove(transferId)
    }

    fun checkTransferWithdraw(transferId: UUID, amount: BigDecimal): Boolean {
        val withdraw = transferWithdraws[transferId]

        return withdraw != null && withdraw.amount == amount
    }

}

data class TransferWithdraw(
    val transferId: UUID,
    val amount: BigDecimal,
)