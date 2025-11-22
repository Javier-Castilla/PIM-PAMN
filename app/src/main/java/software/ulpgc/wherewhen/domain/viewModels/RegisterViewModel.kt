package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.Profile

interface RegisterViewModel {
    fun showLoading()
    fun hideLoading()
    fun showSuccess(profile: Profile)
    fun showError(message: String)
    fun showNameError(message: String)
    fun showEmailError(message: String)
    fun showPasswordError(message: String)
    fun showConfirmPasswordError(message: String)
}
