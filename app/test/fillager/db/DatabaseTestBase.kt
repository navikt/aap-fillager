package fillager.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

internal object InitTestDatabase {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:14")
    private val flyway: Flyway

    internal val dataSource: DataSource

    init {
        postgres.start()
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 3
            minimumIdle = 1
            initializationFailTimeout = 5000
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })

        flyway = Flyway.configure().dataSource(dataSource).locations("flyway").load().apply { migrate() }
    }
}

internal abstract class DatabaseTestBase {
    @BeforeEach
    fun clearTables() {
        InitTestDatabase.dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE innsending_fil, fil, innsending").use { preparedStatement ->
                preparedStatement.execute()
            }
        }
    }
}
