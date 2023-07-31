package io.seqera.events.domain.event

import io.seqera.events.domain.pagination.Pagination

interface EventRepository extends Pagination<Event> {

    Event save(Event event)

}
