package io.seqera.events.application.handlers

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

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
import static io.seqera.events.domain.pagination.PageDetails.of

class EventHandlerIntegrationWithMockedRepoSpec extends Specification {

    @Shared
    private HttpServer httpServer
    @Shared
    private int serverPort = 8080
    @Shared
    def repository = Stub(EventRepository)
    @Shared
    private Properties properties = new Properties()
    @Shared
    private String defaultMissingParams = EventHandler.QueryParamValidator.DEFAULT_MISSING_PARAMS_MESSAGE
    @Shared
    private String defaultInvalidParams = EventHandler.QueryParamValidator.DEFAULT_INVALID_PARAMS_MESSAGE

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
        RestAssured.baseURI = 'http://localhost'
        RestAssured.port = serverPort
    }

    void cleanupSpec() {
        try {
            httpServer.stop(0)
        } catch (ignored) {
        }
    }

    @Unroll
    def """given the repository returns some events
        when doing valid GET with pageNumber=1 and itemCount=3 and with #orderBy and #sort
        then should return #statusCode"""() {

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
        of(1, 3)    | []                                  | 'orderBy=id'        | ' '             | HttpStatus.Ok.code
        of(1, 3)    | []                                  | 'orderBy='          | ' '             | HttpStatus.Ok.code
        of(1, 3)    | []                                  | ' '                 | 'sort=asc'      | HttpStatus.Ok.code
        of(1, 3)    | []                                  | ' '                 | 'sort=desc'     | HttpStatus.Ok.code
        of(1, 3)    | []                                  | 'orderBy='          | 'sort=desc,asc' | HttpStatus.Ok.code
        of(1, 3)    | of(['id'], [false])                 | 'orderBy='          | 'sort='         | HttpStatus.Ok.code
        of(1, 3)    | of(['id'], [false])                 | 'orderBy=id'        | 'sort=desc'     | HttpStatus.Ok.code
        of(1, 3)    | of(['id'], [false])                 | 'orderBy=id'        | 'sort=desc,asc' | HttpStatus.Ok.code
        of(1, 3)    | of(['userId'], [true])              | 'orderBy=userId'    | 'sort=asc'      | HttpStatus.Ok.code
        of(1, 3)    | of(['id', 'userId'], [false, true]) | 'orderBy=id,userId' | 'sort=desc'     | HttpStatus.Ok.code
        of(1, 3)    | of(['id', 'userId'], [false, true]) | 'orderBy=id,userId' | 'sort=desc,asc' | HttpStatus.Ok.code
        of(1, 3)    | of(['id', 'userId'], [true, true])  | 'orderBy=id,userId' | 'sort='         | HttpStatus.Ok.code
    }

    @Unroll
    def """given the repository returns some events
        when doing invalid GET with #pageNumber and #itemCount and #orderBy and #sort
        then should return #statusCode and '#message'"""() {
        when:
        Response response = RestAssured.get("/events?${pageNumber}&${itemCount}&${orderBy}&${sort}")
        String body = response.asString()
        String error = JsonPath.from(body).getString('error')

        then:
        response.statusCode() == HttpStatus.BadRequest.code
        response.contentType() == ContentType.JSON.toString()
        error == message
        // 0 * repository.retrievePage(_, _)

        where:
        pageNumber      | itemCount      | orderBy           | sort           | statusCode                 | message
        'pageNumber=1'  | 'itemCount=1'  | 'orderBy=id'      | 'sort=invalid' | HttpStatus.BadRequest.code | "$defaultInvalidParams: orderBy/sort"
        'pageNumber=1'  | 'itemCount=1'  | 'orderBy=invalid' | 'sort=desc'    | HttpStatus.BadRequest.code | "$defaultInvalidParams: orderBy/sort"
        'pageNumber=1'  | 'itemCount=1'  | 'orderBy=invalid' | 'sort=asc'     | HttpStatus.BadRequest.code | "$defaultInvalidParams: orderBy/sort"
        'pageNumber=-1' | 'itemCount=1'  | 'orderBy=id'      | 'sort=asc'     | HttpStatus.BadRequest.code | "$defaultInvalidParams: pageNumber"
        'pageNumber=0'  | 'itemCount=0'  | 'orderBy=id'      | 'sort=asc'     | HttpStatus.BadRequest.code | "$defaultInvalidParams: pageNumber"
        'pageNumber=1'  | 'itemCount=-1' | 'orderBy=id'      | 'sort=asc'     | HttpStatus.BadRequest.code | "$defaultInvalidParams: itemCount"
        'pageNumber=1'  | 'itemCount=0'  | 'orderBy=id'      | 'sort=asc'     | HttpStatus.BadRequest.code | "$defaultInvalidParams: itemCount"
        ' '             | 'itemCount=1'  | ' '               | 'sort=asc'     | HttpStatus.BadRequest.code | "$defaultMissingParams: pageNumber/itemCount"
        'pageNumber=1'  | ' '            | ' '               | 'sort=desc'    | HttpStatus.BadRequest.code | "$defaultMissingParams: pageNumber/itemCount"
    }

    static List<Ordering> of(List<String> columns, List<Boolean> sortOrders) {
        List<Ordering> orderings = []
        for (i in 0..<columns.size()) {
            orderings << Ordering.of(columns[i], sortOrders[i])
        }
        return orderings
    }

}
