package fillager.db

import fillager.db.InitTestDatabase.dataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
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
        assertEquals(String(fysiskFil), String(fil?.fil?:throw RuntimeException()))
    }

    @Test
    fun `Lagre fil og hent ut med innsendingsID`(){
        val filid = UUID.randomUUID()
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()

        filDAO.insertFil(filid, innsendingid, "Tittel", fysiskFil)

        val filer = filDAO.selectInnsending(innsendingid)
        assertEquals(1,filer.size)
        assertEquals(filid, filer.first().filreferanse)
    }

    @Test
    fun `Lagre fil deretter slett fil`(){
        val filid = UUID.randomUUID()
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()

        filDAO.insertFil(filid, innsendingid, "Tittel", fysiskFil)
        filDAO.deleteFil(filid)

        val filer = filDAO.selectInnsending(innsendingid)
        assertEquals(0,filer.size)
    }

    @Test
    fun `lagre to filer på samme innsending`(){
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()
        val filid = UUID.randomUUID()
        val filid2 = UUID.randomUUID()

        filDAO.insertFil(filid, innsendingid, "Tittel", fysiskFil)
        filDAO.insertFil(filid2, innsendingid, "Tittel", fysiskFil)

        val filer = filDAO.selectInnsending(innsendingid)
        assertEquals(2,filer.size)
        assertEquals(filid, filer.first().filreferanse)
    }

    @Test
    fun `slett en hel innsending`(){
        val innsendingid = UUID.randomUUID()
        val fysiskFil = "FILINNHOLD".toByteArray()
        val filid = UUID.randomUUID()
        val filid2 = UUID.randomUUID()

        filDAO.insertFil(filid, innsendingid, "Tittel", fysiskFil)
        filDAO.insertFil(filid2, innsendingid, "Tittel", fysiskFil)

        filDAO.deleteInnsending(innsendingid)

        val filer = filDAO.selectInnsending(innsendingid)
        assertEquals(0,filer.size)
    }

}