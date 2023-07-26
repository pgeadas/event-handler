package io.seqera.events.utils.db

import groovy.sql.Sql

interface ConnectionProvider {
    Sql getConnection()
}
