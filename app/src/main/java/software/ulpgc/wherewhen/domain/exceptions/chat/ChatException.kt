package software.ulpgc.wherewhen.domain.exceptions.chat

import software.ulpgc.wherewhen.domain.exceptions.DomainException

sealed class ChatException(message: String) : DomainException(message)
