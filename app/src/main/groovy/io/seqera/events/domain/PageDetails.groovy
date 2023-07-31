package io.seqera.events.domain

import groovy.transform.CompileStatic

@CompileStatic
class PageDetails {
    final long pageNumber
    final int itemCount

    PageDetails(long pageNumber, int itemCount) {
        this.pageNumber = pageNumber
        this.itemCount = itemCount
    }

    static PageDetails of(long pageNumber, int itemCount) {
        validate(pageNumber, itemCount)
        return new PageDetails(pageNumber, itemCount)
    }

    Long rangeStart() {
        return pageNumber * itemCount - itemCount
    }

    Long rangeEnd() {
        return pageNumber * itemCount - 1
    }

    static validate(long pageNumber, int itemCount) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Invalid pageNumber: ${pageNumber}")
        }
        if (itemCount < 1) {
            throw new IllegalArgumentException("Invalid itemCount: ${itemCount}")
        }
    }



}
