package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UserNotFoundException : UserException {
    constructor(uuid: UUID) : super("User not found: $uuid")
    constructor(email: Email) : super("User not found: $email")
}
