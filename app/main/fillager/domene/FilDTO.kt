package fillager.domene

import java.time.LocalDateTime
import java.util.*

data class FilDTO(val filreferanse: UUID, val tittel: String, val fil: String, val opprettet: LocalDateTime)