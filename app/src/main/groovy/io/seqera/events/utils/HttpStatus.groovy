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
}
