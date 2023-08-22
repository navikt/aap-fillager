package fillager

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.aap.ktor.config.loadConfig
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import javax.sql.DataSource

private val secureLog = LoggerFactory.getLogger("secureLog")

data class Config(
    val database: DbConfig,
)

data class DbConfig(
    val url: String,
    val username: String,
    val password: String
)

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::server).start(wait = true)
}

internal fun Application.server() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val config = loadConfig<Config>()

    install(MicrometerMetrics) { registry = prometheus }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    install(CallLogging) {
        level = Level.INFO
        logger = secureLog
        filter { call -> call.request.path().startsWith("/api") }
    }

    val virusScanClient = VirusScanClient()
    val pdfGen = PdfGen()
    val datasource = initDatasource(config.database)
    migrate(datasource)


    routing {
        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }

            get("/live") {
                call.respond(HttpStatusCode.OK, "oppgavestyring")
            }

            get("/ready") {
                call.respond(HttpStatusCode.OK, "oppgavestyring")
            }
        }

        route("/paabegynt") {
            get {
                //TODO: Hent navn/filreferanse/innsendingsreferanse på filer vi har nå
            }
        }

        route("/{innsendingsreferanse}") {
            post {
                withContext(Dispatchers.IO) {
                    val fil: ByteArray = call.receive<ByteArray>()
                    if (virusScanClient.scan(fil).result == ScanResult.Result.FOUND) {
                        call.respond(406)
                    }
                    pdfGen.bildeTilPfd(fil)

                }
                //TODO: Lagre
                //TODO: returner innsendings referanse og filreferanse
            }
            get {
                //TODO: Returner alle relevante file
            }
        }
    }
}

private fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 3
    minimumIdle = 1
    initializationFailTimeout = 5000
    idleTimeout = 10001
    connectionTimeout = 1000
    maxLifetime = 30001
})

private fun migrate(dataSource: DataSource) {
    Flyway
        .configure()
        .cleanDisabled(false) // TODO: husk å skru av denne før prod
        .cleanOnValidationError(true) // TODO: husk å skru av denne før prod
        .dataSource(dataSource)
        .locations("flyway")
        .load()
        .migrate()
}