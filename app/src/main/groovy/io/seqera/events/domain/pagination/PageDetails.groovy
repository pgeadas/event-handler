package io.seqera.events.domain.pagination

import groovy.transform.CompileStatic

@CompileStatic
class PageDetails {
    final long pageNumber
    final int itemCount

    private PageDetails(long pageNumber, int itemCount) {
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

    static validate(long pageNumber, int itemCount) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Invalid pageNumber: ${pageNumber}")
        }
        if (itemCount < 1) {
            throw new IllegalArgumentException("Invalid itemCount: ${itemCount}")
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        PageDetails that = (PageDetails) o

        if (itemCount != that.itemCount) return false
        if (pageNumber != that.pageNumber) return false

        return true
    }

    int hashCode() {
        int result
        result = (int) (pageNumber ^ (pageNumber >>> 32))
        result = 31 * result + itemCount
        return result
    }

    @Override
    String toString() {
        return "PageDetails{" +
                "pageNumber=" + pageNumber +
                ", itemCount=" + itemCount +
                '}';
    }
}
