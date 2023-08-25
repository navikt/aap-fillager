package fillager.db

import fillager.Fil
import fillager.Innsending
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
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()

        filDAO.insertFil(filid, fysiskFil)

        val fil = filDAO.selectFil(filid)
        assertEquals(String(fysiskFil), String(fil ?: throw RuntimeException()))
    }

    @Test
    fun `Lagre fil deretter slett fil`() {
        val filid = UUID.randomUUID()
        val innsendingid = UUID.randomUUID()
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

        assertEquals("tittel1", queryTittel(filid))
        assertEquals("tittel2", queryTittel(filid2))
    }

    /*@Test
    fun `slett en hel innsending`() {
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()
        val filid = UUID.randomUUID()
        val filid2 = UUID.randomUUID()

        filDAO.insertFil(filid, innsendingid, "Tittel", fysiskFil)
        filDAO.insertFil(filid2, innsendingid, "Tittel", fysiskFil)

        filDAO.deleteInnsending(innsendingid)

        val filer = filDAO.selectInnsending(innsendingid)
        assertEquals(0, filer.size)
    }*/

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

    private fun queryCountFiler(innsendingsreferanse: UUID): Int? {
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
}
