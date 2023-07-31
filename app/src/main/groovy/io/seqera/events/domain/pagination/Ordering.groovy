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

    @Override
     String toString() {
        return "Ordering{" +
                "orderBy='" + orderBy + '\'' +
                ", isAscending=" + isAscending +
                '}';
    }
}
