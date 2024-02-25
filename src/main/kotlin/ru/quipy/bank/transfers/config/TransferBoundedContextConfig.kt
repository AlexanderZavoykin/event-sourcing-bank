package ru.quipy.bank.transfers.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.bank.transfers.api.TransferAggregate
import ru.quipy.bank.transfers.logic.Transfer
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.UUID

@Configuration
class TransferBoundedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun transferEsService(): EventSourcingService<UUID, TransferAggregate, Transfer> =
        eventSourcingServiceFactory.create()
}