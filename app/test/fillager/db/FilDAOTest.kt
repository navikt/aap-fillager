package fillager.db

import fillager.db.InitTestDatabase.dataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class FilDAOTest : DatabaseTestBase() {
    private val filDAO = FilDAO(dataSource)

    @Test
    fun `Lagre en fil og gjør et oppslag`() {
        val filid = UUID.randomUUID()
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()

        filDAO.insertFil(filid, innsendingid, "Tittel", fysiskFil)

        val fil = filDAO.selectFil(filid)
        assertEquals(innsendingid, fil?.innsendingsreferanse)
    }

}