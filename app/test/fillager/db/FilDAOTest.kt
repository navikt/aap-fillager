package fillager.db

import fillager.domene.Fil
import fillager.domene.Innsending
import fillager.db.InitTestDatabase.dataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

internal class FilDAOTest : DatabaseTestBase() {
    private val filDAO = FilDAO(dataSource)

    @Test
    fun `Lagre en fil og gjør et oppslag`() {
        val filid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()

        filDAO.insertFil(filid, fysiskFil)

        val fil = filDAO.selectFil(filid)
        assertEquals(String(fysiskFil), String(fil ?: throw RuntimeException()))
    }

    @Test
    fun `Lagre fil deretter slett fil`() {
        val filid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()

        filDAO.insertFil(filid, fysiskFil)
        filDAO.deleteFil(filid)

        val filer = filDAO.selectFil(filid)
        assertNull(filer)
    }

    @Test
    fun `insert innsending`() {
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()
        val filid = UUID.randomUUID()
        val filid2 = UUID.randomUUID()

        filDAO.insertFil(filid, fysiskFil)
        filDAO.insertFil(filid2, fysiskFil)

        val innsending = Innsending(
            innsendingid, listOf(
                Fil(filid, "tittel1"),
                Fil(filid2, "tittel2")
            )
        )
        filDAO.insertInnsending(innsending)

        assertEquals(1, queryCountInnsending())
        assertEquals(2,queryCountInnsendingFiler(innsendingid))
        assertEquals("tittel1", queryTittel(filid))
        assertEquals("tittel2", queryTittel(filid2))
    }

    @Test
    fun `test oppdatering av innsending`(){
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()
        val filid = UUID.randomUUID()
        val filid2 = UUID.randomUUID()

        filDAO.insertFil(filid, fysiskFil)

        val innsending = Innsending(
            innsendingid,
            listOf(
                Fil(filid, "tittel1"),
            )
        )
        filDAO.insertInnsending(innsending)

        assertEquals(1, queryCountInnsending())
        assertEquals(1,queryCountInnsendingFiler(innsendingid))


        filDAO.insertFil(filid2, fysiskFil)

        val innsendingoppdatering = Innsending(
            innsendingid,
            listOf(
                Fil(filid2, "tittel1"),
            )
        )

        filDAO.insertInnsendingFil(innsendingoppdatering)

        assertEquals(1, queryCountInnsending())
        assertEquals(2,queryCountInnsendingFiler(innsendingid))
    }

    @Test
    fun `test at det ikke blir duplikater i innsending_fil`(){
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()
        val filid = UUID.randomUUID()
        val filid2 = UUID.randomUUID()

        filDAO.insertFil(filid, fysiskFil)

        val innsending = Innsending(
            innsendingid,
            listOf(
                Fil(filid, "tittel1"),
            )
        )
        filDAO.insertInnsending(innsending)
        filDAO.insertFil(filid2, fysiskFil)

        val innsendingoppdatering = Innsending(
            innsendingid,
            listOf(
                Fil(filid, "tittel1"),
                Fil(filid2, "tittel1"),
            )
        )

        filDAO.insertInnsendingFil(innsendingoppdatering)

        assertEquals(1, queryCountInnsending())
        assertEquals(2,queryCountInnsendingFiler(innsendingid))
    }

    @Test
    fun `slett en hel innsending`() {
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()
        val filid = UUID.randomUUID()
        val filid2 = UUID.randomUUID()

        filDAO.insertFil(filid, fysiskFil)
        filDAO.insertFil(filid2, fysiskFil)

        val innsending = Innsending(
            innsendingid, listOf(
                Fil(filid, "tittel1"),
                Fil(filid2, "tittel2")
            )
        )

        filDAO.insertInnsending(innsending)
        filDAO.deleteInnsending(innsendingid)
        assertEquals(0, queryCountInnsending())
        assertEquals(0,queryCountFiler())
        assertEquals(0,queryCountInnsendingFiler(innsendingid))
    }

    private fun queryTittel(filreferanse: UUID): String? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT tittel FROM fil WHERE filreferanse = ?").use { preparedStatement ->
                preparedStatement.setObject(1, filreferanse)

                val resultSet = preparedStatement.executeQuery()

                resultSet.map { row ->
                    row.getString("tittel")
                }.singleOrNull()
            }
        }
    }

    private fun queryCountInnsendingFiler(innsendingsreferanse: UUID): Int? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM innsending_fil WHERE innsendingsreferanse = ?")
                .use { preparedStatement ->
                    preparedStatement.setObject(1, innsendingsreferanse)

                    val resultSet = preparedStatement.executeQuery()

                    resultSet.map { row ->
                        row.getInt(1)
                    }.singleOrNull()
                }
        }
    }

    private fun queryCountFiler(): Int? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM fil")
                .use { preparedStatement ->

                    val resultSet = preparedStatement.executeQuery()

                    resultSet.map { row ->
                        row.getInt(1)
                    }.singleOrNull()
                }
        }
    }
    private fun queryCountInnsending(): Int? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM innsending")
                .use { preparedStatement ->

                    val resultSet = preparedStatement.executeQuery()

                    resultSet.map { row ->
                        row.getInt(1)
                    }.singleOrNull()
                }
        }
    }
}
