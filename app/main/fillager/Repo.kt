package fillager

import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotlin.collections.List

class Repo(datasource: DataSource) {
    private val filDAO = FilDAO(datasource)

    fun slettInnsendingOgTilhørendeFiler(innsendingsreferanse: UUID) {
        filDAO.deleteInnsending(innsendingsreferanse)
    }

    fun slettEnkeltFil(filreferanse: UUID){
        filDAO.deleteFil(filreferanse)
    }

    fun getEnkeltFil(filreferanse: UUID):Fil{
        return requireNotNull(filDAO.selectFil(filreferanse)){"Fil ikke funnet"}
    }

    fun getFilerTilhørendeEnInnsending(innsendingsreferanse: UUID):List<Fil>{
        return filDAO.selectInnsending(innsendingsreferanse)
    }

    fun opprettNyFil(filreferanse: UUID, innsendingsreferanse: UUID, tittel: String,fil: ByteArray){
        filDAO.insertFil(filreferanse,innsendingsreferanse,tittel,fil)
    }

    fun opprettNyInnsending(innsendingsreferanse: UUID){
        filDAO.insertInnsending(innsendingsreferanse)
    }

}

class FilDAO(private val datasource: DataSource) {

    private val insertFilQuery = """
                INSERT INTO fil VALUES (:filreferanse, :innsendingsreferanse, :tittel, :opprettet, :fil)
            """

    private val insertInnsendingQuery = """
                INSERT INTO innsending VALUES (:innsendingsreferanse, :opprettet)
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
