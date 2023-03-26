package ru.quipy.bank.accounts.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.quipy.bank.accounts.dto.TransferInfoResponse
import ru.quipy.bank.accounts.dto.TransferState
import ru.quipy.bank.accounts.projections.TransferProjectionService
import ru.quipy.bank.saga.service.TransferService
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/transfers")
class TransferController(
    private val transferService: TransferService,
    private val transferProjectionService: TransferProjectionService,
) {

    @PostMapping
    fun initiateTransfer(
        @RequestParam sourceBankAccountId: UUID,
        @RequestParam destinationBankAccountId: UUID,
        @RequestParam amount: BigDecimal,
    ): TransferInfoResponse =
        transferService.initiateTransfer(sourceBankAccountId, destinationBankAccountId, amount)

    @GetMapping("/{transferId}")
    fun getTransfer(@PathVariable transferId: UUID): TransferInfoResponse =
        transferProjectionService.findById(transferId)
            ?.let {
                TransferInfoResponse(
                    transferId = it.transferId,
                    sourceBankAccountId = it.sourceBankAccountId,
                    destinationBankAccountId = it.destinationBankAccountId,
                    amount = it.amount,
                    state = TransferState.valueOf(it.state.name)
                )
            }
            ?: throw IllegalArgumentException("Transfer $transferId is not found")

}