package io.seqera.events

import com.sun.net.httpserver.HttpServer
import groovy.transform.CompileStatic
import groovy.yaml.YamlSlurper
import io.seqera.events.application.handlers.EventHandler
import io.seqera.events.application.handlers.base.Handler
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.infra.sql.migrations.SqlDatabaseMigrator
import io.seqera.events.infra.sql.providers.SqlContextProvider
import io.seqera.events.infra.sql.repositories.SqlEventRepository
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.ConnectionProviderFactory
import io.seqera.events.utils.QueryParamParser

@CompileStatic
class App {

    private static final int PORT = 8000
    private static final String EVENT = "EVENT"
    private static final String PROPERTIES_FILENAME = 'messages.properties'

    static void main(String[] args) {
        SqlContextProvider contextProvider = new SqlContextProvider(
                new ConnectionProviderFactory(),
                new SqlDatabaseMigrator(),
                new YamlSlurper()
        )
        def connectionProvider = contextProvider.buildContext()
        def connection = connectionProvider.getConnection()

        EventRepository repository = new SqlEventRepository(connection, EVENT)

        FindEventsUseCase findEventsUseCase = new FindEventsUseCase(repository)
        SaveEventUseCase saveEventUseCase = new SaveEventUseCase(repository)

        def props = loadPropertiesFile(PROPERTIES_FILENAME)
        println "Reading config: $props"

        Handler[] handlers = [new EventHandler(findEventsUseCase, saveEventUseCase, props, new QueryParamParser())]
        def httpServer = startServer(handlers)

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            void run() {
                println 'Server is shutting down'
                if (connection != null) {
                    println 'Closing db connection'
                    connection.close()
                }
                if (httpServer != null) {
                    println 'Stopping server'
                    httpServer.stop(10)
                }
                println 'Server gracefully stopped'
            }
        })
    }

    static HttpServer startServer(Handler[] handlers) {
        def httpServer = HttpServer.create(new InetSocketAddress(PORT), /*max backlog*/ 0)
        println "Server is listening on ${PORT}, hit Ctrl+C to exit gracefully."
        for (def h : handlers) {
            httpServer.createContext(h.handlerPath, h)
        }
        httpServer.start()
        return httpServer
    }

    static Properties loadPropertiesFile(String configName = 'messages.properties') {
        def inputStream = App.classLoader.getResourceAsStream(configName)
        if (!inputStream) {
            throw new RuntimeException("Resource not found: $configName")
        }

        def props = new Properties()
        props.load(inputStream)
        return props
    }

}
