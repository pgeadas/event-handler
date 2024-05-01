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

    private static final String ORDER_BY_OR_SORT = 'orderBy/sort'
    private static final String PAGE_NUMBER_OR_ITEM_COUNT = 'pageNumber/itemCount'
    private static final String ITEM_COUNT = 'itemCount'
    private static final String PAGE_NUMBER = 'pageNumber'

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
        return '/events'
    }

    @Override
    void handle(HttpExchange http) throws IOException {
        switch (http.requestMethod) {
        // mega-linter complains because it is set for groovy3...
        //            case "POST" -> handlePost(http)
        //            case "GET" -> handleGet(http)
            case 'POST':
                handlePost(http)
                break
            case 'GET':
                handleGet(http)
                break
            default:
                http.sendResponseHeaders(HttpStatus.MethodNotAllowed.code, 0)
        }
    }

    void handleGet(HttpExchange http) {
        def query = getQueryString(http)
        println "query: $query"
        if (!query) {
            printAndSendBadRequestResponse(http, queryParamValidator.&missingParamsMessageOrDefault, PAGE_NUMBER_OR_ITEM_COUNT)
            return
        }

        def queryParams = paramParser.parseQueryParams(query)

        if (!queryParamValidator.hasPageNumberAndItemCount(queryParams)) {
            printAndSendBadRequestResponse(http, queryParamValidator.&missingParamsMessageOrDefault, PAGE_NUMBER_OR_ITEM_COUNT)
            return
        }

        Long pageNumber = queryParamValidator.validatePageNumber(queryParams)
        if (!pageNumber || pageNumber < 1) {
            printAndSendBadRequestResponse(http, queryParamValidator.&invalidParamsMessageOrDefault, PAGE_NUMBER)
            return
        }

        Integer itemCount = queryParamValidator.validateItemCount(queryParams)
        if (!itemCount || itemCount < 1) {
            printAndSendBadRequestResponse(http, queryParamValidator.&invalidParamsMessageOrDefault, ITEM_COUNT)
            return
        }

        PageDetails pageDetails = queryParamValidator.validatePageDetails(pageNumber, itemCount)
        if (!pageDetails) {
            printAndSendBadRequestResponse(http, queryParamValidator.&invalidParamsMessageOrDefault, PAGE_NUMBER_OR_ITEM_COUNT)
            return
        }

        List<Ordering> orderings = queryParamValidator.validateOrdering(queryParams)
        if (orderings == null) {
            printAndSendBadRequestResponse(http, queryParamValidator.&invalidParamsMessageOrDefault, ORDER_BY_OR_SORT)
            return
        }

        def events = retrievePage(pageDetails, orderings)
        if (events != null) {
            printAndSendOkResponse(pageDetails, orderings, events, http)
        } else {
            printAndSendErrorResponse(http, queryParamValidator.internalServerErrorMessageOrDefault(), HttpStatus.InternalServerError)
        }
    }

    private static void printAndSendOkResponse(PageDetails pageDetails, List<Ordering> orderings, List<Event> events, HttpExchange http) {
        println "Ok ($pageDetails, ${Arrays.toString(orderings)}) -> events=$events"
        sendOkResponse(http, events, HttpStatus.Ok.code)
    }

    private static void printAndSendBadRequestResponse(HttpExchange http, Closure<String> messageProvider, String param) {
        printAndSendErrorResponse(http, messageProvider(param), HttpStatus.BadRequest)
    }

    private static void printAndSendErrorResponse(HttpExchange http, String message, HttpStatus httpStatus) {
        println message
        sendErrorResponse(http, message, httpStatus.code)
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
        // messages
        private static final String INVALID_PARAMS_MESSAGE = 'request.invalid.params'
        static final String DEFAULT_INVALID_PARAMS_MESSAGE = 'Invalid params'

        private static final String MISSING_PARAMS_MESSAGE = 'request.missing.params'
        static final String DEFAULT_MISSING_PARAMS_MESSAGE = 'Missing params'

        private static final String INTERNAL_SERVER_ERROR_MESSAGE = 'internal.server-error'
        static final String DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE = 'Internal server error'

        private static final String INVALID_BODY_MESSAGE = 'request.invalid.body'
        static final String DEFAULT_INVALID_BODY_MESSAGE = 'Invalid request body'

        private static final String MAX_ITEM_COUNT = 'request.get.defaults.item-count'
        static final int DEFAULT_MAX_ITEM_COUNT = 100

        // query param names
        private static final String ASCENDING = 'asc'
        private static final String DEFAULT_SORTING_ORDER = ASCENDING
        private static final String DESCENDING = 'desc'
        private static final String PAGE_NUMBER = 'pagenumber'
        private static final String ITEM_COUNT = 'itemcount'
        private static final String ORDER_BY = 'orderby'
        private static final String SORTING_ORDER = 'sort'

        private final Properties properties
        private final int maxItemCountParsed

        private QueryParamValidator(Properties properties) {
            this.properties = properties
            this.maxItemCountParsed = parseMaxItemCountFromProperties()
        }

        private int parseMaxItemCountFromProperties() {
            String maxItemCount = ''
            try {
                maxItemCount = properties.getProperty(MAX_ITEM_COUNT)
                return maxItemCount as int
            } catch (RuntimeException ignored) {
                println """Error parsing maxItemCount from properties: ${maxItemCount}
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
            return orderBy == null || orderBy.isEmpty() ? ASCENDING
                    : orderBy == ASCENDING ? ASCENDING
                    : orderBy == DESCENDING ? DESCENDING
                    : null
        }

        private static Long validatePageNumber(Map<String, String> queryParams) {
            try {
                return queryParams[PAGE_NUMBER] as Long
            } catch (NumberFormatException ex) {
                println "Error validating pageNumber: $ex.message"
                return null
            }
        }

        private Integer validateItemCount(Map<String, String> queryParams) {
            try {
                Integer itemCount = queryParams[ITEM_COUNT] as Integer
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
            return queryParams[PAGE_NUMBER] && queryParams[ITEM_COUNT]
        }

        private static List<Ordering> validateOrdering(Map<String, String> queryParams) {
            if (!queryParams[ORDER_BY]) {
                return []
            }
            String orderBy = queryParams[ORDER_BY]

            String sortingOrder
            if (!queryParams[SORTING_ORDER]) {
                sortingOrder = DEFAULT_SORTING_ORDER
            } else {
                sortingOrder = queryParams[SORTING_ORDER]
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
            return index < sortingOrder.size() ? sortingOrder[index] == ASCENDING : sortingOrderDefault
        }

    }

}
