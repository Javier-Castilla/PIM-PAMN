package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UserInactiveException(uuid: UUID) : UserException("User account is inactive: $uuid");