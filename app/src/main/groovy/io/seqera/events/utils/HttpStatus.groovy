package io.seqera.events.utils

enum HttpStatus {

    Ok(200),
    BadRequest(400),
    MethodNotAllowed(405),
    InternalServerError(500)

    final int code

    HttpStatus(int code) {
        this.code = code
    }

    static HttpStatus fromCode(int code) {
        for (HttpStatus status : values()) {
            if (status.code == code) {
                return status
            }
        }
        return null
    }

}
