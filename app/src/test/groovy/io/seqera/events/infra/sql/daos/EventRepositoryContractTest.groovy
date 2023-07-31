package io.seqera.events.infra.sql.daos

import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails
import org.assertj.core.api.Assertions as AssertJAssertions
import org.junit.jupiter.api.Assertions as JUnit5Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class EventRepositoryContractTest {

    abstract EventRepository populateDB(List<Event> events)

    @Test
    void """given pageNumber is valid
            when less than itemCount events in database
            then should retrieve all"""() {

        def details = PageDetails.of(1, 10)
        def ordering = null
        def eventCount = 3

        def dao = populateDB(EventsStub.createEvents(eventCount, EventsStub.&full))

        def events = dao.retrievePage(details, ordering)

        JUnit5Assertions.assertEquals(eventCount, events.size())
    }

    @Test
    void """given pageNumber is valid
            when more than itemCount events in database
            then should retrieve only itemCount"""() {

        def details = PageDetails.of(1, 2)
        def ordering = null
        def eventCount = 3

        def dao = populateDB(EventsStub.createEvents(eventCount, EventsStub.&full))

        def events = dao.retrievePage(details, ordering)

        JUnit5Assertions.assertEquals(details.itemCount, events.size())
    }

    @Test
    void """given pageNumber is valid
            when pageNumber is higher than the number of events in database
            then should not retrieve events"""() {

        def details = PageDetails.of(2, 2)
        def ordering = null

        def dao = populateDB(List.of(EventsStub.full()))

        def events = dao.retrievePage(details, ordering)

        JUnit5Assertions.assertEquals(0, events.size())
    }


    @Test
    void """given database has events
            when retrieved
            then events should have id"""() {

        def eventCount = 2
        def details = PageDetails.of(1, 10)
        def ordering = null

        def dao = populateDB(EventsStub.createEvents(eventCount, EventsStub.&withNullId))

        def events = dao.retrievePage(details, ordering)

        JUnit5Assertions.assertEquals(2, events.size())
        JUnit5Assertions.assertNotNull(events.get(0).id)
        JUnit5Assertions.assertNotNull(events.get(1).id)
    }

    @ParameterizedTest
    @CsvSource([
            'false, 2, 1, 0',
            'true, 0, 1, 2',
    ])
    void """given database has events with userId
            when retrieved and order by userId ascending or descending
            then events should be ordered"""(boolean isAscending, String userId0, String userId1, String userId2) {

        def eventCount = 3
        def details = PageDetails.of(1, eventCount)
        Ordering ordering = Ordering.of("userId", isAscending)

        def dao = populateDB(EventsStub.withEventsString(eventCount, EventsStub.&withUserId))

        def events = dao.retrievePage(details, ordering)

        JUnit5Assertions.assertEquals(3, events.size())
        JUnit5Assertions.assertEquals(userId0, events.get(0).userId)
        JUnit5Assertions.assertEquals(userId1, events.get(1).userId)
        JUnit5Assertions.assertEquals(userId2, events.get(2).userId)
    }

    @ParameterizedTest
    @CsvSource([
            'false, 2, 1, 0',
            'true, 0, 1, 2',
    ])
    void """given database has events with cpu
            when retrieved and order by cpu ascending or descending
            then events should be ordered"""(boolean isAscending, Long cpu0, Long cpu1, Long cpu2) {

        def eventCount = 3
        def details = PageDetails.of(1, eventCount)
        Ordering ordering = Ordering.of("cpu", isAscending)

        def dao = populateDB(EventsStub.createEventsInt(eventCount, EventsStub.&withCpu))

        def events = dao.retrievePage(details, ordering)

        JUnit5Assertions.assertEquals(3, events.size())
        JUnit5Assertions.assertEquals(cpu0, events.get(0).cpu)
        JUnit5Assertions.assertEquals(cpu1, events.get(1).cpu)
        JUnit5Assertions.assertEquals(cpu2, events.get(2).cpu)
    }

    @Test
    void """given database has events
            when retrieved without explicit order by
            then events should be ordered by id"""() {

        def eventCount = 3
        def details = PageDetails.of(1, eventCount)
        Ordering ordering = null

        def dao = populateDB(EventsStub.createEventsInt(eventCount, EventsStub.&withCpu))

        def events = dao.retrievePage(details, ordering)

        JUnit5Assertions.assertEquals(3, events.size())
        def list = events.collect { it.id as int }
        AssertJAssertions.assertThat(list).isSortedAccordingTo(Comparator.naturalOrder())
    }

}
