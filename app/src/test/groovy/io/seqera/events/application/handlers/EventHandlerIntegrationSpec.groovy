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

/** For these tests we could have just Mocked the UseCases and just assert that the right parameters reach them,
 * returning the adequate response if that is the case. This is due to the fact that all interactions with our
 * repositories are already tested in their respective files.
 * **/
class EventHandlerIntegrationSpec extends Specification {

    @Shared
    private  HttpServer httpServer
    @Shared
    private  int serverPort = 8083
    @Shared
    private  EventRepository repository = new InMemoryEventRepository([])
    @Shared
    private  Properties properties = new Properties()

    def setupSpec() {
        EventHandler handler = new EventHandler(new FindEventsUseCase(repository), new SaveEventUseCase(repository), properties, new QueryParamParser())
        httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0).with {
            createContext(handler.handlerPath, handler)
            start()
        }
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = serverPort
        saveEventsInRepositoryWithDifferentUserIds(3)
    }

    def "test GET request handled in EventHandler"() {
        when:
        def response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderby=id&asc=true")
        String body = response.asString()

        then:
        response.statusCode() == HttpStatus.Ok.code
        response.contentType() == ContentType.JSON.toString()
        List<String> events = JsonPath.from(body).getList('data')
        events.size() == 3
    }

    @Unroll
    def """given a GET request
        when valid pageNumber and itemCount and orderBy=#orderBy and isAscending=#isAsc
        then should return #statusCode code and sort results correctly"""() {
        when:
        Response response = RestAssured.get("/events?pageNumber=1&itemCount=10&orderBy=${orderBy}&asc=${isAsc}")
        String body = response.asString()

        then:
        response.statusCode == statusCode
        List<Event> events = JsonPath.from(body).getList('data')
        events.size() == 3
        events.stream().mapToInt { it."$orderBy" as int }.collect() == expected

        where:
        orderBy  | isAsc | expected  | statusCode
        "id"     | true  | [0, 1, 2] | HttpStatus.Ok.code
        "id"     | false | [2, 1, 0] | HttpStatus.Ok.code
        "userId" | true  | [0, 1, 2] | HttpStatus.Ok.code
        "userId" | false | [2, 1, 0] | HttpStatus.Ok.code

    }

    @Unroll
    def """given a GET request
        when pageNumber=#pageNumber and itemCount=#itemCount and with orderBy=#orderBy and isAscending=#isAsc
        then should return #statusCode code"""() {
        when:
        Response response = RestAssured.get("/events?pageNumber=${pageNumber}&itemCount=${itemCount}&orderBy=${orderBy}&asc=${isAsc}")

        then:
        response.statusCode == statusCode

        where:
        pageNumber | itemCount | orderBy   | isAsc     | statusCode
        1          | 1         | "id"      | "invalid" | HttpStatus.Ok.code // invalid will be evaluated as false
        1          | 1         | "id"      | true      | HttpStatus.Ok.code
        1          | 1         | "id"      | false     | HttpStatus.Ok.code
        1          | 1         | "userId"  | true      | HttpStatus.Ok.code
        1          | 1         | "userId"  | false     | HttpStatus.Ok.code
        -1         | 1         | "id"      | true      | HttpStatus.BadRequest.code
        1          | -1        | "id"      | true      | HttpStatus.BadRequest.code
        1          | 0         | "id"      | true      | HttpStatus.BadRequest.code
        1          | 1         | "invalid" | true      | HttpStatus.BadRequest.code
        1          | 1         | "invalid" | false     | HttpStatus.BadRequest.code
        1          | 1         | null      | true      | HttpStatus.BadRequest.code
        1          | 1         | null      | false     | HttpStatus.BadRequest.code
    }

    @Unroll
    def """given a GET request
        when pageNumber=#pageNumber and itemCount=#itemCount and without orderBy
        then should return #statusCode code"""() {
        when:
        Response response = RestAssured.get("/events?pageNumber=${pageNumber}&itemCount=${itemCount}")

        then:
        response.statusCode == statusCode

        where:
        pageNumber | itemCount | statusCode
        1          | 1         | HttpStatus.Ok.code
        1          | 0         | HttpStatus.BadRequest.code
        0          | 1         | HttpStatus.BadRequest.code
        1          | -1        | HttpStatus.BadRequest.code
        -1         | 1         | HttpStatus.BadRequest.code
        _          | 1         | HttpStatus.BadRequest.code
        1          | _         | HttpStatus.BadRequest.code
    }

    private saveEventsInRepositoryWithDifferentUserIds(int amount) {
        EventsStub.createEventsStringClosure(amount, EventsStub.&withUserId).forEach { repository.save(it) }
    }

}
