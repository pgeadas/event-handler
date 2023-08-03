package io.seqera.events.usecases

import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.*

class FindEventsUseCaseTest {

    private FindEventsUseCase useCase
    private EventRepository repository

    @BeforeEach
    void setUp() {
        repository = mock(EventRepository.class)
        useCase = new FindEventsUseCase(repository)
    }

    @Test
    void """Given that we retrieve a page with specific values
            then the retrieval method is called using the same values"""() {
        PageDetails details = PageDetails.of(1, 1)
        def ordering = [Ordering.of("id", true)]
        useCase.retrievePage(details, ordering)
        verify(repository).retrievePage(details, ordering)
        verifyNoMoreInteractions(repository)
    }

    @Test
    void """Given that no values are provided for Ordering
            then should use defaults"""() {
        PageDetails details = PageDetails.of(1, 1)
        List<Ordering> ordering = null
        useCase.retrievePage(details, ordering)
        verify(repository).retrievePage(details, ordering)
        verifyNoMoreInteractions(repository)
    }
}
