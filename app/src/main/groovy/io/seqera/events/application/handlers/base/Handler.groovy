package io.seqera.events.application.handlers.base

import com.sun.net.httpserver.HttpHandler

interface Handler extends HttpHandler {

    String getHandlerPath()

}
