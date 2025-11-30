package software.ulpgc.wherewhen.domain.exceptions.friendship

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class FriendshipNotFoundException(
    userId1: UUID,
    userId2: UUID
) : FriendshipException("Friendship not found between $userId1 and $userId2")
