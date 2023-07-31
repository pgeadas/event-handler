package io.seqera.events.infra.sql.daos

import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails

import java.sql.PreparedStatement
import java.sql.ResultSet

/** Considerations:
 * I'm using prepared statements only when there is no orderBy, since we cannot prepare a statement
 * when the part that is dynamic is not a value (in this case, a column name). My question is: is it worth it?
 * The disadvantage is that we have now two ways of retrieving data, which might add some overhead to future
 * maintainers. This should be load tested to find out if there are any gains or not.
 **/
@CompileStatic
class SqlEventRepository implements EventRepository {

    private static String SELECT = "select id, workspaceId, userId, mem, cpu, io from"

    private final Sql sql
    private final String tableName
    private PreparedStatement retrievePageWithoutOrderBy

    SqlEventRepository(Sql sql, String tableName) {
        this.sql = sql
        this.tableName = tableName
        def query = buildQueryWithoutOrdering()
        this.retrievePageWithoutOrderBy = sql.getConnection().prepareStatement(query)
    }

    private GString buildQueryWithoutOrdering() {
        return """${SELECT} ${tableName} 
                  where id >= ?
                  limit ?
               """
    }

    @Override
    Event save(Event event) {
        String query = """insert into ${tableName}(workspaceId, userId, cpu, mem, io) 
                          values ('$event.workspaceId','$event.userId',$event.cpu,$event.mem,$event.io)"""
        def id = sql.executeInsert(query)[0][0] as Long
        event.id = id
        return event
    }

    @Override
    List<Event> retrievePage(PageDetails pageDetails, @Nullable Ordering ordering) {
        validateArguments(pageDetails, ordering, Event.&isFieldNameValid)
        if (ordering) {
            return retrievePageWithOrdering(pageDetails, ordering)
        } else {
            return retrievePageWithoutOrdering(pageDetails)
        }
    }

    private List<Event> retrievePageWithOrdering(PageDetails pageDetails, Ordering ordering) {
        def query = buildQueryWithOrdering(pageDetails, ordering)
        List<Event> results = []
        sql.eachRow(query) { row -> results << toEvent(row) }
        return results
    }

    private GString buildQueryWithOrdering(PageDetails pageDetails, Ordering ordering) {
        def asc = ordering.isAscending ? 'asc' : 'desc'
        return """${Sql.expand(SELECT)} ${Sql.expand(tableName)} 
                  where id >= ${Sql.expand(pageDetails.rangeStart())}
                  order by ${Sql.expand(ordering.orderBy)} ${Sql.expand(asc)}
                  limit ${Sql.expand(pageDetails.itemCount)}
               """
    }

    private List<Event> retrievePageWithoutOrdering(PageDetails pageDetails) {
        retrievePageWithoutOrderBy.setLong(1, pageDetails.rangeStart())
        retrievePageWithoutOrderBy.setInt(2, pageDetails.itemCount)
        ResultSet resultSet = retrievePageWithoutOrderBy.executeQuery()
        return toEventList(resultSet)
    }

    private static List<Event> toEventList(ResultSet resultSet) {
        List<Event> events = []
        while (resultSet.next()) {
            events << toEvent(resultSet)
        }

        resultSet.close()
        return events
    }

    private static Event toEvent(ResultSet resultSet) {
        Event.of(
                resultSet.getInt('id') as String,
                resultSet.getString('workspaceId'),
                resultSet.getString('userId'),
                resultSet.getLong('mem'),
                resultSet.getLong('cpu'),
                resultSet.getLong('io')
        )
    }


}
