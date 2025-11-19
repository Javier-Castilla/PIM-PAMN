package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UserNotFoundException(uuid: UUID) : UserException("User not found $uuid");
