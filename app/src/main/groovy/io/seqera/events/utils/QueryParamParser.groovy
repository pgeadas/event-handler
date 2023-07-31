package io.seqera.events.utils

import groovy.transform.CompileStatic

@CompileStatic
class QueryParamParser {

    public static final String encoding = "UTF-8"

    Map<String, String> parseQueryParams(String query) {
        Map<String, String> queryParametersMap = [:]
        String[] parameterPairs = query.split('&')

        parameterPairs.each { pair ->
            String[] keyValuePair = pair.split('=')
            if (keyValuePair.size() == 2) {
                String key = decodeQueryParam(keyValuePair[0])
                String value = decodeQueryParam(keyValuePair[1])
                if (key != null && value != null) {
                    queryParametersMap[key.toLowerCase()] = value
                }
            }
        }

        return queryParametersMap
    }

    String decodeQueryParam(String param) {
        try {
            return param ? URLDecoder.decode(param, encoding) : null
        } catch (UnsupportedEncodingException ignored) {
            return param
        }
    }
}
