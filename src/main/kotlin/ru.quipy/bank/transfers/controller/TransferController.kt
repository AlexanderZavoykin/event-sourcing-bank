package ru.quipy.bank.transfers.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.quipy.bank.transfers.api.TransferTransactionCreatedEvent
import ru.quipy.bank.transfers.service.TransactionService
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/transfers")
class TransferController(
    private val transactionService: TransactionService,
) {

    @PostMapping
    fun makeTransfer(
        @RequestParam sourceBankAccountId: UUID,
        @RequestParam destinationBankAccountId: UUID,
        @RequestParam amount: BigDecimal,
    ): TransferTransactionCreatedEvent =
        transactionService.initiateTransferTransaction(sourceBankAccountId, destinationBankAccountId, amount)

}