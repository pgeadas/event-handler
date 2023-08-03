package io.seqera.events.infra.sql.repositories


import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails

/** The purpose of the InMemoryEventRepository is just to exemplify how we could easily swap between different Adapters
 * and how we do not need to modify or create new tests by simply testing the repository contract.
 * From UnitTests it is often preferred to use a fake database like this one, so the contract testing ensures that
 * all implementations show the same behaviour **/
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
            eventList.sort(new EventComparator(orderings))
        }

        int to = calculateMaxToValue(pageDetails) as int
        int from = pageDetails.rangeStart() as int

        if (from > to) {
            return []
        }

        return eventList.subList(from, to)
    }

    private int calculateMaxToValue(PageDetails pageDetails) {
        return pageDetails.itemCount > eventList.size() ? eventList.size() : pageDetails.itemCount
    }

}

class EventComparator implements Comparator<Event> {
    private List<Ordering> orderings

    EventComparator(List<Ordering> orderings) {
        this.orderings = orderings
    }

    @Override
    int compare(Event event1, Event event2) {
        int matches = 0
        for (Ordering ord in orderings) {
            if (ord.sortOrder() == 'asc') {
                matches = event1[ord.orderBy] <=> event2[ord.orderBy]
            } else {
                matches = event2[ord.orderBy] <=> event1[ord.orderBy]
            }
            if (matches != 0) {
                break
            }
        }
        return matches
    }

}
