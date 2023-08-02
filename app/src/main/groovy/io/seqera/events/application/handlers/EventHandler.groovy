package io.seqera.events.application.handlers

import com.sun.net.httpserver.HttpExchange
import groovy.transform.CompileStatic
import io.seqera.events.application.handlers.base.JsonHandler
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.HttpStatus
import io.seqera.events.utils.QueryParamParser

// TODO: list of improvements
// 0: support for multiple columns in orderBy. Add and clause to select
// 1: add Logger instead of print
// 2: add async processing of requests (queue)
// 3: open api specs
// 4: Flyway for automatic migrations
// 5: add a wrapper for response data
// 6: tests for the properties and defaults
// 7: use sort=asc and desc

@CompileStatic
class EventHandler extends JsonHandler {

    private final QueryParamValidator queryParamValidator
    private final FindEventsUseCase findEventsUseCase
    private final SaveEventUseCase saveEventUseCase
    private final Properties properties
    private final QueryParamParser paramParser

    EventHandler(
            FindEventsUseCase findEventsUseCase,
            SaveEventUseCase saveEventUseCase,
            Properties properties,
            QueryParamParser paramParser) {
        this.findEventsUseCase = findEventsUseCase
        this.saveEventUseCase = saveEventUseCase
        this.properties = properties
        this.paramParser = paramParser
        this.queryParamValidator = new QueryParamValidator(properties)
    }

    @Override
    String getHandlerPath() {
        return "/events"
    }

    @Override
    void handle(HttpExchange http) throws IOException {
        switch (http.requestMethod) {
            case "POST" -> handlePost(http)
            case "GET" -> handleGet(http)
            default -> http.sendResponseHeaders(HttpStatus.MethodNotAllowed.code, 0)
        }
    }

    void handleGet(HttpExchange http) {
        def query = getQueryString(http)
        if (!query) {
            println queryParamValidator.missingParamsMessageOrDefault("pageNumber/itemCount")
            sendErrorResponse(http, queryParamValidator.missingParamsMessageOrDefault("pageNumber/itemCount"), HttpStatus.BadRequest.code)
            return
        }

        def queryParams = paramParser.parseQueryParams(query)

        if (!queryParamValidator.hasPageNumberAndItemCount(queryParams)) {
            println queryParamValidator.missingParamsMessageOrDefault("pageNumber/itemCount")
            sendErrorResponse(http, queryParamValidator.missingParamsMessageOrDefault("pageNumber/itemCount"), HttpStatus.BadRequest.code)
            return
        }

        Long pageNumber = queryParamValidator.validatePageNumber(queryParams)
        if (!pageNumber) {
            println queryParamValidator.invalidParamsMessageOrDefault("pageNumber")
            sendErrorResponse(http, queryParamValidator.invalidParamsMessageOrDefault("pageNumber"), HttpStatus.BadRequest.code)
            return
        }

        Integer itemCount = queryParamValidator.validateItemCount(queryParams)
        if (!itemCount) {
            println queryParamValidator.invalidParamsMessageOrDefault("itemCount")
            sendErrorResponse(http, queryParamValidator.invalidParamsMessageOrDefault("itemCount"), HttpStatus.BadRequest.code)
            return
        }

        PageDetails pageDetails = queryParamValidator.validatePageDetails(pageNumber, itemCount)
        if (!pageDetails) {
            println queryParamValidator.invalidParamsMessageOrDefault("pageNumber/itemCount")
            sendErrorResponse(http, queryParamValidator.invalidParamsMessageOrDefault("pageNumber/itemCount"), HttpStatus.BadRequest.code)
            return
        }

        Ordering ordering = queryParamValidator.validateOrdering(queryParams, properties)
        if (!ordering) {
            println queryParamValidator.invalidParamsMessageOrDefault("orderBy/asc")
            sendErrorResponse(http, queryParamValidator.invalidParamsMessageOrDefault("orderBy/asc"), HttpStatus.BadRequest.code)
            return
        }

        def events = retrievePage(pageDetails, ordering)
        if (events != null) {
            println "Ok ($pageDetails, $ordering) -> events=$events"
            sendOkResponse(http, events, HttpStatus.Ok.code)
        } else {
            println queryParamValidator.internalServerErrorMessageOrDefault()
            sendErrorResponse(http, queryParamValidator.internalServerErrorMessageOrDefault(), HttpStatus.InternalServerError.code)
        }

    }

    private List<Event> retrievePage(PageDetails pageDetails, Ordering ordering) {
        try {
            return findEventsUseCase.retrievePage(pageDetails, ordering)
        } catch (RuntimeException ex) {
            println "Error fetching events: $ex.stackTrace"
            return null
        }
    }

    void handlePost(HttpExchange http) {
        def body = getRequestBody(http)
        def event = parseText(body) as Event
        if (!event) {
            println "Error parsing body: $body"
            sendOkResponse(
                    http,
                    queryParamValidator.invalidBodyMessageOrDefault(),
                    HttpStatus.BadRequest.code
            )
            return
        }
        try {
            event = saveEventUseCase.save(event)
            sendOkResponse(http, event, HttpStatus.Ok.code)
        } catch (RuntimeException ex) {
            println "Error saving event: $ex.stackTrace"
            sendOkResponse(
                    http,
                    queryParamValidator.internalServerErrorMessageOrDefault(),
                    HttpStatus.InternalServerError.code
            )
        }
    }

    private static String getQueryString(HttpExchange http) {
        return http.requestURI.query
    }

    private static String getRequestBody(HttpExchange http) {
        return http.requestBody.text
    }

    @CompileStatic
    private class QueryParamValidator {

        private static final String INVALID_PARAMS_MESSAGE = 'request.invalid.params'
        private static final String DEFAULT_INVALID_PARAMS_MESSAGE = 'Invalid params'

        private static final String MISSING_PARAMS_MESSAGE = 'request.missing.params'
        private static final String DEFAULT_MISSING_PARAMS_MESSAGE = 'Missing params'

        private static final String INTERNAL_SERVER_ERROR_MESSAGE = 'internal.server-error'
        private static final String DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE = 'Internal server error'

        private static final String INVALID_BODY_MESSAGE = 'request.invalid.body'
        private static final String DEFAULT_INVALID_BODY_MESSAGE = 'Invalid request body'

        private static final String MAX_ITEM_COUNT = 'request.get.defaults.itemCount'
        private static final int DEFAULT_MAX_ITEM_COUNT = 200

        private static final String ORDER_BY = 'request.get.defaults.orderBy'
        private static final String DEFAULT_ORDER_BY = null

        private static final String ASCENDING = 'request.get.defaults.ascending'
        private static final String DEFAULT_ASCENDING = 'true'

        private final Properties properties
        private final int maxItemCountParsed

        private QueryParamValidator(Properties properties) {
            this.properties = properties
            this.maxItemCountParsed = parseMaxItemCountFromProperties()
        }

        private int parseMaxItemCountFromProperties() {
            try {
                return properties.getProperty(MAX_ITEM_COUNT) as int
            } catch (RuntimeException ex) {
                println """Error parsing maxItemCount from properties: $ex.stackTrace.
                        \nUsing default maxItemCount: $DEFAULT_MAX_ITEM_COUNT"""
                return DEFAULT_MAX_ITEM_COUNT
            }
        }

        private String invalidBodyMessageOrDefault() {
            return properties.getProperty(INVALID_BODY_MESSAGE, DEFAULT_INVALID_BODY_MESSAGE)
        }

        private String internalServerErrorMessageOrDefault() {
            return properties.getProperty(INTERNAL_SERVER_ERROR_MESSAGE, DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE)
        }

        private String missingParamsMessageOrDefault(String param) {
            def message = properties.getProperty(MISSING_PARAMS_MESSAGE, DEFAULT_MISSING_PARAMS_MESSAGE)
            return "$message: $param"
        }

        private String invalidParamsMessageOrDefault(String param) {
            def message = properties.getProperty(INVALID_PARAMS_MESSAGE, DEFAULT_INVALID_PARAMS_MESSAGE)
            return "$message: $param"
        }

        private static boolean isInvalidOrderBy(String orderBy) {
            return orderBy != null && !Event.isFieldNameValid(orderBy)
        }

        private static Long validatePageNumber(Map<String, String> queryParams) {
            try {
                return queryParams["pagenumber"] as Long
            } catch (NumberFormatException ex) {
                println "Error validating pageNumber: $ex.message"
                return null
            }
        }

        private Integer validateItemCount(Map<String, String> queryParams) {
            try {
                Integer itemCount = queryParams["itemcount"] as Integer
                if (itemCount > maxItemCountParsed) {
                    println "ItemCount ($itemCount) is bigger than max allowed ($maxItemCountParsed)}"
                    return maxItemCountParsed
                }
                return itemCount
            } catch (NumberFormatException ex) {
                println "Error validating itemCount: $ex.message"
                return null
            }
        }

        private static PageDetails validatePageDetails(long pageNumber, int itemCount) {
            try {
                return PageDetails.of(pageNumber, itemCount)
            } catch (IllegalArgumentException ex) {
                println "Error creating PageDetails: $ex.message"
                return null
            }
        }

        private static boolean hasPageNumberAndItemCount(Map<String, String> queryParams) {
            return queryParams["pagenumber"] && queryParams["itemcount"]
        }

        private static Ordering validateOrdering(Map<String, String> queryParams, Properties properties) {
            String orderBy
            if (!queryParams["orderby"]) {
                orderBy = properties.getProperty(ORDER_BY, DEFAULT_ORDER_BY)
            } else {
                orderBy = queryParams["orderby"]
            }
            if (isInvalidOrderBy(orderBy)) {
                println "Error validating orderBy: found $orderBy"
                return null
            }

            boolean isAscending
            if (!queryParams["asc"]) {
                isAscending = Boolean.parseBoolean(properties.getProperty(ASCENDING, DEFAULT_ASCENDING))
            } else {
                isAscending = Boolean.parseBoolean(queryParams["asc"])
            }

            return Ordering.of(orderBy, isAscending)
        }
    }
}
