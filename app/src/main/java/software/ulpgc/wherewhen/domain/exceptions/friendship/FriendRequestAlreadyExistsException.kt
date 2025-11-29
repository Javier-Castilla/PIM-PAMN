package software.ulpgc.wherewhen.domain.exceptions.friendship

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class FriendRequestAlreadyExistsException(
    fromUserId: UUID,
    toUserId: UUID
) : FriendshipException("Friend request already exists from $fromUserId to $toUserId")
