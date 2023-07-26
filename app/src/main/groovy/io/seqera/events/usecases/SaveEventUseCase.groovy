package io.seqera.events.usecases

import io.seqera.events.domain.Event
import io.seqera.events.domain.EventDao

class SaveEventUseCase {

    private EventDao eventDao

    SaveEventUseCase(EventDao eventDao) {
        this.eventDao = eventDao
    }

    Event save(Event event) {
        return eventDao.save(event)
    }
}
