package io.seqera.events.domain;

class Ordering {

    final String orderBy
    final boolean isAscending

    Ordering(String orderBy, boolean isAscending) {
        this.orderBy = orderBy
        this.isAscending = isAscending
    }

    static Ordering of(String orderBy, boolean isAscending) {
        return new Ordering(orderBy, isAscending)
    }
}
