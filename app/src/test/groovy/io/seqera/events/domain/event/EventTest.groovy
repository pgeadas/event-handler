package io.seqera.events.domain.event

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource

import java.util.stream.Stream

class EventTest {

    @Test
    void """Given null fieldName
            when checking if a field name is valid
            then should return false """() {
        String fieldName = null
        Assertions.assertFalse(Event.isFieldNameValid(fieldName))
    }

    @Test
    void """Given an invalid fieldName
            when checking if a field name is valid
            then should return false """() {
        String fieldName = 'invalid'
        Assertions.assertFalse(Event.isFieldNameValid(fieldName))
    }

    @ParameterizedTest
    @ArgumentsSource(EventFieldNamesArgumentsSource)
    void """Given a valid fieldName
            when checking if a field name is valid
            then should return true """(String fieldName) {
        Assertions.assertTrue(Event.isFieldNameValid(fieldName))
    }

}

class EventFieldNamesArgumentsSource implements ArgumentsProvider {

    @Override
    Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Event.VALID_FIELD_NAMES.collect { it -> Arguments.of(it) }.stream()
    }

}
