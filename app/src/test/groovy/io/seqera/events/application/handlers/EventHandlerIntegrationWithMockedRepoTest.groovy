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
import static org.mockito.ArgumentMatchers.anyList
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventHandlerIntegrationWithMockedRepoTest {

    private static int serverPort = 8081
    private HttpServer httpServer
    private EventRepository repository = mock(EventRepository)
    private Properties properties = new Properties()

    EventHandlerIntegrationWithMockedRepoTest() {
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
    }

    @AfterAll
    void cleanup() {
        try {
            httpServer.stop(0)
        } catch (ignored) {
        }
    }

    @Test
    void """given events are found
         when doing valid GET request
         then should answer with OK and the results"""() {

        def result = EventsStub.eventsList(3)
        when(repository.retrievePage(any(), any())).thenReturn(result)

        def response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderby=id&sort=asc")
        String body = response.asString()

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        List<String> events = JsonPath.from(body).getList('data')
        Assertions.assertEquals(3, events.size())
    }

    @Test
    void """ given events are not found
         when doing valid GET request
         then should answer with OK and empty results"""() {

        List<Event> result = []
        when(repository.retrievePage(any(), any())).thenReturn(result)

        def response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderby=id&sort=asc")
        String body = response.asString()

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        List<String> events = JsonPath.from(body).getList('data')
        Assertions.assertTrue(events.isEmpty())
    }

    @Test
    void """given query string is missing
         when doing GET request
         then should answer with BadRequest and error"""() {

        List<Event> result = []
        when(repository.retrievePage(any(), any())).thenReturn(result)

        def response = RestAssured.get("/events")
        String body = response.asString()

        Assertions.assertEquals(HttpStatus.BadRequest.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        String error = JsonPath.from(body).getString('error')
        Assertions.assertNotNull(error)
    }

    @Test
    void """given repo throws exception
         then should answer with InternalServerError and error"""() {

        when(repository.retrievePage(any(), anyList())).thenThrow(new RuntimeException("Boing!"))

        def response = RestAssured.get("/events?pageNumber=1&itemCount=1")
        String body = response.asString()

        Assertions.assertEquals(HttpStatus.InternalServerError.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        String error = JsonPath.from(body).getString('error')
        Assertions.assertNotNull(error)
    }

    @Test
    void """given only pageNumber and itemCount
            when repo is empty
            then should answer Ok and empty"""() {

        def result = []
        when(repository.retrievePage(any(), anyList())).thenReturn(result)

        def response = RestAssured.get("/events?pageNumber=100&itemCount=100")
        String body = response.asString()

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        def data = JsonPath.from(body).getList('data')
        Assertions.assertTrue(data.isEmpty())
    }

    @Test
    void """given only pageNumber and itemCount
            when repo has events
            then should answer Ok and not empty"""() {

        int amount = 3
        def result = EventsStub.eventsList(amount)
        when(repository.retrievePage(any(), anyList())).thenReturn(result)

        def response = RestAssured.get("/events?pageNumber=100&itemCount=100")
        String body = response.asString()

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        def data = JsonPath.from(body).getList('data')
        Assertions.assertEquals(amount, data.size())
    }

}
