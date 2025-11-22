package software.ulpgc.wherewhen.domain.exceptions.chat

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class InvalidChatParticipantException(
    userId: UUID
) : ChatException("User $userId is not a valid participant for this chat")
