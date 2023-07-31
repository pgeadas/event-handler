package io.seqera.events.infra.sql.providers

import groovy.sql.Sql

interface ConnectionProvider {
    Sql getConnection()
}
