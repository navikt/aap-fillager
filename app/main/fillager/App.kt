package fillager

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fillager.db.Repo
import fillager.domene.Innsending
import fillager.filhandtering.PdfGen
import fillager.filhandtering.ScanResult
import fillager.filhandtering.VirusScanClient
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
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
import java.util.*
import javax.sql.DataSource

private val secureLog = LoggerFactory.getLogger("secureLog")
private val logger = LoggerFactory.getLogger("App")

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

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil", cause)
            call.respondText(text = "Feil i tjeneste: ${cause.message}" , status = HttpStatusCode.InternalServerError)
        }
    }

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
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val callId = call.request.header("x-callId") ?: call.request.header("nav-callId") ?: "ukjent"
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, callId: $callId"
        }
        filter { call -> call.request.path().startsWith("/api") }
    }
    //TODO: Autentisering?
    val virusScanClient = VirusScanClient()
    val pdfGen = PdfGen()
    val datasource = initDatasource(config.database)
    migrate(datasource)

    val repo = Repo(datasource)


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

        route("/fil") {
            post {
                withContext(Dispatchers.IO) {
                    val fil = call.receive<ByteArray>()
                    if (virusScanClient.scan(fil).result == ScanResult.Result.FOUND) {
                        call.respond(HttpStatusCode.NotAcceptable,"Fant virus i fil")
                    }
                    val pdf = pdfGen.bildeTilPfd(fil)
                    val filreferanse = repo.opprettNyFil(pdf)
                    call.respond(HttpStatusCode.Created,filreferanse)
                }
            }

            get("/{filreferanse}") {
                withContext(Dispatchers.IO) {
                    val fil = repo.getEnkeltFil(UUID.fromString(call.parameters["filreferanse"]))
                    call.respond(fil)
                }
            }

            delete("/{filreferanse}") {
                withContext(Dispatchers.IO) {
                    repo.slettEnkeltFil(UUID.fromString(call.parameters["filreferanse"]))
                }
            }
        }

        route("/innsending") {
            post {
                withContext(Dispatchers.IO) {
                    val innsending = call.receive<Innsending>()
                    repo.opprettNyInnsending(innsending)
                    call.respond(HttpStatusCode.Created)
                }
            }
            get("/{innsendingsreferanse}") {
                withContext(Dispatchers.IO) {
                    call.respond(repo.getFilerTilhørendeEnInnsending(UUID.fromString(call.parameters["innsendingsreferanse"])))
                }
            }
            delete("/{innsendingsreferanse}") {
                withContext(Dispatchers.IO) {
                    repo.slettInnsendingOgTilhørendeFiler(UUID.fromString(call.parameters["innsendingsreferanse"]))
                }
            }
        }


    }
}

fun initDatasource(dbConfig: DbConfig) = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 3
    minimumIdle = 1
    initializationFailTimeout = 5000
    idleTimeout = 10001
    connectionTimeout = 1000
    maxLifetime = 30001
    driverClassName = "org.postgresql.Driver"
})

fun migrate(dataSource: DataSource) {
    Flyway
        .configure()
        .cleanDisabled(false) // TODO: husk å skru av denne før prod
        .cleanOnValidationError(true) // TODO: husk å skru av denne før prod
        .dataSource(dataSource)
        .locations("flyway")
        .load()
        .migrate()
}
