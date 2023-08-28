package fillager.db

import fillager.domene.Fil
import fillager.domene.Innsending
import java.util.*
import javax.sql.DataSource
import kotlin.collections.List

class Repo(datasource: DataSource) {
    private val filDAO = FilDAO(datasource)

    fun slettInnsendingOgTilhørendeFiler(innsendingsreferanse: UUID) {
        filDAO.deleteInnsending(innsendingsreferanse)
    }

    fun slettEnkeltFil(filreferanse: UUID){
        filDAO.deleteFil(filreferanse)
    }

    fun getEnkeltFil(filreferanse: UUID): ByteArray {
        return requireNotNull(filDAO.selectFil(filreferanse)){"Fil ikke funnet"}
    }

    fun getFilerTilhørendeEnInnsending(innsendingsreferanse: UUID):List<Fil>{
        return emptyList() //FIXME filDAO.selectInnsending(innsendingsreferanse)
    }

    fun opprettNyFil(fil: ByteArray):UUID{
        val id = UUID.randomUUID()
        filDAO.insertFil(id, fil)
        return id
    }

    fun opprettNyInnsending(innsending: Innsending){
        filDAO.insertInnsending(innsending)
    }

}

