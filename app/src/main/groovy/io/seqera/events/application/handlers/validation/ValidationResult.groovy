package io.seqera.events.application.handlers.validation

class ValidationResult<T> {
    final T value
    final String errorMessage

    ValidationResult(T value) {
        this.value = value
        this.errorMessage = null
    }

    ValidationResult(String errorMessage) {
        this.value = null
        this.errorMessage = errorMessage
    }

    boolean isValid() {
        return value != null
    }
}
