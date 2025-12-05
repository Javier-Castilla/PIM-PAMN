package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.user.Profile
import software.ulpgc.wherewhen.domain.usecases.friendship.FriendshipStatus

interface UserProfileViewModel {
    fun showLoading()
    fun hideLoading()
    fun showUserProfile(profile: Profile, status: FriendshipStatus)
    fun showError(message: String)
    fun updateFriendshipStatus(status: FriendshipStatus)
}
