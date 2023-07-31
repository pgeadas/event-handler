package io.seqera.events.usecases

import io.seqera.events.domain.Event
import io.seqera.events.domain.EventDao
import io.seqera.events.domain.Ordering
import io.seqera.events.domain.PageDetails

class FindEventsUseCase {

    private final EventDao eventDao

    FindEventsUseCase(EventDao eventDao) {
        this.eventDao = eventDao
    }

    List<Event> retrievePage(PageDetails pageDetails, Ordering ordering = null) {
        return eventDao.retrievePage(pageDetails, ordering)
    }
}
