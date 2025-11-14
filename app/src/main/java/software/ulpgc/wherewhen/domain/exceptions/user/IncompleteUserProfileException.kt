package software.ulpgc.wherewhen.domain.exceptions.user

class IncompleteUserProfileException(missingFields: List<String>) : UserException("User profile is incomplete. Missing fields: $missingFields");