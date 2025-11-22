package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.Message
import software.ulpgc.wherewhen.domain.model.User

interface ChatViewModel {
    fun showLoading()
    fun hideLoading()
    fun showMessages(messages: List<Message>)
    fun showError(message: String)
    fun setOtherUser(user: User)
}
