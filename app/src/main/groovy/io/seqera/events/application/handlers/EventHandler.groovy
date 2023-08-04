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
        println "query: $query"
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
        if (!pageNumber || pageNumber < 1) {
            println queryParamValidator.invalidParamsMessageOrDefault("pageNumber")
            sendErrorResponse(http, queryParamValidator.invalidParamsMessageOrDefault("pageNumber"), HttpStatus.BadRequest.code)
            return
        }

        Integer itemCount = queryParamValidator.validateItemCount(queryParams)
        if (!itemCount || itemCount < 1) {
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

        List<Ordering> orderings = queryParamValidator.validateOrdering(queryParams)
        if (orderings == null) {
            println queryParamValidator.invalidParamsMessageOrDefault("orderBy/sort")
            sendErrorResponse(http, queryParamValidator.invalidParamsMessageOrDefault("orderBy/sort"), HttpStatus.BadRequest.code)
            return
        }

        def events = retrievePage(pageDetails, orderings)
        if (events != null) {
            println "Ok ($pageDetails, ${Arrays.toString(orderings)}) -> events=$events"
            sendOkResponse(http, events, HttpStatus.Ok.code)
        } else {
            println queryParamValidator.internalServerErrorMessageOrDefault()
            sendErrorResponse(http, queryParamValidator.internalServerErrorMessageOrDefault(), HttpStatus.InternalServerError.code)
        }

    }

    private List<Event> retrievePage(PageDetails pageDetails, List<Ordering> orderings) {
        try {
            return findEventsUseCase.retrievePage(pageDetails, orderings)
        } catch (RuntimeException ex) {
            println "${ex} - ${ex.stackTrace}"
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
            sendErrorResponse(
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
    class QueryParamValidator {

        private static final String INVALID_PARAMS_MESSAGE = 'request.invalid.params'
        static final String DEFAULT_INVALID_PARAMS_MESSAGE = 'Invalid params'

        private static final String MISSING_PARAMS_MESSAGE = 'request.missing.params'
        static final String DEFAULT_MISSING_PARAMS_MESSAGE = 'Missing params'

        private static final String INTERNAL_SERVER_ERROR_MESSAGE = 'internal.server-error'
        static final String DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE = 'Internal server error'

        private static final String INVALID_BODY_MESSAGE = 'request.invalid.body'
        static final String DEFAULT_INVALID_BODY_MESSAGE = 'Invalid request body'

        private static final String MAX_ITEM_COUNT = 'request.get.defaults.itemCount'
        static final int DEFAULT_MAX_ITEM_COUNT = 100

        private static final String DEFAULT_SORTING_ORDER = 'asc'

        private final Properties properties
        private final int maxItemCountParsed

        private QueryParamValidator(Properties properties) {
            this.properties = properties
            this.maxItemCountParsed = parseMaxItemCountFromProperties()
        }

        private int parseMaxItemCountFromProperties() {
            try {
                return properties.getProperty(MAX_ITEM_COUNT) as int
            } catch (RuntimeException ignored) {
                println """Error parsing maxItemCount from properties:
Using default maxItemCount: $DEFAULT_MAX_ITEM_COUNT"""
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

        private static String validateSortingOrder(String orderBy) {
            return orderBy == null || orderBy.isEmpty() ? "asc"
                    : orderBy == 'asc' ? 'asc'
                    : orderBy == 'desc' ? 'desc'
                    : null
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

        private static List<Ordering> validateOrdering(Map<String, String> queryParams) {
            if (!queryParams["orderby"]) {
                return []
            }
            String orderBy = queryParams["orderby"]

            String sortingOrder
            if (!queryParams["sort"]) {
                sortingOrder = DEFAULT_SORTING_ORDER
            } else {
                sortingOrder = queryParams["sort"]
            }

            try {
                return validateOrderings(orderBy, sortingOrder, DEFAULT_SORTING_ORDER)
            } catch (ex) {
                println "Failed to validate Orderings: ${ex}"
                return null
            }
        }

        private static List<Ordering> validateOrderings(String orderBy, String sortingOrder, String sortingOrderDefault) {
            List<String> splitOrderBy = orderBy.split(',').toList()
            for (String ob : splitOrderBy) {
                if (isInvalidOrderBy(ob)) {
                    println "Error validating orderBy: found $ob"
                    return null
                }
            }

            List<String> sortingOrders = []
            for (String so : sortingOrder.split(',')) {
                def validatedSort = validateSortingOrder(so)
                if (validatedSort) {
                    sortingOrders << validatedSort
                } else {
                    println "Error validating sorting order: found $validatedSort"
                    return null
                }
            }

            List<Ordering> orderings = buildOrderings(splitOrderBy, sortingOrders, sortingOrderDefault)

            return orderings
        }

        private static List<Ordering> buildOrderings(List<String> splitOrderBy, List<String> sortingOrder, String sortingOrderDefault) {
            List<Ordering> result = []
            for (int i = 0; i < splitOrderBy.size(); i++) {
                result << Ordering.of(splitOrderBy[i], getIsAscendingOrDefault(sortingOrder, i, sortingOrderDefault))
            }
            return result
        }

        private static boolean getIsAscendingOrDefault(List<String> sortingOrder, int index, String sortingOrderDefault) {
            return index < sortingOrder.size() ? sortingOrder[index] == 'asc' : sortingOrderDefault
        }

    }
}
