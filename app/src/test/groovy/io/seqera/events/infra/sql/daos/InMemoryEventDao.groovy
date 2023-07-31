package io.seqera.events.infra.sql.daos

import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import io.seqera.events.domain.Event
import io.seqera.events.domain.EventDao
import io.seqera.events.domain.Ordering
import io.seqera.events.domain.PageDetails

/** The purpose of the InMemoryEventDao is just to exemplify how we could easily swap between different Adapters
 * and how we do not need to modify or create new tests by simply testing the dao (repository) contract.
 * From UnitTests it is often preferred to use a fake database like this one, so the contract testing ensures that
 * all implementations show the same behaviour **/
@CompileStatic
class InMemoryEventDao implements EventDao {

    private List<Event> eventList

    InMemoryEventDao(List<Event> eventList) {
        this.eventList = eventList
    }

    @Override
    Event save(Event event) {
        event.id = eventList.size()
        eventList.add(event)
        return event
    }

    private void sortEventsList(Ordering ordering) {
        EventComparator comparator = new EventComparator(ordering.orderBy)
        if (ordering.isAscending) {
            eventList.sort(comparator)
        } else {
            eventList.sort(comparator.reversed())
        }
    }

    @Override
    List retrievePage(PageDetails pageDetails, @Nullable Ordering ordering) {
        if (!validateArguments(pageDetails, ordering)) {
            return []
        }

        if (ordering) {
            sortEventsList(ordering)
        }

        int to = (pageDetails.itemCount > eventList.size() ? eventList.size() : pageDetails.itemCount) as int
        int from = pageDetails.rangeStart() as int

        if (from > to) {
            return []
        }

        return eventList.subList(from, to)
    }
}


class EventComparator implements Comparator<Event> {
    private String fieldToCompare

    EventComparator(String fieldToCompare) {
        this.fieldToCompare = fieldToCompare
    }

    @Override
    int compare(Event event1, Event event2) {
        Object value1 = event1."$fieldToCompare"
        Object value2 = event2."$fieldToCompare"

        if (value1 == null && value2 == null) {
            return 0
        } else if (value1 == null) {
            return -1
        } else if (value2 == null) {
            return 1
        }

        if (value1 < value2) {
            return -1
        } else if (value1 > value2) {
            return 1
        } else {
            return 0
        }
    }
}
