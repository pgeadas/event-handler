package io.seqera.events.infra.sql

import groovy.sql.Sql

interface ConnectionProvider {
    Sql getConnection()
}
