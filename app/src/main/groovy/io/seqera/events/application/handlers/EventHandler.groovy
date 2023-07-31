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

@CompileStatic
class EventHandler extends JsonHandler {

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

    private final FindEventsUseCase findEventsUseCase
    private final SaveEventUseCase saveEventUseCase
    private final Properties properties
    private final int maxItemCountParsed

    EventHandler(
            FindEventsUseCase findEventsUseCase,
            SaveEventUseCase saveEventUseCase,
            Properties properties) {
        this.findEventsUseCase = findEventsUseCase
        this.saveEventUseCase = saveEventUseCase
        this.properties = properties
        this.maxItemCountParsed = parseMaxItemCountFromProperties(properties)
    }

    private static int parseMaxItemCountFromProperties(Properties properties) {
        try {
            return properties.getProperty(MAX_ITEM_COUNT) as int
        } catch (Exception ignored) {
            return DEFAULT_MAX_ITEM_COUNT
        }
    }

    @Override
    String getHandlerPath() {
        return "/events"
    }

    @Override
    void handle(HttpExchange http) throws IOException {
        switch (http.requestMethod) {
            case "POST" -> {
                handlePost(http)
            }
            case "GET" -> {
                handleGet(http)
            }
            default -> http.sendResponseHeaders(HttpStatus.MethodNotAllowed.code, 0)
        }
    }

    // TODO: open api specs
    // TODO: tests controller
    // TODO: Async processing
    void handleGet(HttpExchange http) {
        def query = getQueryString(http)
        def queryParams = parseQueryParams(query)

        if (!hasPageNumberAndItemCount(queryParams)) {
            sendResponse(http, missingParamsMessageOrDefault("pageNumber/itemCount"), HttpStatus.BadRequest.code)
            return
        }

        Long pageNumber = validatePageNumber(queryParams)
        if (!pageNumber) {
            sendResponse(http, invalidParamsMessageOrDefault("pageNumber"), HttpStatus.BadRequest.code)
            return
        }

        Integer itemCount = validateItemCount(queryParams)
        if (!itemCount) {
            sendResponse(http, invalidParamsMessageOrDefault("itemCount"), HttpStatus.BadRequest.code)
            return
        }

        PageDetails pageDetails = validatePageDetails(pageNumber, itemCount)
        if (!pageDetails) {
            sendResponse(http, invalidParamsMessageOrDefault("pageNumber/itemCount"), HttpStatus.BadRequest.code)
            return
        }

        // TODO: support for multiple columns in orderBy
        Ordering ordering = validateOrdering(queryParams, properties)
        if (!ordering) {
            sendResponse(http, invalidParamsMessageOrDefault("orderBy/asc"), HttpStatus.BadRequest.code)
            return
        }

        def events = retrievePage(pageDetails, ordering)
        if (events) {
            sendResponse(http, events, HttpStatus.Ok.code)
        } else {
            sendResponse(http, internalServerErrorMessageOrDefault(), HttpStatus.InternalServerError.code)
        }

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

    private List<Event> retrievePage(PageDetails pageDetails, Ordering ordering) {
        try {
            return findEventsUseCase.retrievePage(pageDetails, ordering)
        } catch (RuntimeException ex) {
            println "Error fetching events: $ex.stackTrace"
            return null
        }
    }

    private static Ordering validateOrdering(Map<String, String> queryParams, Properties properties) {
        String orderBy
        if (!queryParams["orderby"]) {
            orderBy = properties.getProperty(ORDER_BY, DEFAULT_ORDER_BY)
        } else {
            orderBy = queryParams["orderby"]
        }
        if (isInvalidOrderBy(orderBy)) {
            println "Error validating orderBy: found ${orderBy}"
            return null
        }

        boolean isAscending
        try {
            if (!queryParams["asc"]) {
                isAscending = properties.getProperty(ASCENDING, DEFAULT_ASCENDING).toBoolean()
            } else {
                isAscending = queryParams["asc"].toBoolean()
            }
        } catch (RuntimeException ex) {
            println "Error validating isAscending: ${ex.message}"
            return null
        }

        return Ordering.of(orderBy, isAscending)
    }

    private static boolean isInvalidOrderBy(String orderBy) {
        return orderBy != null && !Event.isFieldNameValid(orderBy)
    }

    private static Long validatePageNumber(Map<String, String> queryParams) {
        try {
            return queryParams["pageNumber"] as Long
        } catch (NumberFormatException ex) {
            println "Error validating pageNumber: ${ex.message}"
            return null
        }
    }

    private Integer validateItemCount(Map<String, String> queryParams) {
        try {
            def itemCount = queryParams["itemCount"] as Integer
            if (itemCount > maxItemCountParsed) {
                println "ItemCount ($itemCount) is bigger than max allowed ($maxItemCountParsed)}"
                return maxItemCountParsed
            }
        } catch (NumberFormatException ex) {
            println "Error validating itemCount: ${ex.message}"
            return null
        }
    }

    private static PageDetails validatePageDetails(long pageNumber, int itemCount) {
        try {
            return PageDetails.of(pageNumber, itemCount)
        } catch (IllegalArgumentException ex) {
            println "Error creating PageDetails: ${ex.message}"
            return null
        }
    }

    private static boolean hasPageNumberAndItemCount(Map<String, String> queryParams) {
        return queryParams["pagenumber"] && queryParams["itemcount"]
    }

    void handlePost(HttpExchange http) {
        def body = getRequestBody(http)
        def event = json.parseText(body) as Event
        if (!event) {
            sendResponse(
                    http,
                    properties.getProperty(INVALID_BODY_MESSAGE, DEFAULT_INVALID_BODY_MESSAGE),
                    HttpStatus.BadRequest.code
            )
            return
        }
        try {
            event = saveEventUseCase.save(event)
            sendResponse(http, event, HttpStatus.Ok.code)
        } catch (RuntimeException ex) {
            println "Error saving event: $ex.stackTrace"
            sendResponse(
                    http,
                    properties.getProperty(INTERNAL_SERVER_ERROR_MESSAGE, DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE),
                    HttpStatus.InternalServerError.code
            )
        }
    }

    @CompileStatic
    class QueryParamValidator {
        private final Properties properties
        private final int maxItemCountParsed

        QueryParamValidator(Properties properties, int maxItemCountParsed) {
            this.properties = properties
            this.maxItemCountParsed = maxItemCountParsed
        }

    }
}
