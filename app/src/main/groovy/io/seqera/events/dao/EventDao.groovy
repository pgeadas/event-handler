package io.seqera.events.dao

import io.seqera.events.dto.Event

interface EventDao {
    Event save(Event event)

    List<Event> list();
}
