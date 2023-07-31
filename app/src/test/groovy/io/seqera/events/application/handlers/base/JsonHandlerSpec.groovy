package io.seqera.events.application.handlers.base

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpContext
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpPrincipal
import groovy.json.JsonOutput
import spock.lang.Specification

class JsonHandlerTest extends Specification {

    private FakeJsonHandler handler

    def setup() {
        handler = new FakeJsonHandler()
    }

    def "getHandlerPath should return not null"() {
        when:
        String path = handler.getHandlerPath()

        then:
        path != null
    }


    def "parseText should correctly parse JSON text"() {
        given:
        String jsonText = '{"orderBy": "id", "itemCount": 30}'
        Map parsedData = handler.parseText(jsonText) as Map

        when:
        String orderBy = parsedData['orderBy']
        int itemCount = parsedData['itemCount'] as int

        then:
        orderBy == 'id'
        itemCount == 30
    }

    def "sendResponse should send JSON response with Content-type and length"() {
        given:
        HttpExchange httpExchange = new TestHttpExchange()
        def obj = [orderBy: 'id', itemCount: 30]
        def response = JsonOutput.toJson(obj)

        when:
        JsonHandler.sendResponse(httpExchange, obj, 200)

        then:
        httpExchange.responseHeaders.getFirst("Content-type") == "application/json"
        httpExchange.getResponseCode() == 200
        httpExchange.getResponseLength() == response.getBytes("UTF-8").length
        httpExchange.responseBody != null
        httpExchange.responseBody.toString() == response
    }


    private static class TestHttpExchange extends HttpExchange {
        private ByteArrayOutputStream responseBodyStream = new ByteArrayOutputStream()
        private Headers responseHeaders = new Headers()
        private int responseCode
        private long responseLength

        long getResponseLength() {
            return responseLength
        }

        @Override
        Headers getRequestHeaders() {
            return null
        }

        @Override
        Headers getResponseHeaders() {
            return responseHeaders
        }

        @Override
        URI getRequestURI() {
            return null
        }

        @Override
        String getRequestMethod() {
            return null
        }

        @Override
        HttpContext getHttpContext() {
            return null
        }

        @Override
        void close() {

        }

        @Override
        InputStream getRequestBody() {
            return null
        }

        @Override
        OutputStream getResponseBody() {
            return responseBodyStream
        }

        @Override
        void sendResponseHeaders(int rCode, long responseLength) throws IOException {
            this.responseCode = rCode
            this.responseLength = responseLength
        }

        @Override
        InetSocketAddress getRemoteAddress() {
            return null
        }

        @Override
        int getResponseCode() {
            return responseCode
        }

        @Override
        InetSocketAddress getLocalAddress() {
            return null
        }

        @Override
        String getProtocol() {
            return null
        }

        @Override
        Object getAttribute(String name) {
            return null
        }

        @Override
        void setAttribute(String name, Object value) {

        }

        @Override
        void setStreams(InputStream i, OutputStream o) {

        }

        @Override
        HttpPrincipal getPrincipal() {
            return null
        }

    }

    private static class FakeJsonHandler extends JsonHandler {
        FakeJsonHandler() {
            super()
        }

        @Override
        void handle(HttpExchange http) {
        }

        @Override
        String getHandlerPath() {
            return "/fakePath"
        }
    }

}

