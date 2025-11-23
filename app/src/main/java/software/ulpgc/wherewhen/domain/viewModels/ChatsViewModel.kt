package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.chat.ChatWithUser

interface ChatsViewModel {
    fun showLoading()
    fun hideLoading()
    fun showChats(chats: List<ChatWithUser>)
    fun showError(message: String)
}
