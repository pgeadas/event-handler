<<<<<<<< HEAD:app/src/main/groovy/io/seqera/events/domain/events/Event.groovy
package io.seqera.events.domain.events
========
package io.seqera.events.domain
>>>>>>>> 8a76af8 (Refactor code to follow clean architecture):app/src/main/groovy/io/seqera/events/domain/Event.groovy

import groovy.transform.CompileStatic

@CompileStatic
class Event {

    static final def VALID_FIELD_NAMES = ["id", "workspaceId", "userId", "cpu", "mem", "io"]

    String id
    String workspaceId
    String userId
    Long cpu
    Long mem
    Long io

    static Event of(String id, String workspaceId, String userId, Long mem, Long cpu, Long io) {
        return new Event(id: id, workspaceId: workspaceId, userId: userId, mem: mem, cpu: cpu, io: io)
    }

    // TODO: try to use something like Object value1 = event1."$fieldToCompare"? What is more performant?
    static boolean isFieldNameValid(String fieldName) {
        return VALID_FIELD_NAMES.any { it == fieldName }
    }

}
