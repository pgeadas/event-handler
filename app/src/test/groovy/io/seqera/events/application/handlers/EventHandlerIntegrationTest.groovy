package io.seqera.events.application.handlers

import com.sun.net.httpserver.HttpServer
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.HttpStatus
import io.seqera.events.utils.QueryParamParser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventHandlerIntegrationTest {

    private static int serverPort = 8080
    private HttpServer httpServer
    // Could not make this Mocking work with Spock :)
    private EventRepository repository = mock(EventRepository)
    private Properties properties = new Properties()

    EventHandlerIntegrationTest() {
        EventHandler handler = new EventHandler(
                new FindEventsUseCase(repository),
                new SaveEventUseCase(repository),
                properties,
                new QueryParamParser()
        )
        httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0).with {
            createContext(handler.handlerPath, handler)
            start()
        }
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = serverPort
        saveEventsInRepositoryWithDifferentUserIds(3)
    }

    @AfterAll
    void cleanup() {
        try {
            httpServer.stop(0)
        } catch (ignored) {
        }
    }

    @Test
    void """ given events are found
         when valid GET request
         then should answer with OK and the results"""() {

        def result = EventsStub.createEvents(3, EventsStub.&full)
        when(repository.retrievePage(any(), any())).thenReturn(result)

        def response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderby=id&asc=true")
        String body = response.asString()

        Assertions.assertEquals(response.statusCode(), HttpStatus.Ok.code)
        Assertions.assertEquals(response.contentType(), ContentType.JSON.toString())
        List<String> events = JsonPath.from(body).getList('data')
        Assertions.assertEquals(3, events.size())
    }

    @Test
    void """ given events are not found
         when valid GET request
         then should answer with OK and empty results"""() {

        List<Event> result = []
        when(repository.retrievePage(any(), any())).thenReturn(result)

        def response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderby=id&asc=true")
        String body = response.asString()

        Assertions.assertEquals(response.statusCode(), HttpStatus.Ok.code)
        Assertions.assertEquals(response.contentType(), ContentType.JSON.toString())
        List<String> events = JsonPath.from(body).getList('data')
        Assertions.assertTrue(events.isEmpty())
    }

    @Test
    void """ given repo throws exception
         then should answer with InternalServerError and error"""() {

        when(repository.retrievePage(any(), any())).thenThrow(new RuntimeException("Boing!"))

        def response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderby=id&asc=true")
        String body = response.asString()

        Assertions.assertEquals(response.statusCode(), HttpStatus.InternalServerError.code)
        Assertions.assertEquals(response.contentType(), ContentType.JSON.toString())
        String error = JsonPath.from(body).getString('error')
        Assertions.assertNotNull(error)
    }

    private saveEventsInRepositoryWithDifferentUserIds(int amount) {
        EventsStub.createEventsStringClosure(amount, EventsStub.&withUserId).forEach { repository.save(it) }
    }

}
