package io.seqera.events.utils

import groovy.transform.CompileStatic
import io.seqera.events.infra.sql.providers.ConnectionProvider
import io.seqera.events.infra.sql.providers.ConnectionProviderImpl

@CompileStatic
class ConnectionProviderFactory {

    ConnectionProvider create(Object databaseConfig) {
        return new ConnectionProviderImpl(
                serverUrl: databaseConfig['url'],
                username: databaseConfig['username'],
                password: databaseConfig['password'],
                driver: databaseConfig['driver']
        )
    }
}
