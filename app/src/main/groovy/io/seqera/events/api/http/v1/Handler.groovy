package io.seqera.events.api.http.v1

import com.sun.net.httpserver.HttpHandler

interface Handler extends HttpHandler {
    String getHandlerPath()
}
