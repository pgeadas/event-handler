package io.seqera.events.domain.pagination

import groovy.transform.CompileStatic;

@CompileStatic
class Ordering {

    final String orderBy
    final boolean isAscending

    private Ordering(String orderBy, boolean isAscending) {
        this.orderBy = orderBy
        this.isAscending = isAscending
    }

    static Ordering of(String orderBy = null, boolean isAscending = true) {
        return new Ordering(orderBy, isAscending)
    }

    String sortOrder() {
        return isAscending ? "asc" : "desc"
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        Ordering ordering = (Ordering) o

        if (isAscending != ordering.isAscending) return false
        if (orderBy != ordering.orderBy) return false

        return true
    }

    int hashCode() {
        int result
        result = (orderBy != null ? orderBy.hashCode() : 0)
        result = 31 * result + (isAscending ? 1 : 0)
        return result
    }

    @Override
    String toString() {
        return "Ordering{" +
                "orderBy='" + orderBy + '\'' +
                ", isAscending=" + isAscending +
                '}';
    }
}
