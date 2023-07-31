package io.seqera.events.domain

import groovyjarjarantlr4.v4.runtime.misc.Nullable

interface Pagination<T> {

    List<T> retrievePage(PageDetails pageDetails, @Nullable Ordering ordering)

    default boolean validateArguments(PageDetails pageDetails, Ordering ordering) {
        if (pageDetails.itemCount <= 0) {
            println "Validation Error (itemCount): ${pageDetails.itemCount}"
            return false
        }
        if (pageDetails.pageNumber < 0) {
            println "Validation Error (pageNumber): ${pageDetails.pageNumber}"
            return false
        }
        if (ordering && !Event.isFieldNameValid(ordering.orderBy)) {
            println "Validation Error (orderBy): ${ordering.orderBy}"
            return false
        }
        return true
    }

}
