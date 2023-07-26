<<<<<<<< HEAD:app/src/main/groovy/io/seqera/events/api/http/v1/events/EventHandler.groovy
package io.seqera.events.api.http.v1.events
========
package io.seqera.events.api.http.v1
>>>>>>>> 8a76af8 (Refactor code to follow clean architecture):app/src/main/groovy/io/seqera/events/api/http/v1/EventHandler.groovy

import com.sun.net.httpserver.HttpExchange
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
<<<<<<<< HEAD:app/src/main/groovy/io/seqera/events/api/http/v1/events/EventHandler.groovy
import io.seqera.events.api.http.v1.Handler
import io.seqera.events.domain.events.Event
import io.seqera.events.usecases.find.FindEventsUseCase
import io.seqera.events.usecases.save.SaveEventUseCase
========
import io.seqera.events.domain.Event
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
>>>>>>>> 8a76af8 (Refactor code to follow clean architecture):app/src/main/groovy/io/seqera/events/api/http/v1/EventHandler.groovy

@CompileStatic
class EventHandler implements Handler {

    private FindEventsUseCase findEventsUseCase
    private SaveEventUseCase saveEventUseCase
    private JsonSlurper json

    EventHandler(FindEventsUseCase findEventsUseCase, SaveEventUseCase saveEventUseCase) {
        this.findEventsUseCase = findEventsUseCase
        this.saveEventUseCase = saveEventUseCase
        this.json = new JsonSlurper()
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
            default -> http.sendResponseHeaders(405, 0)
        }
    }

    void handleGet(HttpExchange http) {
        def events = findEventsUseCase.list()
<<<<<<<< HEAD:app/src/main/groovy/io/seqera/events/api/http/v1/events/EventHandler.groovy

========
        okResponse(http, events, 200)
    }

    static void okResponse(HttpExchange http, Object obj, int rCode) {
        // TODO: encapsulate common flow into super class handling json header and parsing
        def response = JsonOutput.toJson(obj)
>>>>>>>> 8a76af8 (Refactor code to follow clean architecture):app/src/main/groovy/io/seqera/events/api/http/v1/EventHandler.groovy
        http.responseHeaders.add("Content-type", "application/json")
        http.sendResponseHeaders(rCode, response.length())
        http.responseBody.withWriter { out ->
            out << response
        }
    }

    void handlePost(HttpExchange http) {
        def body = http.requestBody.text
        def event = this.json.parseText(body) as Event
        event = saveEventUseCase.save(event)
<<<<<<<< HEAD:app/src/main/groovy/io/seqera/events/api/http/v1/events/EventHandler.groovy
        // TODO: encapsulate common flow into super class handling json header and parsing
        http.responseHeaders.add("Content-type", "application/json")
        def response = JsonOutput.toJson(event)
        http.sendResponseHeaders(200, response.length())
        http.responseBody.withWriter { out ->
            out << response
        }
========
        okResponse(http, event, 201)
>>>>>>>> 8a76af8 (Refactor code to follow clean architecture):app/src/main/groovy/io/seqera/events/api/http/v1/EventHandler.groovy
    }
}
