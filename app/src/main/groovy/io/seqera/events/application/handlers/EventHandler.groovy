package io.seqera.events.application.handlers

import com.sun.net.httpserver.HttpExchange
import groovy.transform.CompileStatic
import io.seqera.events.application.handlers.base.JsonHandler
import io.seqera.events.domain.Event
import io.seqera.events.domain.Ordering
import io.seqera.events.domain.PageDetails
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.HttpStatus

@CompileStatic
class EventHandler extends JsonHandler {

    private static String INVALID_PARAMS = 'request.invalid.params'
    private static String MISSING_PARAMS = 'request.missing.params'
    private static String INVALID_BODY = 'request.invalid.body'
    private static String INTERNAL_SERVER_ERROR = 'internal.server-error'

    private static String DEFAULT_MAXROWS = 'request.get.defaults.maxRows'
    private static final int MAX_MAXROWS = 500 // hard limit in case we cant read the default from properties
    private static String DEFAULT_ORDERBY = 'request.get.defaults.orderBy'
    private static String DEFAULT_ASCENDING = 'request.get.defaults.ascending'

    private final FindEventsUseCase findEventsUseCase
    private final SaveEventUseCase saveEventUseCase
    private final Properties properties

    EventHandler(
            FindEventsUseCase findEventsUseCase,
            SaveEventUseCase saveEventUseCase,
            Properties properties) {
        this.findEventsUseCase = findEventsUseCase
        this.saveEventUseCase = saveEventUseCase
        this.properties = properties
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

    // TODO: comment on the open api that num<1=1 and < Integer.MaxSize
    // TODO: open api specs
    // TODO: indexes, cache
    // TODO: create constants for the query params
    // TODO: tests controller
    // TODO: Async processing
    void handleGet(HttpExchange http) {
        def query = getQueryString(http)
        def queryParams = parseQueryParameters(query)

        if (!queryParams["pagenumber"]) {
            sendResponse(http, properties.get(MISSING_PARAMS), HttpStatus.BadRequest.code)
            return
        }
        long pageNumber
        try {
            pageNumber = queryParams["pagenumber"] as long
        } catch (RuntimeException ignored) {
            sendResponse(http, properties.get(INVALID_PARAMS), HttpStatus.BadRequest.code)
            return
        }

        if (!queryParams["itemcount"]) {
            sendResponse(http, properties.get(MISSING_PARAMS), HttpStatus.BadRequest.code)
            return
        }
        int itemCount
        try {
            itemCount = queryParams["itemcount"] as int
        } catch (RuntimeException ignored) {
            sendResponse(http, properties.get(INVALID_PARAMS), HttpStatus.BadRequest.code)
            return
        }

        PageDetails pageDetails
        try {
            pageDetails = pageDetails.of(pageNumber, itemCount)
        } catch (RuntimeException ignored) {
            sendResponse(http, properties.get(INVALID_PARAMS), HttpStatus.BadRequest.code)
            return
        }

        // TODO: support for multiple columns in orderBy
        String orderBy
        if (!queryParams["orderby"]) {
            orderBy = properties.get(DEFAULT_ORDERBY)
            if (!orderBy || orderBy == "null") {
                orderBy = null
            }
        } else {
            orderBy = queryParams["orderby"]
        }
        if (orderBy != null && !Event.isFieldNameValid(orderBy)) {
            sendResponse(http, properties.get(INVALID_PARAMS), HttpStatus.BadRequest.code)
            return
        }

        boolean ascending
        try {
            if (queryParams["asc"] == null) {
                ascending = (properties.get(DEFAULT_ASCENDING) as String).toBoolean()
            } else {
                ascending = queryParams["asc"].toBoolean()
            }
        } catch (RuntimeException ignored) {
            sendResponse(http, properties.get(INVALID_PARAMS), HttpStatus.BadRequest.code)
            return
        }

        Ordering ordering = Ordering.of(orderBy, ascending)

        try {
            def events = findEventsUseCase.retrievePage(pageDetails, ordering)
            sendResponse(http, events, HttpStatus.Ok.code)
        } catch (RuntimeException ex) {
            println "Error:" + ex.cause
            println ex.stackTrace
            sendResponse(http, properties.get(INTERNAL_SERVER_ERROR), HttpStatus.InternalServerError.code)
        }

    }

    void handlePost(HttpExchange http) {
        def body = getRequestBody(http)
        def event = json.parseText(body) as Event
        if (!event) {
            sendResponse(http, properties.get(INVALID_BODY), HttpStatus.BadRequest.code)
            return
        }
        try {
            event = saveEventUseCase.save(event)
            sendResponse(http, event, HttpStatus.Ok.code)
        } catch (RuntimeException ex) {
            println "Error:" + ex.cause
            println ex.stackTrace
            sendResponse(http, properties.get(INTERNAL_SERVER_ERROR), HttpStatus.InternalServerError.code)
        }
    }
}
