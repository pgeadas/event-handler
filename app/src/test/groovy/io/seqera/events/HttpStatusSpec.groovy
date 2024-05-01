package io.seqera.events

import spock.lang.Specification

import io.seqera.events.utils.HttpStatus

class HttpStatusSpec extends Specification {

    def "should have correct HTTP codes for enum values"() {
        expect:
        HttpStatus.Ok.code == 200
        HttpStatus.BadRequest.code == 400
        HttpStatus.MethodNotAllowed.code == 405
        HttpStatus.InternalServerError.code == 500
    }

    def "should get correct enum value from HTTP code"() {
        expect:
        HttpStatus.fromCode(code) == expectedStatus

        where:
        code | expectedStatus
        200  | HttpStatus.Ok
        400  | HttpStatus.BadRequest
        405  | HttpStatus.MethodNotAllowed
        500  | HttpStatus.InternalServerError
        300  | null
    }

}
