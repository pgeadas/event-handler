package io.seqera.events.domain.pagination

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class PaginationSpec extends Specification {

    @Subject
    private Pagination<String> pagination = new Pagination<String>() {
        @Override
        List<String> retrievePage(PageDetails pageDetails, List<Ordering> orderings) {
            return []
        }
    }

    @Unroll
    def "should validate arguments correctly for #details, #orderBy, and #validator"() {
        when:
        boolean result = pagination.validateArguments(details, orderBy, validator)

        then:
        result == expected

        where:
        details              | orderBy                                 | validator                     | expected
        PageDetails.of(1, 1) | []                                      | null                          | true
        PageDetails.of(1, 1) | []                                      | { it -> it in ['id', 'mem'] } | true
        PageDetails.of(1, 1) | [Ordering.of('id')]                     | null                          | false
        PageDetails.of(1, 1) | [Ordering.of('io')]                     | { it -> it in ['id', 'mem'] } | false
        PageDetails.of(1, 1) | [Ordering.of('id')]                     | { it -> it in ['id', 'mem'] } | true
        PageDetails.of(1, 1) | [Ordering.of('mem')]                    | { it -> it in ['id', 'mem'] } | true
        PageDetails.of(1, 1) | [Ordering.of('id'), Ordering.of('mem')] | { it -> it in ['id', 'mem'] } | true
        PageDetails.of(1, 1) | [Ordering.of('mem'), Ordering.of('io')] | { it -> it in ['id', 'mem'] } | false
    }

}
