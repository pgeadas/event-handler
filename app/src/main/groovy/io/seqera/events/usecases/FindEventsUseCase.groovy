package io.seqera.events.usecases

import io.seqera.events.domain.Event
import io.seqera.events.domain.EventDao

class FindEventsUseCase {

    private EventDao eventDao

    FindEventsUseCase(EventDao eventDao) {
        this.eventDao = eventDao
    }

    List<Event> list() {
        return eventDao.list()
    }
}
