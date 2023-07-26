import com.sun.net.httpserver.HttpServer
import groovy.sql.Sql
import groovy.yaml.YamlSlurper
import io.seqera.events.api.http.v1.EventHandler
import io.seqera.events.api.http.v1.Handler
import io.seqera.events.domain.EventDao
import io.seqera.events.infra.sql.ConnectionProvider
import io.seqera.events.infra.sql.ConnectionProviderImpl
import io.seqera.events.infra.sql.dao.SqlEventDao
import io.seqera.events.usecases.FindEventsUseCase
import io.seqera.events.usecases.SaveEventUseCase
import io.seqera.events.utils.AppContext

class App {

    static PORT = 8000
    static Handler[] handlers
    static HttpServer httpServer
    static AppContext context
    static ConnectionProvider connectionProvider

    static void main(String[] args) {
        context = buildContext()
        EventDao dao = new SqlEventDao(context.connectionProvider.getConnection())
        FindEventsUseCase findEventsUseCase = new FindEventsUseCase(dao)
        SaveEventUseCase saveEventUseCase = new SaveEventUseCase(dao)
        handlers = [new EventHandler(findEventsUseCase, saveEventUseCase)]
        httpServer = startServer()
    }


    static AppContext buildContext() {
        connectionProvider = buildConnectionProvider()
        migrateDb()
        return new AppContext(connectionProvider: connectionProvider)
    }

    static HttpServer startServer() {
        return HttpServer.create(new InetSocketAddress(PORT), /*max backlog*/ 0).with {
            println "Server is listening on ${PORT}, hit Ctrl+C to exit."
            for (def h : handlers) {
                createContext(h.handlerPath, h)
            }
            start()
        }
    }

    static migrateFrom(Sql sql, String migrationFolder) {
        def folder = new File(App.classLoader.getResource(migrationFolder).toURI())
        def migrationFiles = folder.listFiles { it -> it.name.endsWith(".sql") }.sort { Long.parseLong(it) } as File[]
        migrationFiles.each {
            sql.execute(it.text)
        }
    }

    static ConnectionProvider buildConnectionProvider() {
        def file = new File(App.class.getResource('/app.yaml').toURI())
        def conf = new YamlSlurper().parse(file)
        def databaseConfig = conf['app']['database']
        return new ConnectionProviderImpl(serverUrl: databaseConfig['url'], username: databaseConfig['username'],
                password: databaseConfig['password'], driver: databaseConfig['driver'])
    }


    static def migrateDb() {
        def file = new File(App.class.getResource('/app.yaml').toURI())
        def conf = new YamlSlurper().parse(file)
        def databaseConfig = conf['app']['database']
        def sql = connectionProvider.getConnection()
        if (databaseConfig['migrations']) {
            migrateFrom(sql, databaseConfig['migrations'] as String)
        }
        return sql
    }
}
