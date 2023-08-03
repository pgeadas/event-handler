package io.seqera.events

import com.sun.net.httpserver.HttpServer
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

// TODO: list of improvements (not all related with pagination)
// 1: add Logger instead of print
// 2: add async processing of requests (queue)
// 3: open api specs
// 4: Flyway for automatic migrations
// 5: add a wrapper class for response data
// 6: tests for the properties and defaults
// 8: fix reading properties from resource from a jar

class App {

    private static final PORT = 8000
    private static final String EVENT = "EVENT"
    private static final String PROPERTIES_FILENAME = 'messages.properties'

    static void main(String[] args) {
        SqlContextProvider contextProvider = new SqlContextProvider(
                'app.yaml',
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
        return HttpServer.create(new InetSocketAddress(PORT), /*max backlog*/ 0).with {
            println "Server is listening on ${PORT}, hit Ctrl+C to exit gracefully."
            for (def h : handlers) {
                createContext(h.handlerPath, h)
            }
            start()
        }
    }

    static Properties loadPropertiesFile(String configName) {
        def inputStream = App.classLoader.getResourceAsStream(configName)
        if (!inputStream) {
            throw new RuntimeException("Resource not found: $configName")
        }

        def props = new Properties()
        props.load(inputStream)
        return props
    }

}
