package io.seqera.events.usecases.save

import io.seqera.events.domain.events.Event
import io.seqera.events.domain.events.EventDao

class SaveEventUseCase {

    private EventDao eventDao

    SaveEventUseCase(EventDao eventDao) {
        this.eventDao = eventDao
    }

    Event save(Event event) {
        return eventDao.save(event)
    }
}
