package fillager.db

import fillager.Fil
import fillager.Innsending
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

class FilDAO(private val datasource: DataSource) {

    private val insertFilQuery = """
                INSERT INTO fil VALUES (:filreferanse, :opprettet, :fil)
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
    private val deleteInnsendingFilQuery = """
                DELETE FROM innsending_fil WHERE innsendingsreferanse = :innsendingsreferanse
            """
    private val insertInnsendingFil = """
                INSERT INTO innsending_fil VALUES (:innsendingsreferanse, :filreferanse)
            """
    private val updateFilTittel = """
                UPDATE fil WHERE filreferanse = :filreferanse SET tittel = :tittel
            """

    fun insertInnsending(innsending: Innsending) {
        sessionOf(datasource).use { session ->
            session.transaction { tSession ->
                tSession.run(
                    queryOf(
                        insertInnsendingQuery, mapOf(
                            "innsendingsreferanse" to innsending.innsendingsreferanse,
                            "opprettet" to Timestamp(Date().time),
                        )
                    ).asUpdate
                )
                innsending.filer.forEach { fil ->
                    tSession.run {
                        queryOf(
                            insertInnsendingFil, mapOf(
                                "innsendingsreferanse" to innsending.innsendingsreferanse,
                                "filreferanse" to fil.filreferanse
                            )
                        ).asUpdate
                    }
                    tSession.run {
                        queryOf(
                            updateFilTittel, mapOf(
                                "filreferanse" to fil.filreferanse,
                                "tittel" to fil.tittel
                            )
                        ).asUpdate
                    }
                }
            }
        }
    }

    fun insertFil(filreferanse: UUID, fil: ByteArray) {
        sessionOf(datasource).use { session ->
            session.transaction { tSession ->
                tSession.run(
                    queryOf(
                        insertFilQuery, mapOf(
                            "filreferanse" to filreferanse,
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
            session.transaction { tx ->
                tx.run {
                    queryOf(
                        deleteInnsendingFilQuery,
                        mapOf("innsendingsreferanse" to innsendingsreferanse)
                    )
                }.asExecute
                tx.run {
                    queryOf(
                        deleteInnsendingQuery,
                        mapOf("innsendingsreferanse" to innsendingsreferanse)
                    ).asExecute
                }
            }
        }
    }

    fun selectFil(filreferanse: UUID): ByteArray? {
        return sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    selectFilQuery, mapOf("filreferanse" to filreferanse)
                ).map { row ->
                        row.bytes(5)
                }.asSingle
            )
        }
    }

    /*fun selectInnsending(innsendingsreferanse: UUID): List<Fil> {
        return sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    selectInnsendingQuery, mapOf("innsendingsreferanse" to innsendingsreferanse)
                ).map { row ->
                    Fil(row.bytes(5)
                    )
                }.asList
            )
        }
    }*///TODO: hvordan returnerer vi mange filer uten å streame
}