package io.seqera.events.infra.sql.daos

import io.seqera.events.domain.Event
import io.seqera.events.domain.EventDao

class InMemoryEventDaoTest extends EventRepositoryContractTest {

    @Override
    EventDao populateDB(List<Event> events) {
        int id = 0
        events.each { it.id = id++ }
        return new InMemoryEventDao(new ArrayList<Event>(events))
    }
}
