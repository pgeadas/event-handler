package io.seqera.events.infra.sql.repositories

import spock.lang.Specification
import spock.lang.Unroll

import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails

abstract class EventRepositoryContractSpec extends Specification {

    abstract EventRepository populateDB(List<Event> events)

    @Unroll
    def """given #details and #ordering
            when events in database
            then should retrieve #itemCount items from repository
            """() {
        given:
        def eventCount = 3
        def repository = populateDB(EventsStub.eventsList(eventCount))

        when:
        def events = repository.retrievePage(details, ordering)

        def eventIds = events.collect { it.id as int }
        def eventUserIds = events.collect { it.userId }
        def eventMemory = events.collect { it.mem as Long }
        def eventIo = events.collect { it.io as Long }

        then:
        events.size() == itemCount
        eventIds == ids
        eventUserIds == userIds
        eventMemory == memory as List<Long>
        eventIo == io as List<Long>

        where:
        details               | ordering                           | ids       | userIds                           | memory    | io           | itemCount
        // itemCount is bigger than the #events in database
        PageDetails.of(1, 10) | []                                 | [0, 1, 2] | ['userId1', 'userId2', 'userId3'] | [9, 8, 7] | [10, 10, 10] | 3
        // itemCount is less than the #events in database
        PageDetails.of(1, 2)  | []                                 | [0, 1]    | ['userId1', 'userId2']            | [9, 8]    | [10, 10]     | details.itemCount
        // pageNumber is greater than the #events in database
        PageDetails.of(3, 2)  | []                                 | []        | []                                | []        | []           | 0
        // invalid column name
        PageDetails.of(1, 3)  | of(['bad', 'mem'], [false, false]) | []        | []                                | []        | []           | 0
        // valid pageDetails and ordering by 1 column ascending and descending
        PageDetails.of(1, 3)  | [Ordering.of('userId', true)]      | [0, 1, 2] | ['userId1', 'userId2', 'userId3'] | [9, 8, 7] | [10, 10, 10] | details.itemCount
        PageDetails.of(1, 3)  | [Ordering.of('userId', false)]     | [2, 1, 0] | ['userId3', 'userId2', 'userId1'] | [7, 8, 9] | [10, 10, 10] | details.itemCount
        // tests below this line: valid pageDetails and ordering by 2 columns ascending and descending
        PageDetails.of(1, 3)  | of(['io', 'id'], [true, true])     | [0, 1, 2] | ['userId1', 'userId2', 'userId3'] | [9, 8, 7] | [10, 10, 10] | details.itemCount
        PageDetails.of(1, 3)  | of(['io', 'id'], [false, false])   | [2, 1, 0] | ['userId3', 'userId2', 'userId1'] | [7, 8, 9] | [10, 10, 10] | details.itemCount
        PageDetails.of(1, 3)  | of(['io', 'id'], [true, false])    | [2, 1, 0] | ['userId3', 'userId2', 'userId1'] | [7, 8, 9] | [10, 10, 10] | details.itemCount
        PageDetails.of(1, 3)  | of(['id', 'id'], [false, false])   | [2, 1, 0] | ['userId3', 'userId2', 'userId1'] | [7, 8, 9] | [10, 10, 10] | details.itemCount
        PageDetails.of(1, 3)  | of(['id', 'mem'], [true, false])   | [0, 1, 2] | ['userId1', 'userId2', 'userId3'] | [9, 8, 7] | [10, 10, 10] | details.itemCount
        PageDetails.of(1, 3)  | of(['id', 'mem'], [false, true])   | [2, 1, 0] | ['userId3', 'userId2', 'userId1'] | [7, 8, 9] | [10, 10, 10] | details.itemCount
        PageDetails.of(1, 3)  | of(['id', 'mem'], [false, false])  | [2, 1, 0] | ['userId3', 'userId2', 'userId1'] | [7, 8, 9] | [10, 10, 10] | details.itemCount
    }

    static List<Ordering> of(List<String> columns, List<Boolean> sortOrders) {
        List<Ordering> orderings = []
        for (i in 0..<columns.size()) {
            orderings << Ordering.of(columns[i], sortOrders[i])
        }
        return orderings
    }

}
