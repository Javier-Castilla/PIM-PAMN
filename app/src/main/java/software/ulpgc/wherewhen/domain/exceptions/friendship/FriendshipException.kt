package software.ulpgc.wherewhen.domain.exceptions.friendship

import software.ulpgc.wherewhen.domain.exceptions.DomainException

sealed class FriendshipException(message: String) : DomainException(message)
