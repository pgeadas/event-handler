package io.seqera.events.application.handlers.base

import com.sun.net.httpserver.HttpExchange
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
abstract class JsonHandler implements Handler {

    private final JsonSlurper json

    protected JsonHandler() {
        this.json = new JsonSlurper()
    }

    static void sendOkResponse(HttpExchange http, Object obj, int rCode) {
        def response = JsonOutput.toJson(["data": obj])
        sendResponse(http, response, rCode)
    }

    static void sendErrorResponse(HttpExchange http, Object obj, int rCode) {
        def response = JsonOutput.toJson(["error": obj])
        sendResponse(http, response, rCode)
    }

    private static sendResponse(HttpExchange http, String response, int rCode) {
        http.responseHeaders.add("Content-type", "application/json")
        http.sendResponseHeaders(rCode, response.length())
        http.responseBody.withWriter { out ->
            out << response
        }
    }

    Object parseText(String text) {
        return json.parseText(text)
    }
}
