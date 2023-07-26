package io.seqera.events.dao

import groovy.sql.Sql
import groovy.transform.CompileStatic
import io.seqera.events.dto.Event

@CompileStatic
class SqlEventDao implements EventDao {

    private Sql sql;
    def String tableName = "EVENT"

    SqlEventDao(Sql sql) {
        this.sql = sql;
    }

    @Override
    Event save(Event event) {
        String query = """insert into ${tableName}(workspaceId, userId, cpu, mem,io) values ('$event.workspaceId','$event.userId',$event.cpu,$event.mem,$event.io)"""
        def id = sql.executeInsert(query)[0][0] as Long
        event.id = id
        return event
    }

    @Override
    List<Event> list() {
        String query = """select * from ${tableName}"""
        return sql.rows(query).collect { new Event(id: it.id, workspaceId: it.workspaceId, userId: it.userId, mem: it['mem'] as Long, cpu: it.cpu as Long, io: it.io as Long) }

    }
}
