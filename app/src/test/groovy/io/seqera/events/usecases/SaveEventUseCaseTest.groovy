package io.seqera.events.usecases

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource

import java.util.stream.Stream

import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import static org.mockito.Mockito.*

class SaveEventUseCaseTest {

    private SaveEventUseCase useCase
    private EventRepository repository

    @BeforeEach
    void setUp() {
        repository = mock(EventRepository)
        useCase = new SaveEventUseCase(repository)
    }

    @ParameterizedTest
    @ArgumentsSource(EventsArgumentSource)
    void "Should save a given Event"(Event event) {
        useCase.save(event)
        verify(repository).save(event)
        verifyNoMoreInteractions(repository)
    }

    static class EventsArgumentSource implements ArgumentsProvider {

        @Override
        Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(EventsStub.empty()),
                    Arguments.of(EventsStub.full()),
                    Arguments.of(EventsStub.withNullId()),
                    Arguments.of(EventsStub.withNullUserId())
            )
        }

    }

}

