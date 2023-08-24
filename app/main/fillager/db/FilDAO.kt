package fillager.db

import fillager.Fil
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class FilDAO(private val datasource: DataSource) {

    private val insertFilQuery = """
                INSERT INTO fil VALUES (:filreferanse, :innsendingsreferanse, :tittel, :opprettet, :fil)
            """

    private val insertInnsendingQuery = """
                INSERT INTO innsending VALUES (:innsendingsreferanse, :opprettet)
                ON CONFLICT ON CONSTRAINT unique_innsendingsreferanse DO NOTHING
            """

    private val selectFilQuery = """
                SELECT * FROM fil WHERE filreferanse = :filreferanse    
            """
    private val selectInnsendingQuery = """
                SELECT * FROM fil WHERE innsendingsreferanse = :innsendingsreferanse    
            """

    private val deleteFilQuery = """
                DELETE FROM fil WHERE filreferanse = :filreferanse 
            """

    private val deleteInnsendingQuery = """
                DELETE FROM innsending WHERE innsendingsreferanse = :innsendingsreferanse 
            """

    fun insertInnsending(innsendingsreferanse: UUID) {
        sessionOf(datasource).use { session ->
            session.transaction { tSession ->
                tSession.run(
                    queryOf(
                        insertInnsendingQuery, mapOf(
                            "innsendingsreferanse" to innsendingsreferanse,
                            "opprettet" to Timestamp(Date().time),
                        )
                    ).asUpdate
                )
            }
        }
    }

    fun insertFil(filreferanse: UUID, innsendingsreferanse: UUID, tittel: String, fil: ByteArray) {
        insertInnsending(innsendingsreferanse)

        sessionOf(datasource).use { session ->
            session.transaction { tSession ->
                tSession.run(
                    queryOf(
                        insertFilQuery, mapOf(
                            "filreferanse" to filreferanse,
                            "innsendingsreferanse" to innsendingsreferanse,
                            "tittel" to tittel,
                            "opprettet" to Timestamp(Date().time),
                            "fil" to fil
                        )
                    ).asUpdate
                )
            }
        }
    }

    fun deleteFil(filreferanse: UUID) {
        sessionOf(datasource).use { session ->
            session.run(
                queryOf(deleteFilQuery, mapOf("filreferanse" to filreferanse)).asExecute
            )
        }
    }

    fun deleteInnsending(innsendingsreferanse: UUID) {
        sessionOf(datasource).use { session ->
            session.run(
                queryOf(deleteInnsendingQuery, mapOf("innsendingsreferanse" to innsendingsreferanse)).asExecute
            )
        }
    }

    fun selectFil(filreferanse: UUID): Fil? {
        return sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    selectFilQuery, mapOf("filreferanse" to filreferanse)
                ).map { row ->
                    Fil(
                        row.uuid("filreferanse"),
                        row.uuid("innsendingsreferanse"),
                        row.string(3),
                        row.sqlTimestamp(4),
                        row.bytes(5)
                    )
                }.asSingle
            )
        }
    }

    fun selectInnsending(innsendingsreferanse: UUID): List<Fil> {
        return sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    selectInnsendingQuery, mapOf("innsendingsreferanse" to innsendingsreferanse)
                ).map { row ->
                    Fil(
                        row.uuid("filreferanse"),
                        row.uuid("innsendingsreferanse"),
                        row.string(3),
                        row.sqlTimestamp(4),
                        row.bytes(5)
                    )
                }.asList
            )
        }
    }
}