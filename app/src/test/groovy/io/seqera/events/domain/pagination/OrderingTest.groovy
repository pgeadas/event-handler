package io.seqera.events.domain.pagination

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OrderingTest {

    @Test
    void """"given same orderBy and ascending
             then should be equals"""() {
        def o1 = Ordering.of('id', true)
        def o2 = Ordering.of('id', true)
        Assertions.assertEquals(o1, o2)
        Assertions.assertEquals(o1.hashCode(), o2.hashCode())
    }

    @Test
    void """"given different orderBy and ascending
             then should be equals"""() {
        def o1 = Ordering.of('id', true)
        def o2 = Ordering.of('id2', true)
        Assertions.assertNotEquals(o1, o2)
        Assertions.assertNotEquals(o1.hashCode(), o2.hashCode())
    }

}
