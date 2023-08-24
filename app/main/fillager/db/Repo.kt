package fillager.db

import fillager.Fil
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

    fun getEnkeltFil(filreferanse: UUID): Fil {
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

