package io.seqera.events.domain.events

interface EventDao {

    Event save(Event event)

    List<Event> list();
}
