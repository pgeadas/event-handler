package io.seqera.events.infra.sql.providers

import groovy.transform.CompileStatic
import groovy.yaml.YamlSlurper
import io.seqera.events.infra.sql.SqlDatabaseMigrator
import io.seqera.events.utils.ConnectionProviderFactory

@CompileStatic
class SqlContextProvider {

    private final String configName
    private final ConnectionProviderFactory connectionProviderBuilder
    private final SqlDatabaseMigrator databaseMigrator
    private final YamlSlurper yamlSlurper

    SqlContextProvider(String configName,
                       ConnectionProviderFactory connectionProviderBuilder,
                       SqlDatabaseMigrator databaseMigrator,
                       YamlSlurper yamlSlurper) {
        this.configName = configName
        this.connectionProviderBuilder = connectionProviderBuilder
        this.databaseMigrator = databaseMigrator
        this.yamlSlurper = yamlSlurper
    }

    ConnectionProvider buildContext() {
        def databaseConfig = loadDatabaseConfig(configName)
        def connectionProvider = connectionProviderBuilder.create(databaseConfig)
        databaseMigrator.migrateDb(connectionProvider, databaseConfig)
        return connectionProvider
    }

    /** Assumes that a file named 'configName' exits in the resources folder **/
    private Object loadDatabaseConfig(String configName) {
        def resource = getClass().classLoader.getResourceAsStream(configName)
        if (!resource) {
            throw new RuntimeException("Resource not found: $configName")
        }
        return yamlSlurper.parse(resource)['app']['database']
    }

}
