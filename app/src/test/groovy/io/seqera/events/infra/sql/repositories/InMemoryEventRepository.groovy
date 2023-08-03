package io.seqera.events.infra.sql.repositories

import groovy.transform.CompileStatic
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails

/** The purpose of the InMemoryEventRepository is just to exemplify how we could easily swap between different Adapters
 * and how we do not need to modify or create new tests by simply testing the repository contract.
 * From UnitTests it is often preferred to use a fake database like this one, so the contract testing ensures that
 * all implementations show the same behaviour **/
@CompileStatic
class InMemoryEventRepository implements EventRepository {

    private List<Event> eventList

    InMemoryEventRepository(List<Event> eventList) {
        this.eventList = eventList
    }

    @Override
    Event save(Event event) {
        event.id = eventList.size()
        eventList.add(event)
        return event
    }

    @Override
    List retrievePage(PageDetails pageDetails, List<Ordering> orderings) {
        if (!validateArguments(pageDetails, orderings, Event.&isFieldNameValid)) {
            return []
        }

        if (!orderings.isEmpty()) {
            if (orderings[0].isAscending) {
                eventList.sort(new EventComparator(orderings[0].orderBy))
            } else {
                eventList.sort(new EventComparator(orderings[0].orderBy).reversed())
            }
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

        return value1 == null && value2 == null ? 0
                : value1 == null ? -1
                : value2 == null ? 1
                : value1 <=> value2
    }
}
