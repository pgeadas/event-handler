package io.seqera.events.application.handlers

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

import com.sun.net.httpserver.HttpServer
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.HttpStatus
import io.seqera.events.utils.QueryParamParser
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

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
        RestAssured.baseURI = 'http://localhost'
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
        def pageDetails = PageDetails.of(1, 10)
        def orderings = Ordering.of('id', true)
        when(repository.retrievePage(pageDetails, [orderings])).thenReturn(result)

        def response = RestAssured.get('/events?pageNumber=1&itemCount=10&orderby=id&sort=asc')
        def data = JsonPath.from(response.getBody().asString()).getList('data')

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        Assertions.assertNotNull(data)
        verify(repository).retrievePage(pageDetails, [orderings])
    }

    @Test
    void """given events are not found
         when doing valid GET request
         then should answer with OK and empty results"""() {

        List<Event> result = []
        def pageDetails = PageDetails.of(1, 10)
        def orderings = Ordering.of('userId', true)
        when(repository.retrievePage(pageDetails, [orderings])).thenReturn(result)

        def response = RestAssured.get('/events?pageNumber=1&itemCount=10&orderby=userId&sort=asc')
        def data = JsonPath.from(response.getBody().asString()).getList('data')

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        Assertions.assertNotNull(data)
        verify(repository).retrievePage(pageDetails, [orderings])
    }

    @Test
    void """given query string is missing
         when doing GET request
         then should answer with BadRequest and error"""() {

        reset(repository)
        def response = RestAssured.get('/events')
        String error = JsonPath.from(response.asString()).getString('error')

        Assertions.assertEquals(HttpStatus.BadRequest.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        Assertions.assertNotNull(error)
        verify(repository, never()).retrievePage(any(), anyList())
    }

    @Test
    void """given repo throws exception
         then should answer with InternalServerError and error"""() {

        def pageDetails = PageDetails.of(100, 10)
        when(repository.retrievePage(eq(pageDetails), anyList())).thenThrow(new RuntimeException('Boing!'))

        def response = RestAssured.get('/events?pageNumber=100&itemCount=10')
        String error = JsonPath.from(response.asString()).getString('error')

        Assertions.assertEquals(HttpStatus.InternalServerError.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        Assertions.assertNotNull(error)
        verify(repository).retrievePage(pageDetails, [])
    }

    @Test
    void """given only pageNumber and itemCount
            when repo is empty
            then should answer Ok and empty"""() {

        def result = []
        def pageDetails = PageDetails.of(100, 100)
        when(repository.retrievePage(eq(pageDetails), anyList())).thenReturn(result)

        def response = RestAssured.get('/events?pageNumber=100&itemCount=100')
        def data = JsonPath.from(response.asString()).getList('data')

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        Assertions.assertTrue(data.isEmpty())
        verify(repository).retrievePage(pageDetails, [])
    }

    @Test
    void """given only pageNumber and itemCount is bigger than max allowed
            when repo has events
            then should answer Ok and not empty and limit itemCount to the max"""() {

        int amount = 3
        def result = EventsStub.eventsList(amount)
        def pageDetails = PageDetails.of(10, EventHandler.QueryParamValidator.DEFAULT_MAX_ITEM_COUNT)
        when(repository.retrievePage(eq(pageDetails), anyList())).thenReturn(result)

        def response = RestAssured.get('/events?pageNumber=10&itemCount=1000')
        def data = JsonPath.from(response.asString()).getList('data')

        Assertions.assertEquals(HttpStatus.Ok.code, response.statusCode())
        Assertions.assertEquals(ContentType.JSON.toString(), response.contentType())
        Assertions.assertEquals(amount, data.size())
        verify(repository).retrievePage(pageDetails, [])
    }

}
