# Event Handler io.seqera.events.App

Seqera is building the next event handling app
to store all the data coming from [Nextflow](http://nextflow.io)
pipelines execution.

The app has been built to be almost dependency-less
except for JDK, Groovy database driver(s) dependencies along with
testing frameworks: unless differently specified in the issue no additional dependencies
should be explicitly added to the project.

The startup of the process is centralized in
the [App.groovy](app/src/main/groovy/io/seqera/events/App.groovy)
which takes care of:

- starting the server
- configuring the [handler](app/src/main/groovy/io/seqera/events/application/handlers/EventHandler.groovy)
- configuring
  the [database connection provider](app/src/main/groovy/io/seqera/events/infra/sql/providers/ConnectionProvider.groovy)
- migration of database tables

A minimal request example can be found using the [event.http](event.http) (IntelliJ internal http client tester)

## Configuration

Configuration is located in the [app/src/main/resources/app.yaml](app/src/main/resources/app.yaml)

## Execute locally

Launch the io.seqera.events.App.groovy main method or use the command `./gradlew app:run` from the root folder

