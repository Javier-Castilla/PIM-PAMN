package software.ulpgc.wherewhen.domain.exceptions.friendship

class UnauthorizedFriendshipActionException(
    action: String
) : FriendshipException("Not authorized to perform friendship action: $action")
