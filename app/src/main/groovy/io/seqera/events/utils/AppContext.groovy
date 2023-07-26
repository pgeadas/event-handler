package io.seqera.events.utils

import groovy.transform.CompileStatic
import io.seqera.events.infra.sql.ConnectionProvider

@CompileStatic
class AppContext {
    ConnectionProvider connectionProvider
}
