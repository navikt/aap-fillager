package fillager

import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf

class Repo(datasource: DataSource) {

}

class FilDAO(private val datasource: DataSource) {

    private val insertQuery = """
        INSERT INTO fil VALUES (:filreferanse,:tittel,:opprettet,:fil_base64)
    """

    val selectQuery = """
                SELECT * FROM fil WHERE filreferanse = :filreferanse    
            """

    val deleteQuery = """
                DELETE FROM fil WHERE filreferanse = :filreferanse 
            """


    fun insert(filreferanse: UUID, tittel: String, fil: String) {
        sessionOf(datasource).use { session ->
            session.transaction { tSession ->
                tSession.run(
                    queryOf(
                        insertQuery, mapOf(
                            "filreferanse" to filreferanse,
                            "tittel" to tittel,
                            "opprettet" to Timestamp(Date().time),
                            "fil_base64" to fil
                        )
                    ).asUpdate
                )
            }
        }
    }

    fun delete(filreferanse: UUID) {
        sessionOf(datasource).use { session ->
            session.run(
                queryOf(deleteQuery, mapOf("filreferanse" to filreferanse)).asExecute
            )
        }
    }

    fun select(filreferanse: UUID) {
        sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    selectQuery, mapOf("filreferanse" to filreferanse)
                ).map { row ->
                    val fil = Fil(
                        row.uuid("filreferanse"),
                        row.string(2),
                        row.sqlTimestamp(3),
                        row.string(4)
                    )
                }.asSingle
            )
        }
    }
}
