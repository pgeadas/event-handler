package io.seqera.events.domain

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PageDetailsTest {

    @ParameterizedTest
    @CsvSource([
            "-1",
            "0"]
    )
    void """"given pageNumber is less than 1
             then should throw exception"""(long pageNumber) {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PageDetails.of(pageNumber, 1),
                "Invalid pageNumber: ${pageNumber}"
        )
    }

    @ParameterizedTest
    @CsvSource([
            "-1",
            "0"]
    )
    void """"given itemCount is less than 1
             then should throw exception"""() {
        def itemCount = -1
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PageDetails.of(1, itemCount),
                "Invalid itemCount: ${itemCount}"
        )
    }

}
