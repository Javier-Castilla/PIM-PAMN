package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.user.Profile

interface ProfileViewModel {
    fun showLoading()
    fun hideLoading()
    fun showUserProfile(profile: Profile)
    fun showUpdateSuccess()
    fun showError(message: String)
}
