package io.seqera.events.usecases.find

import io.seqera.events.domain.events.Event
import io.seqera.events.domain.events.EventDao

class FindEventsUseCase {

    private EventDao eventDao

    FindEventsUseCase(EventDao eventDao) {
        this.eventDao = eventDao
    }

    List<Event> list() {
        return eventDao.list()
    }
}
