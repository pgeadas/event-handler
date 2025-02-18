package io.seqera.events.usecases


import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails

class FindEventsUseCase {

    private final EventRepository repository

    FindEventsUseCase(EventRepository repository) {
        this.repository = repository
    }

    List<Event> retrievePage(PageDetails pageDetails, List<Ordering> orderings) {
        return repository.retrievePage(pageDetails, orderings)
    }
}
