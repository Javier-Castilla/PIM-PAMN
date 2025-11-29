package software.ulpgc.wherewhen.domain.exceptions.friendship

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class FriendRequestNotFoundException(
    requestId: UUID
) : FriendshipException("Friend request not found: $requestId")
