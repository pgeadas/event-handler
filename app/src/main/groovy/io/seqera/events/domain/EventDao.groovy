package io.seqera.events.domain

interface EventDao {

    Event save(Event event)

    List<Event> list();
}
