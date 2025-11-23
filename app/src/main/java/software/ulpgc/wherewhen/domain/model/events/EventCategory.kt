package software.ulpgc.wherewhen.domain.model.events

enum class EventCategory {
    MUSIC,
    SPORTS,
    ARTS,
    THEATER,
    FAMILY,
    FILM,
    MISCELLANEOUS,
    OTHER;

    companion object {
        fun fromTicketmaster(segment: String?): EventCategory {
            return when (segment?.lowercase()) {
                "music" -> MUSIC
                "sports" -> SPORTS
                "arts" -> ARTS
                "theater" -> THEATER
                "family" -> FAMILY
                "film" -> FILM
                "miscellaneous" -> MISCELLANEOUS
                else -> OTHER
            }
        }
    }
}
