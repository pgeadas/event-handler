package io.seqera.events.utils


import spock.lang.Specification

class QueryParamParserTest extends Specification {

    private final QueryParamParser paramParser = new QueryParamParser()

    def "should return the correct map for valid query parameters"() {
        given:
        String query = "orderBy=id&itemCount=30"

        when:
        Map<String, String> queryParams = paramParser.parseQueryParams(query)

        then:
        queryParams.size() == 2
        queryParams["orderby"] == "id"
        queryParams["itemcount"] == "30"
    }

    def "should ignore malformed parameters"() {
        given:
        String query = "orderBy=id&itemCount=30&invalidParam"

        when:
        Map<String, String> queryParams = paramParser.parseQueryParams(query)

        then:
        queryParams.size() == 2
        queryParams["orderby"] == "id"
        queryParams["itemcount"] == "30"
        !queryParams.containsKey("invalidparam")
    }

    def "should handle empty query"() {
        given:
        String query = ""

        when:
        Map<String, String> queryParams = paramParser.parseQueryParams(query)

        then:
        queryParams.isEmpty()
    }

    def "should discard param with no value"() {
        given:
        String query = "orderBy=id&itemCount="

        when:
        Map<String, String> queryParams = paramParser.parseQueryParams(query)

        then:
        queryParams.size() == 1
        queryParams["orderby"] == "id"
    }

    def "should handle query with encoded values"() {
        given:
        String query = "orderBy=name%20desc&itemCount=10%2C20%2C30"

        when:
        Map<String, String> queryParams = paramParser.parseQueryParams(query)

        then:
        queryParams.size() == 2
        queryParams["orderby"] == "name desc"
        queryParams["itemcount"] == "10,20,30"
    }
}
