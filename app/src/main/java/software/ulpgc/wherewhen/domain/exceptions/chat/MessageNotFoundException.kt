package software.ulpgc.wherewhen.domain.exceptions.chat

import software.ulpgc.wherewhen.domain.valueObjects.UUID

class MessageNotFoundException(
    messageId: UUID
) : ChatException("Message not found: $messageId")
