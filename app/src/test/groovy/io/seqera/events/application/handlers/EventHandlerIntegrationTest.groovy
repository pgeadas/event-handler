package io.seqera.events.application.handlers

import com.sun.net.httpserver.HttpServer
import io.restassured.RestAssured
import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.infra.sql.repositories.InMemoryEventRepository
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.QueryParamParser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventHandlerIntegrationTest {

    private static HttpServer httpServer
    private static int serverPort = 8080
    private static boolean SERVER_STARTED = false
    private static List<Event> eventList = []
    private static EventRepository repository = new InMemoryEventRepository(eventList)
    private static FindEventsUseCase findEventsUseCase = new FindEventsUseCase(repository)
    private static Properties properties = new Properties()

    static {
        if (!SERVER_STARTED) {
            EventHandler handler = new EventHandler(findEventsUseCase, new SaveEventUseCase(repository), properties, new QueryParamParser())
            httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0).with {
                createContext(handler.handlerPath, handler)
                start()
            }
            SERVER_STARTED = true
        }

        // Configure RestAssured to use the embedded HTTP server
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = serverPort

        // Save events in the repository for testing
        saveEventsInRepositoryWithDifferentUserIds(3)
    }

    @AfterAll
    void cleanup() {
        try {
            httpServer.stop(0)
        } catch (ignored) {
        }
    }

    private static saveEventsInRepositoryWithDifferentUserIds(int amount) {
        EventsStub.createEventsStringClosure(amount, EventsStub.&withUserId).forEach { repository.save(it) }
    }



}
