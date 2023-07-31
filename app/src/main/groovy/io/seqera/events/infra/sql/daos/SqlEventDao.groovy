package io.seqera.events.infra.sql.daos

import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import io.seqera.events.domain.Event
import io.seqera.events.domain.EventDao
import io.seqera.events.domain.Ordering
import io.seqera.events.domain.PageDetails

import java.sql.PreparedStatement
import java.sql.ResultSet

/** Considerations:
 * I'm using prepared statements only when there is no orderBy, since we cannot prepare a statement
 * when the part that is dynamic is not a value (in this case, a column name). My question is: is it worth it?
 * The disadvantage is that we have now two ways of retrieving data, which might add some overhead to future
 * maintainers. This should be load tested to find out if there are any gains or not.
 * **/
@CompileStatic
class SqlEventDao implements EventDao {

    private static String SELECT = "select id, workspaceId, userId, mem, cpu, io from"

    private final Sql sql
    private final String tableName
    private PreparedStatement retrievePageWithoutOrderBy

    SqlEventDao(Sql sql, String tableName) {
        this.sql = sql
        this.tableName = tableName
        this.retrievePageWithoutOrderBy = sql.getConnection().prepareStatement(prepareQueryWithoutOrderByPaginatoin2())
    }

    @Override
    Event save(Event event) {
        String query = """insert into ${tableName}(workspaceId, userId, cpu, mem, io) 
                          values ('$event.workspaceId','$event.userId',$event.cpu,$event.mem,$event.io)"""
        def id = sql.executeInsert(query)[0][0] as Long
        event.id = id
        return event
    }

    private List<Event> retrievePageWithOrderBy(PageDetails pageDetails, Ordering ordering) {
        def query = prepareQueryWithOrderByPaginatoin(pageDetails, ordering)
        List<Event> results = []
        sql.eachRow(query) { row -> results << toEvent(row) }
        return results
    }

    private List<Event> retrievePageWithoutOrderBy(PageDetails pageDetails) {
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

    @Override
    List<Event> retrievePage(PageDetails pageDetails, @Nullable Ordering ordering) {
        validateArguments(pageDetails, ordering)
        if (ordering) {
            return retrievePageWithOrderBy(pageDetails, ordering)
        } else {
            return retrievePageWithoutOrderBy(pageDetails)
        }
    }

    private GString prepareQueryWithOrderByPaginatoin(PageDetails pageDetails, Ordering ordering) {
        def asc = ordering.isAscending ? 'asc' : 'desc'
        return """${Sql.expand(SELECT)} ${Sql.expand(tableName)} 
                  where id >= ${Sql.expand(pageDetails.rangeStart())}
                  order by ${Sql.expand(ordering.orderBy)} ${Sql.expand(asc)}
                  limit ${Sql.expand(pageDetails.itemCount)}
               """
    }


    private GString prepareQueryWithoutOrderByPaginatoin() {
        return """${Sql.expand(SELECT)} ${Sql.expand(tableName)} 
                  where id >= ?
                  limit ?
               """
    }

    private GString prepareQueryWithoutOrderByPaginatoin2() {
        return """${SELECT} ${tableName} 
                  where id >= ?
                  limit ?
               """
    }

}
