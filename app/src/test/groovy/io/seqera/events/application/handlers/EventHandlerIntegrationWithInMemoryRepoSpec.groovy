package io.seqera.events.application.handlers

import com.sun.net.httpserver.HttpServer
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.infra.sql.repositories.InMemoryEventRepository
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.HttpStatus
import io.seqera.events.utils.QueryParamParser
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class EventHandlerIntegrationWithInMemoryRepoSpec extends Specification {

    @Shared
    private HttpServer httpServer
    @Shared
    private int serverPort = 8080
    @Shared
    private EventRepository repository = new InMemoryEventRepository([])
    @Shared
    private Properties properties = new Properties()

    def setupSpec() {
        EventsStub.eventsList(3).forEach { repository.save(it) }

        EventHandler handler = new EventHandler(
                new FindEventsUseCase(repository),
                new SaveEventUseCase(repository),
                properties, new QueryParamParser()
        )
        httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0).with {
            createContext(handler.handlerPath, handler)
            start()
        }
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = serverPort
    }

    def "test GET request handled in EventHandler"() {
        when:
        def response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderby=id&sort=asc")
        String body = response.asString()

        then:
        response.statusCode() == HttpStatus.Ok.code
        response.contentType() == ContentType.JSON.toString()
        List<String> events = JsonPath.from(body).getList('data')
        events.size() == 3
    }

    @Unroll
    def """given a GET request
        when valid pageNumber and itemCount and orderBy=#orderBy and sort=#sort
        then should return #statusCode code and sort results correctly"""() {
        when:
        Response response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderBy=${orderBy}&sort=${sort}")
        String body = response.asString()

        then:
        response.statusCode == statusCode
        List<Event> events = JsonPath.from(body).getList('data')
        events.size() == 3
        events.stream().collect { it."$orderBy" } == expectedOrder

        where:
        orderBy  | sort   | expectedOrder                     | statusCode
        "id"     | "asc"  | ["0", "1", "2"]                   | HttpStatus.Ok.code
        "id"     | "desc" | ["2", "1", "0"]                   | HttpStatus.Ok.code
        "userId" | "asc"  | ["userId1", "userId2", "userId3"] | HttpStatus.Ok.code
        "userId" | "desc" | ["userId3", "userId2", "userId1"] | HttpStatus.Ok.code
    }

    @Unroll
    def """given a GET request
        when #pageNumber and #itemCount and with #orderBy and #sort
        then should return #statusCode code"""() {
        when:
        Response response = RestAssured.get("/events?${pageNumber}&${itemCount}&${orderBy}&${sort}")

        then:
        response.statusCode == statusCode

        where:
        pageNumber      | itemCount       | orderBy           | sort           | statusCode
        "pageNumber=1"  | "itemCount=1"   | "orderBy=id"      | "sort=asc"     | HttpStatus.Ok.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=id"      | "sort=desc"    | HttpStatus.Ok.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=userId"  | "sort=asc"     | HttpStatus.Ok.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=userId"  | "sort=desc"    | HttpStatus.Ok.code
        "pageNumber=1"  | "itemCount=1"   | []                | "sort=asc"     | HttpStatus.Ok.code
        "pageNumber=1"  | "itemCount=1"   | []                | "sort=desc"    | HttpStatus.Ok.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=id"      | "sort=invalid" | HttpStatus.BadRequest.code
        "pageNumber=-1" | "pageNumber=1"  | "orderBy=id"      | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "pageNumber=-1" | "orderBy=id"      | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "pageNumber=0"  | "orderBy=id"      | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=invalid" | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=invalid" | "sort=desc"    | HttpStatus.BadRequest.code
        " "             | "itemCount=1"   | []                | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | " "             | []                | "sort=desc"    | HttpStatus.BadRequest.code
    }

    @Unroll
    def """given a GET request
        when #pageNumber and #itemCount and without orderBy
        then should return #statusCode code and '#message'"""() {
        when:
        Response response = RestAssured.get("/events?${pageNumber}&${itemCount}")
        def from = JsonPath.from(response.asString())
        String error = "Ok"
        if (from.getString('error')) {
            error = from.getString('error')
        }

        then:
        response.statusCode == statusCode
        error == message

        /* TODO: to properly validate the methods that assert on the error messages returned, we need to load them
                 from properties. For now they are hardcoded */
        where:
        pageNumber           | itemCount           | statusCode                 | message
        "pageNumber=1"       | "itemCount=1"       | HttpStatus.Ok.code         | "Ok"
        "pageNumber=1000"    | "itemCount=1"       | HttpStatus.Ok.code         | "Ok" // no limit
        "pageNumber=1"       | "itemCount=1000"    | HttpStatus.Ok.code         | "Ok"// returns the default max
        "pageNumber=1"       | "itemCount=0"       | HttpStatus.BadRequest.code | "Invalid params: itemCount"
        "pageNumber=0"       | "itemCount=1"       | HttpStatus.BadRequest.code | "Invalid params: pageNumber"
        "pageNumber=1"       | "itemCount=-1"      | HttpStatus.BadRequest.code | "Invalid params: pageNumber/itemCount"
        "pageNumber=-1"      | "itemCount=1"       | HttpStatus.BadRequest.code | "Invalid params: pageNumber/itemCount"
        "pageNumber=invalid" | "itemCount=1"       | HttpStatus.BadRequest.code | "Invalid params: pageNumber"
        "pageNumber=1"       | "itemCount=invalid" | HttpStatus.BadRequest.code | "Invalid params: itemCount"
        " "                  | "itemCount=1"       | HttpStatus.BadRequest.code | "Missing params: pageNumber/itemCount"
        "pageNumber=1"       | " "                 | HttpStatus.BadRequest.code | "Missing params: pageNumber/itemCount"
    }

}
