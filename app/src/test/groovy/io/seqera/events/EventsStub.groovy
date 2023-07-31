package io.seqera.events

import groovy.transform.CompileStatic
import io.seqera.events.domain.event.Event

@CompileStatic
class EventsStub {

    public static final String ID = "id"
    public static final String WORKSPACE_ID = "workspaceId"
    public static final String USER_ID = "userId"
    public static final long MEM = 20
    public static final long CPU = 30
    public static final int IO = 40

    static List<Event> createEventsStringClosure(int count, Closure<Event> eventCreationClosure) {
        List<Event> events = []
        for (c in 0..<count) {
            events << eventCreationClosure(c as String)
        }
        return events
    }

    static List<Event> createEventsIntClosure(int count, Closure<Event> eventCreationClosure) {
        List<Event> events = []
        for (c in 0..<count) {
            events << eventCreationClosure(c)
        }
        return events
    }

    static List<Event> createEvents(int count, Closure<Event> eventCreationClosure) {
        List<Event> events = []
        for (c in 0..<count) {
            events << eventCreationClosure()
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

    static Event withUserId(String userId) {
        return Event.of(
                null,
                WORKSPACE_ID,
                userId,
                MEM,
                CPU,
                IO
        )
    }

    static Event withCpu(int cpu) {
        return Event.of(
                null,
                WORKSPACE_ID,
                USER_ID,
                MEM,
                cpu,
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
