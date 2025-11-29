package software.ulpgc.wherewhen.domain.exceptions.chat

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class ChatNotFoundException(
    chatId: UUID
) : ChatException("Chat not found: $chatId")
