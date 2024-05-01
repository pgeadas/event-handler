package io.seqera.events.infra.sql.repositories

import java.sql.PreparedStatement
import java.sql.ResultSet

import groovy.sql.Sql
import groovy.transform.CompileStatic
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.domain.pagination.Ordering
import io.seqera.events.domain.pagination.PageDetails

/** Considerations:
 * I'm using prepared statements only when there is no orderBy, since we cannot prepare a statement
 * when the part that is dynamic is not a value (in this case, a column name). My question is: is it worth it?
 * The disadvantage is that we have now two ways of retrieving data, which might add some overhead to future
 * maintainers. This should be load tested to find out if there are any gains or not.
 **/
@CompileStatic
class SqlEventRepository implements EventRepository {

    private static String SELECT = 'select id, workspaceId, userId, mem, cpu, io from'

    private final Sql sql
    private final String tableName
    private PreparedStatement retrievePageWithoutOrderBy

    SqlEventRepository(Sql sql, String tableName = 'EVENT') {
        this.sql = sql
        this.tableName = tableName
        String query = buildQueryWithoutOrdering()
        this.retrievePageWithoutOrderBy = sql.getConnection().prepareStatement(query)
    }

    private String buildQueryWithoutOrdering() {
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
    List<Event> retrievePage(PageDetails pageDetails, List<Ordering> orderings) {
        if (!validateArguments(pageDetails, orderings, Event.&isFieldNameValid)) {
            return []
        }
        return orderings.isEmpty() ?
                retrievePageWithoutOrdering(pageDetails) :
                retrievePageWithOrdering(pageDetails, orderings)
    }

    private List<Event> retrievePageWithOrdering(PageDetails pageDetails, List<Ordering> orderings) {
        def query = buildQueryWithOrdering(pageDetails, orderings)
        List<Event> results = []
        sql.eachRow(query) { row -> results << toEvent(row) }
        return results
    }

    private GString buildQueryWithOrdering(PageDetails pageDetails, List<Ordering> orderings) {
        def orderClause = orderings.collect { ordering ->
            "$ordering.orderBy ${ordering.sortOrder()}"
        }.join(', ')

        def query = """
                ${Sql.expand(SELECT)} ${Sql.expand(tableName)}
                where id >= ${Sql.expand(pageDetails.rangeStart())}
                order by ${Sql.expand(orderClause)}
                limit ${Sql.expand(pageDetails.itemCount)}"""
        println query
        return query
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
