package io.seqera.events.usecases

import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository

class SaveEventUseCase {

    private final EventRepository eventDao

    SaveEventUseCase(EventRepository eventDao) {
        this.eventDao = eventDao
    }

    Event save(Event event) {
        return eventDao.save(event)
    }
}
