<<<<<<<< HEAD:app/src/main/groovy/io/seqera/events/domain/events/EventDao.groovy
package io.seqera.events.domain.events
========
package io.seqera.events.domain
>>>>>>>> 8a76af8 (Refactor code to follow clean architecture):app/src/main/groovy/io/seqera/events/domain/EventDao.groovy

interface EventDao {

    Event save(Event event)

    List<Event> list();
}
