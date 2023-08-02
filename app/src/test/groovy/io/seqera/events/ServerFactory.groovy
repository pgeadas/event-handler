package io.seqera.events

import com.sun.net.httpserver.HttpServer
import io.seqera.events.application.handlers.base.Handler

class ServerFactory {
    private static final Object lock = new Object();
    private static HttpServer httpServer;

//     HttpServer createServer(int serverPort, Handler handler) {
//        synchronized (lock) {
//            if (httpServer == null) {
//                try {
//                    httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0)
//                    httpServer.createContext(handler.handlerPath, handler)
//                    httpServer.start()
//                } catch (Exception ex) {
//                    println "ERROR:" + ex.stackTrace
//                }
//            }
//        }
//        return httpServer
//    }

}
