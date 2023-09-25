package fillager.db

import fillager.domene.Innsending
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
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
                DELETE FROM fil WHERE filreferanse IN (SELECT filreferanse FROM innsending_fil WHERE innsendingsreferanse = ?)
            """

    private val insertInnsendingFil = """
                INSERT INTO innsending_fil VALUES (?, ?) 
                ON CONFLICT ON CONSTRAINT unique_innsending_fil DO NOTHING
            """

    private val updateFilTittel = """
                UPDATE fil SET tittel = ? WHERE filreferanse = ? 
            """

    fun insertInnsending(innsending: Innsending) {
        datasource.transaction {
            prepareExecuteStatement(insertInnsendingQuery) {
                setParams {
                    setUUID(1, innsending.innsendingsreferanse)
                    setTimestamp(2, LocalDateTime.now())
                }
            }

            innsending.filer.forEach { fil ->
                prepareExecuteStatement(insertInnsendingFil) {
                    setParams {
                        setUUID(1, fil.filreferanse)
                        setUUID(2, innsending.innsendingsreferanse)
                    }
                }

                prepareExecuteStatement(updateFilTittel) {
                    setParams {
                        setString(1, fil.tittel)
                        setUUID(2, fil.filreferanse)
                    }
                }
            }
        }
    }

    fun insertInnsendingFil(innsending: Innsending){
        datasource.transaction {
            innsending.filer.forEach { fil ->
                prepareExecuteStatement(insertInnsendingFil) {
                    setParams {
                        setUUID(1, fil.filreferanse)
                        setUUID(2, innsending.innsendingsreferanse)
                    }
                }
            }
        }
    }

    fun insertFil(filreferanse: UUID, fil: ByteArray) {
        datasource.connect {
            prepareExecuteStatement(insertFilQuery) {
                setParams {
                    setUUID(1, filreferanse)
                    setTimestamp(2, LocalDateTime.now())
                    setBytes(3, fil)
                }
            }
        }
    }

    fun deleteFil(filreferanse: UUID) {
        datasource.connect {
            prepareExecuteStatement(deleteFilQuery) {
                setParams {
                    setUUID(1, filreferanse)
                }
            }
        }
    }

    fun deleteInnsending(innsendingsreferanse: UUID) {
        datasource.transaction {
            prepareExecuteStatement(deleteInnsendingFilQuery) {
                setParams {
                    setUUID(1, innsendingsreferanse)
                }
            }

            prepareExecuteStatement(deleteInnsendingQuery) {
                setParams {
                    setUUID(1, innsendingsreferanse)
                }
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
        return datasource.connect {
            prepareQueryStatement(selectFilQuery) {
                setParams {
                    setUUID(1, filreferanse)
                }
                setRowMapper {
                    getBytes("fil")
                }
                setResultMapper { result ->
                    result.singleOrNull()
                }
            }
        }
    }

    private fun <T> DataSource.connect(block: DbConnection.() -> T): T {
        return this.connection.use { connection ->
            DbConnection(connection).block()
        }
    }

    private fun <T> DataSource.transaction(block: DbConnection.() -> T): T {
        return this.connection.use { connection ->
            try {
                connection.autoCommit = false
                val result = DbConnection(connection).block()
                connection.commit()
                result
            } catch (e: Throwable) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private class DbConnection(private val connection: Connection) {
        fun <T : Any, R> prepareQueryStatement(
            query: String,
            block: PreparedQueryStatement<T, R>.() -> Unit
        ): R {
            return this.connection.prepareStatement(query).use { preparedStatement ->
                val preparedQueryStatement = PreparedQueryStatement<T, R>(preparedStatement)
                preparedQueryStatement.block()
                preparedQueryStatement.executeQuery()
            }
        }

        fun prepareExecuteStatement(
            query: String,
            block: PreparedExecuteStatement.() -> Unit
        ) {
            return this.connection.prepareStatement(query).use { preparedStatement ->
                val myPreparedStatement = PreparedExecuteStatement(preparedStatement)
                myPreparedStatement.block()
                myPreparedStatement.execute()
            }
        }
    }

    private class PreparedQueryStatement<T : Any, R>(private val preparedStatement: PreparedStatement) {
        private lateinit var rowMapper: Row.() -> T
        private lateinit var resultMapper: (Sequence<T>) -> R

        fun setParams(block: Params.() -> Unit) {
            Params(preparedStatement).block()
        }

        fun setRowMapper(block: Row.() -> T) {
            rowMapper = block
        }

        fun setResultMapper(block: (result: Sequence<T>) -> R) {
            resultMapper = block
        }

        fun executeQuery(): R {
            val resultSet = preparedStatement.executeQuery()
            return resultSet
                .map { currentResultSet ->
                    Row(currentResultSet).rowMapper()
                }
                .let(resultMapper)

        }
    }

    private class PreparedExecuteStatement(private val preparedStatement: PreparedStatement) {
        fun setParams(block: Params.() -> Unit) {
            Params(preparedStatement).block()
        }

        fun execute() {
            preparedStatement.execute()
        }
    }

    private class Params(private val preparedStatement: PreparedStatement) {
        fun setBytes(index: Int, bytes: ByteArray) {
            preparedStatement.setBytes(index, bytes)
        }

        fun setString(index: Int, value: String) {
            preparedStatement.setString(index, value)
        }

        fun setTimestamp(index: Int, localDateTime: LocalDateTime) {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(localDateTime))
        }

        fun setUUID(index: Int, uuid: UUID) {
            preparedStatement.setObject(index, uuid)
        }
    }

    private class Row(private val resultSet: ResultSet) {
        fun getBytes(columnLabel: String): ByteArray {
            return resultSet.getBytes(columnLabel)
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
