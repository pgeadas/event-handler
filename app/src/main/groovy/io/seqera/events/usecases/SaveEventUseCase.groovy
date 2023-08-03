package io.seqera.events.usecases

import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository

class SaveEventUseCase {

    private final EventRepository repository

    SaveEventUseCase(EventRepository repository) {
        this.repository = repository
    }

    Event save(Event event) {
        return repository.save(event)
    }
}
