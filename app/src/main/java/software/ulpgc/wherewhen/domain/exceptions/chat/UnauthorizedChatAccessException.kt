package software.ulpgc.wherewhen.domain.exceptions.chat

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class UnauthorizedChatAccessException(
    userId: UUID,
    chatId: UUID
) : ChatException("User $userId not authorized to access chat $chatId")
