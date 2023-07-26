package io.seqera.events.api.http.v1.events

import com.sun.net.httpserver.HttpExchange
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.seqera.events.api.http.v1.Handler
import io.seqera.events.domain.events.Event
import io.seqera.events.usecases.find.FindEventsUseCase
import io.seqera.events.usecases.save.SaveEventUseCase

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

        http.responseHeaders.add("Content-type", "application/json")
        def response = JsonOutput.toJson(events)
        http.sendResponseHeaders(200, response.length())
        http.responseBody.withWriter { out ->
            out << response
        }
    }

    void handlePost(HttpExchange http) {
        def body = http.requestBody.text
        def event = this.json.parseText(body) as Event
        event = saveEventUseCase.save(event)
        // TODO: encapsulate common flow into super class handling json header and parsing
        http.responseHeaders.add("Content-type", "application/json")
        def response = JsonOutput.toJson(event)
        http.sendResponseHeaders(200, response.length())
        http.responseBody.withWriter { out ->
            out << response
        }
    }
}
