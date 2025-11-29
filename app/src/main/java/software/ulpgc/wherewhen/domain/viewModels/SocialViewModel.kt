package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.user.User

interface SocialViewModel {
    fun showLoading()
    fun hideLoading()
    fun showUsers(users: List<User>)
    fun showError(message: String)
    fun showEmptyResults()
}
