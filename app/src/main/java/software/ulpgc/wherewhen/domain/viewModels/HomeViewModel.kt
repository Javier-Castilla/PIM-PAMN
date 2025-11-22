package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.Profile

interface HomeViewModel {
    fun showLoading()
    fun hideLoading()
    fun showUserData(profile: Profile)
    fun showError(message: String)
}
