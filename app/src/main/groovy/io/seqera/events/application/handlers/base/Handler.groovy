package io.seqera.events.application.handlers.base

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler

interface Handler extends HttpHandler {

    default Map<String, String> parseQueryParams(String query) {
        Map<String, String> queryParametersMap = [:]
        String[] parameterPairs = query.split('&')

        parameterPairs.each { pair ->
            String[] keyValuePair = pair.split('=')
            if (keyValuePair.size() == 2) {
                queryParametersMap[keyValuePair[0].toLowerCase()] = keyValuePair[1]
            }
        }

        return queryParametersMap
    }

    default String getQueryString(HttpExchange http) {
        return http.requestURI.query
    }

    default String getRequestBody(HttpExchange http) {
        return http.requestBody.text
    }

    String getHandlerPath()

}
