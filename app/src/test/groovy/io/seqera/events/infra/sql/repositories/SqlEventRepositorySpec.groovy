package io.seqera.events.infra.sql.repositories

import groovy.sql.Sql
import groovy.yaml.YamlSlurper
import io.seqera.events.domain.event.Event
import io.seqera.events.domain.event.EventRepository
import io.seqera.events.infra.sql.migrations.SqlDatabaseMigrator
import io.seqera.events.infra.sql.providers.SqlContextProvider
import io.seqera.events.utils.ConnectionProviderFactory
import spock.lang.Shared

class SqlEventRepositorySpec extends EventRepositoryContractSpec {
    private static final String CONFIG_NAME = 'app-test.yaml'
    private static final String TABLE_NAME = "EVENT"
    @Shared
    private EventRepository repository
    @Shared
    private Sql connection

    def setupSpec() {
        SqlContextProvider contextProvider = new SqlContextProvider(
                CONFIG_NAME,
                new ConnectionProviderFactory(),
                new SqlDatabaseMigrator(),
                new YamlSlurper()
        )
        def connectionProvider = contextProvider.buildContext()
        connection = connectionProvider.getConnection()
        repository = new SqlEventRepository(connection, TABLE_NAME)
    }

    def cleanup() {
        String query = "TRUNCATE TABLE ${TABLE_NAME}"
        connection.execute(query)
        query = "ALTER TABLE ${TABLE_NAME} ALTER COLUMN id RESTART WITH 0"
        connection.execute(query)
    }


    void cleanupSpec() {
        connection.close()
    }

    @Override
    EventRepository populateDB(List<Event> events) {
        for (Event event : events) {
            String query = """insert into ${TABLE_NAME}(workspaceId, userId, cpu, mem, io) 
                              values ('$event.workspaceId','$event.userId',$event.cpu,$event.mem,$event.io)"""
            connection.executeInsert(query)[0][0] as Long
        }
        return repository
    }
}
