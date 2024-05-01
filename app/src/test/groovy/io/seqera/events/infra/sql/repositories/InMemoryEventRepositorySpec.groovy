package io.seqera.events.infra.sql.repositories

import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository

class InMemoryEventRepositorySpec extends EventRepositoryContractSpec {

    @Override
    EventRepository populateDB(List<Event> events) {
        int id = 0
        events.each { it.id = id++ }
        return new InMemoryEventRepository(events)
    }

}
