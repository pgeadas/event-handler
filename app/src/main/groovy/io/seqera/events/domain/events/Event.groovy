package io.seqera.events.domain.events

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
