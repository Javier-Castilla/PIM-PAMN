package software.ulpgc.wherewhen.domain.exceptions.friendship

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class AlreadyFriendsException(
    userId1: UUID,
    userId2: UUID
) : FriendshipException("Users $userId1 and $userId2 are already friends")
