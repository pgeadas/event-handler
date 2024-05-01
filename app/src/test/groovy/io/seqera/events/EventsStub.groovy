package io.seqera.events

import groovy.transform.CompileStatic
import io.seqera.events.domain.event.Event

@CompileStatic
class EventsStub {

    public static final String ID = 'id'
    public static final String WORKSPACE_ID = 'workspaceId'
    public static final String USER_ID = 'userId'
    public static final long MEM = 10
    public static final long CPU = 10
    public static final int IO = 10

    static List<Event> eventsList(int count) {
        List<Event> events = []
        for (i in 1..count) {
            events << Event.of(
                    "$WORKSPACE_ID${i}",
                    "$USER_ID${i}",
                    MEM - i,
                    CPU * i,
                    IO)
        }
        return events
    }

    static List<Event> eventsListWithId(int count) {
        List<Event> events = []
        for (i in 1..count) {
            events << Event.of(
                    "${i - 1}",
                    "$WORKSPACE_ID${i}",
                    "$USER_ID${i}",
                    MEM - i,
                    CPU * i,
                    IO)
        }
        return events
    }

    static Event full() {
        return Event.of(
                ID,
                WORKSPACE_ID,
                USER_ID,
                MEM,
                CPU,
                IO
        )
    }

    static Event empty() {
        return new Event()
    }

    static Event withNullId() {
        return Event.of(
                null,
                WORKSPACE_ID,
                USER_ID,
                MEM,
                CPU,
                IO
        )
    }

    static Event withNullUserId() {
        return Event.of(
                ID,
                WORKSPACE_ID,
                null,
                MEM,
                CPU,
                IO
        )
    }

}
