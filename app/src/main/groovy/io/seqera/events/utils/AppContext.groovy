package io.seqera.events.utils

import groovy.transform.CompileStatic
import io.seqera.events.utils.db.ConnectionProvider

@CompileStatic
class AppContext {
    ConnectionProvider connectionProvider
}
