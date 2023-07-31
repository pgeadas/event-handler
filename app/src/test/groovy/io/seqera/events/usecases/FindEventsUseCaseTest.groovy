package io.seqera.events.usecases

import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class FindEventsUseCaseTest {

    private FindEventsUseCase useCase
    private EventRepository eventDao

    @BeforeEach
    void setUp() {
        eventDao = mock(EventRepository.class)
        useCase = new FindEventsUseCase(eventDao)
    }

    @Test
    void """Given that we retrieve a page with specific values
            then the retrieval method is called using the same values"""() {
        PageDetails details = PageDetails.of(1, 1)
        Ordering ordering = Ordering.of("id", true)
        useCase.retrievePage(details, ordering)
        verify(eventDao).retrievePage(details, ordering)
        verifyNoMoreInteractions(eventDao)
    }

    @Test
    void """Given that no values are provided for Ordering
            then should use defaults"""() {
        PageDetails details = PageDetails.of(1, 1)
        Ordering ordering = null
        useCase.retrievePage(details, ordering)
        verify(eventDao).retrievePage(details, ordering)
        verifyNoMoreInteractions(eventDao)
    }
}
