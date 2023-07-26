package io.seqera.events.dto

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
