package fillager

import java.sql.Timestamp
import java.util.*

data class Fil(val filreferanse: UUID, val tittel:String, val opprettet: Timestamp, val fil_base64:String)