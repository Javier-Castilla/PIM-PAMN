package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.chat.Message
import software.ulpgc.wherewhen.domain.model.user.User

interface ChatViewModel {
    fun showLoading()
    fun hideLoading()
    fun showMessages(messages: List<Message>)
    fun showError(message: String)
    fun setOtherUser(user: User)
}
