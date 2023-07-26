<<<<<<<< HEAD:app/src/main/groovy/io/seqera/events/domain/events/Event.groovy
package io.seqera.events.domain.events
========
package io.seqera.events.domain
>>>>>>>> 8a76af8 (Refactor code to follow clean architecture):app/src/main/groovy/io/seqera/events/domain/Event.groovy

import groovy.transform.CompileStatic

@CompileStatic
class Event {

    String id
    String workspaceId
    String userId
    Long cpu
    Long mem
    Long io
}
