package io.seqera.events.domain.pagination

import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.Nullable

@CompileStatic
interface Pagination<T> {

    List<T> retrievePage(PageDetails pageDetails, List<Ordering> orderings)

    default boolean validateArguments(
            PageDetails pageDetails,
            List<Ordering> orderings,
            @Nullable Closure<Boolean> columnNameValidator = null) {
        if (pageDetails.itemCount <= 0) {
            println "Validation Error (itemCount): $pageDetails.itemCount"
            return false
        }
        if (pageDetails.pageNumber <= 0) {
            println "Validation Error (pageNumber): $pageDetails.pageNumber"
            return false
        }
        if (orderings.isEmpty()) {
            return true
        } else if (columnNameValidator == null) {
            println 'Validation Error (must provide columnNameValidator when Ordering is enabled) '
            return false
        }
        for (Ordering ordering : orderings) {
            if (!columnNameValidator.call(ordering.orderBy)) {
                println "Validation Error (orderBy): $ordering.orderBy"
                return false
            }
        }
        return true
    }

}
