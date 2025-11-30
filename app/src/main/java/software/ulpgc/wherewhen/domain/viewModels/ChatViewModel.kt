package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.model.user.User

interface ChatViewModel {
    fun initChat(otherUser: User)
    fun sendMessage()
    fun isOwnMessage(message: Message): Boolean
    fun onMessageTextChange(text: String)
}
