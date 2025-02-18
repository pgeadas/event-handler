package io.seqera.events.infra.sql.providers

import groovy.sql.Sql
import groovy.transform.TupleConstructor

@TupleConstructor
class ConnectionProviderImpl implements ConnectionProvider {

    String serverUrl
    String username
    String password
    String driver

    @Override
    Sql getConnection() {
        return Sql.newInstance(serverUrl, username, password, driver)
    }
}
