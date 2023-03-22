package ru.quipy.bank.transfers.api

import ru.quipy.core.annotations.AggregateType
import ru.quipy.domain.Aggregate

@AggregateType(aggregateEventsTableName = "transfers")
class TransferTransactionAggregate: Aggregate