package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.user.Profile

interface LoginViewModel {
    fun showLoading()
    fun hideLoading()
    fun showSuccess(profile: Profile)
    fun showError(message: String)
    fun showEmailError(message: String)
    fun showPasswordError(message: String)
}
