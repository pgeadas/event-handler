package io.seqera.events.infra.sql.migrations

import groovy.sql.Sql
import groovy.transform.CompileStatic
import io.seqera.events.infra.sql.providers.ConnectionProvider

@CompileStatic
class SqlDatabaseMigrator {

    Sql migrateDb(ConnectionProvider connectionProvider, Object databaseConfig) {
        def sql = connectionProvider.getConnection()

        if (databaseConfig['migrations']) {
            def migrationFolder = loadMigrationsFolder(databaseConfig['migrations'] as String)
            def migrationFiles = loadMigrationFilesInOrder(migrationFolder, '.sql')
            migrate(sql, migrationFiles)
        } else {
            println "Migration failed: 'migrations' not found in config file"
        }

        return sql
    }

    private static File[] loadMigrationFilesInOrder(File folder, String extension) {
        return folder.listFiles({ File it -> it.name.endsWith(extension) } as FileFilter)
                .sort { it -> Long.parseLong(it.name) } as File[]
    }

    private File loadMigrationsFolder(String migrationFolder) {
        def url = getClass().classLoader.getResource(migrationFolder)
        if (!url) {
            throw new RuntimeException("Resource not found: $migrationFolder")
        }
        println "Loading migrations folder: ${url.toURI()}"
        if (url.toURI().scheme == "file") {
            return new File(url.toURI())
        }

        // TODO: fix reading from jar
        def inputStream = getClass().getResourceAsStream("/${migrationFolder}")
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        return reader.readLines()
                .findAll { line -> line.endsWith('.sql') }
                .collect { fileName -> new File(fileName) }
                .sort { file -> Long.parseLong(file.name) }
                .first()
    }

    private static void migrate(Sql sql, File[] migrationFiles) {
        migrationFiles.each { sql.execute(it.text) }
    }
}
