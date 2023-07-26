package io.seqera.events.handler

import com.sun.net.httpserver.HttpHandler

interface Handler extends HttpHandler {
    String getHandlerPath()
}
