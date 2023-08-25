package fillager.db

import fillager.Innsending
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class FilDAO(private val datasource: DataSource) {

    private val insertFilQuery = """
                INSERT INTO fil (filreferanse, opprettet, fil) VALUES (?, ?, ?)
            """

    private val insertInnsendingQuery = """
                INSERT INTO innsending VALUES (?, ?)
                ON CONFLICT ON CONSTRAINT unique_innsendingsreferanse DO NOTHING
            """

    private val selectFilQuery = """
                SELECT fil FROM fil WHERE filreferanse = ?    
            """

    private val selectInnsendingQuery = """
                SELECT * FROM fil WHERE innsendingsreferanse = ?    
            """

    private val deleteFilQuery = """
                DELETE FROM fil WHERE filreferanse = ? 
            """

    private val deleteInnsendingQuery = """
                DELETE FROM innsending WHERE innsendingsreferanse = ? 
            """
    private val deleteInnsendingFilQuery = """
                DELETE FROM innsending_fil WHERE innsendingsreferanse = :innsendingsreferanse
            """
    private val insertInnsendingFil = """
                INSERT INTO innsending_fil VALUES (?, ?)
            """
    private val updateFilTittel = """
                UPDATE fil SET tittel = ? WHERE filreferanse = ? 
            """

    fun insertInnsending(innsending: Innsending) {
        datasource.connection.transaction { connection ->
            connection.prepareStatement(insertInnsendingQuery).use { preparedStatement ->
                preparedStatement.setObject(1, innsending.innsendingsreferanse)
                preparedStatement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))

                preparedStatement.execute()
            }

            innsending.filer.forEach { fil ->
                connection.prepareStatement(insertInnsendingFil).use { preparedStatement ->
                    preparedStatement.setObject(1, fil.filreferanse)
                    preparedStatement.setObject(2, innsending.innsendingsreferanse)

                    preparedStatement.execute()
                }

                connection.prepareStatement(updateFilTittel).use { preparedStatement ->
                    preparedStatement.setString(1, fil.tittel)
                    preparedStatement.setObject(2, fil.filreferanse)

                    preparedStatement.execute()
                }
            }
        }
    }

    fun insertFil(filreferanse: UUID, fil: ByteArray) {
        datasource.connection.use { connection ->
            connection.prepareStatement(insertFilQuery).use { preparedStatement ->
                preparedStatement.setObject(1, filreferanse)
                preparedStatement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                preparedStatement.setBytes(3, fil)

                preparedStatement.execute()
            }
        }
    }

    fun deleteFil(filreferanse: UUID) {
        datasource.connection.use { connection ->
            connection.prepareStatement(deleteFilQuery).use { preparedStatement ->
                preparedStatement.setObject(1, filreferanse)

                preparedStatement.execute()
            }
        }
    }

    fun deleteInnsending(innsendingsreferanse: UUID) {
        datasource.connection.use { connection ->
            connection.prepareStatement(deleteInnsendingQuery).use { preparedStatement ->
                preparedStatement.setObject(1, innsendingsreferanse)

                preparedStatement.execute()
            }
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

    fun selectFil(filreferanse: UUID): ByteArray? {
        return datasource.connection.use { connection ->
            connection.prepareStatement(selectFilQuery).use { preparedStatement ->
                preparedStatement.setObject(1, filreferanse)

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    row.getBytes("fil")
                }.singleOrNull()
            }
        }
    }

//    fun selectInnsending(innsendingsreferanse: UUID): List<Fil> {
//        val prepareStatement = datasource.connection.prepareStatement(selectInnsendingQuery)
//
//        prepareStatement.setObject(1, innsendingsreferanse)
//
//        val resultSet = prepareStatement.executeQuery()
//
//        return resultSet.map { row ->
//            Fil(
//                row.getObject(1, UUID::class.java),
//                row.getObject(2, UUID::class.java),
//                row.getString(3),
//                row.getTimestamp(4),
//                row.getBytes(5)
//            )
//        }
//    }
}
