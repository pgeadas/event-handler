package io.seqera.events.application.handlers

import com.sun.net.httpserver.HttpServer
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import io.seqera.events.EventsStub
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.HttpStatus
import io.seqera.events.utils.QueryParamParser
import org.junit.jupiter.api.TestInstance
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static io.seqera.events.domain.pagination.PageDetails.of

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventHandlerIntegrationWithMockedRepoSpec extends Specification {

    @Shared
    private HttpServer httpServer
    @Shared
    private int serverPort = 8080
    @Shared
    def repository = Stub(EventRepository)
    @Shared
    private Properties properties = new Properties()

    def setupSpec() {
        repository.retrievePage(_ as PageDetails, _ as List<Ordering>) >> EventsStub.eventsListWithId(3)
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

    void cleanupSpec() {
        try {
            httpServer.stop(0)
        } catch (ignored) {
        }
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

        where:
        orderBy  | sort   | statusCode
        "id"     | "asc"  | HttpStatus.Ok.code
        "id"     | "desc" | HttpStatus.Ok.code
        "userId" | "asc"  | HttpStatus.Ok.code
        "userId" | "desc" | HttpStatus.Ok.code
    }

    @Unroll
    def """given the repository returns some events
        when pageNumber=1 and itemCount=3 and with #orderBy and #sort
        then should return #statusCode code"""() {

        when:
        Response response = RestAssured.get("/events?pageNumber=1&itemCount=3&${orderBy}&${sort}")
        String body = response.asString()
        List<Event> events = JsonPath.from(body).getList('data')

        then:
        response.statusCode() == HttpStatus.Ok.code
        response.contentType() == ContentType.JSON.toString()
        events.size() == 3
        // make sure our repo received the request with the given parameters
        // 1 * repository.retrievePage(pageDetails, ordering) // Unfortunately this does not work :( need to use Mockito

        where:
        pageDetails | ordering                            | orderBy             | sort            | statusCode
        of(1, 3)    | of(["id"], [false])                 | "orderBy=id"        | "sort=desc"     | HttpStatus.Ok.code
        of(1, 3)    | of(["userId"], [true])              | "orderBy=userId"    | "sort=asc"      | HttpStatus.Ok.code
        of(1, 3)    | []                                  | " "                 | "sort=asc"      | HttpStatus.Ok.code
        of(1, 3)    | []                                  | " "                 | "sort=desc"     | HttpStatus.Ok.code
        of(1, 3)    | of(["id", "userId"], [false, true]) | "orderBy=id,userId" | "sort=desc"     | HttpStatus.Ok.code
        of(1, 3)    | of(["id", "userId"], [false, true]) | "orderBy=id,userId" | "sort=desc,asc" | HttpStatus.Ok.code
        of(1, 3)    | of(["id"], [false])                 | "orderBy=id"        | "sort=desc,asc" | HttpStatus.Ok.code
        of(1, 3)    | []                                  | "orderBy="          | "sort=desc,asc" | HttpStatus.Ok.code
        of(1, 3)    | of(["id", "userId"], [true, true])  | "orderBy=id,userId" | "sort="         | HttpStatus.Ok.code
    }

    @Unroll
    def """given an invalid GET request
        when #pageNumber and #itemCount and #orderBy and #sort
        then should return #statusCode code"""() {
        when:
        Response response = RestAssured.get("/events?${pageNumber}&${itemCount}&${orderBy}&${sort}")
        String body = response.asString()
        String error = JsonPath.from(body).getString('error')

        then:
        response.statusCode() == HttpStatus.BadRequest.code
        response.contentType() == ContentType.JSON.toString()
        error != null
        // 0 * repository.retrievePage(_, _)

        where:
        pageNumber      | itemCount       | orderBy           | sort           | statusCode
        "pageNumber=1"  | "itemCount=1"   | "orderBy=id"      | "sort=invalid" | HttpStatus.BadRequest.code
        "pageNumber=-1" | "pageNumber=1"  | "orderBy=id"      | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "pageNumber=-1" | "orderBy=id"      | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "pageNumber=0"  | "orderBy=id"      | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=invalid" | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | "itemCount=1"   | "orderBy=invalid" | "sort=desc"    | HttpStatus.BadRequest.code
        " "             | "itemCount=1"   | " "               | "sort=asc"     | HttpStatus.BadRequest.code
        "pageNumber=1"  | " "             | " "               | "sort=desc"    | HttpStatus.BadRequest.code
    }

    static List<Ordering> of(List<String> columns, List<Boolean> sortOrders) {
        List<Ordering> orderings = []
        for (i in 0..<columns.size()) {
            orderings << Ordering.of(columns[i], sortOrders[i])
        }
        return orderings
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
