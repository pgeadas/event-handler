package io.seqera.events.domain

interface EventDao extends Pagination<Event> {

    Event save(Event event)

}
