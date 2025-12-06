package software.ulpgc.wherewhen

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.ports.persistence.AuthenticationRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.ExternalEventRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserEventRepository
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.ports.storage.ImageUploadService
import software.ulpgc.wherewhen.domain.services.TokenService
import software.ulpgc.wherewhen.domain.usecases.user.*
import software.ulpgc.wherewhen.domain.usecases.friendship.*
import software.ulpgc.wherewhen.domain.usecases.chat.*
import software.ulpgc.wherewhen.domain.usecases.events.*
import software.ulpgc.wherewhen.infrastructure.api.TicketmasterExternalEventApiService
import software.ulpgc.wherewhen.infrastructure.location.AndroidLocationService
import software.ulpgc.wherewhen.infrastructure.persistence.cached.CachedChatRepository
import software.ulpgc.wherewhen.infrastructure.persistence.cached.CachedEventRepository
import software.ulpgc.wherewhen.infrastructure.persistence.cached.CachedFriendshipRepository
import software.ulpgc.wherewhen.infrastructure.persistence.cached.CachedFriendRequestRepository
import software.ulpgc.wherewhen.infrastructure.persistence.cached.CachedMessageRepository
import software.ulpgc.wherewhen.infrastructure.persistence.CompositeEventRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseAuthenticationRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseUserRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseFriendRequestRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseFriendshipRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseChatRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseEventRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseMessageRepository
import software.ulpgc.wherewhen.infrastructure.services.MockTokenService
import software.ulpgc.wherewhen.infrastructure.storage.ImgBBImageUploadService

class WhereWhenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        container = DefaultAppContainer(applicationContext)
    }
}

interface AppContainer {
    val authenticateUserUseCase: AuthenticateUserUseCase
    val registerUserUseCase: RegisterUserUseCase
    val getUserUseCase: GetUserUseCase
    val updateUserProfileUseCase: UpdateUserProfileUseCase
    val deleteUserUseCase: DeleteUserUseCase
    val searchUsersUseCase: SearchUsersUseCase
    val sendFriendRequestUseCase: SendFriendRequestUseCase
    val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase
    val acceptFriendRequestUseCase: AcceptFriendRequestUseCase
    val rejectFriendRequestUseCase: RejectFriendRequestUseCase
    val checkFriendshipStatusUseCase: CheckFriendshipStatusUseCase
    val getUserFriendsUseCase: GetUserFriendsUseCase
    val removeFriendUseCase: RemoveFriendUseCase
    val createOrGetChatUseCase: CreateOrGetChatUseCase
    val getUserChatsUseCase: GetUserChatsUseCase
    val getChatMessagesUseCase: GetChatMessagesUseCase
    val sendMessageUseCase: SendMessageUseCase
    val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase
    val searchNearbyEventsUseCase: SearchNearbyEventsUseCase
    val searchEventsByCategoryUseCase: SearchEventsByCategoryUseCase
    val searchEventsByNameUseCase: SearchEventsByNameUseCase
    val getEventByIdUseCase: GetEventByIdUseCase
    val joinEventUseCase: JoinEventUseCase
    val leaveEventUseCase: LeaveEventUseCase
    val getEventAttendeesUseCase: GetEventAttendeesUseCase
    val getUserJoinedEventsUseCase: GetUserJoinedEventsUseCase
    val getUserCreatedEventsUseCase: GetUserCreatedEventsUseCase
    val createUserEventUseCase: CreateUserEventUseCase
    val deleteUserEventUseCase: DeleteUserEventUseCase
    val updateUserEventUseCase: UpdateUserEventUseCase
    val locationService: LocationService
    val imageUploadService: ImageUploadService
    val getSentFriendRequestsUseCase: GetSentFriendRequestsUseCase
    val cancelFriendRequestUseCase: CancelFriendRequestUseCase
    val updateProfileImageUseCase: UpdateProfileImageUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val tokenService: TokenService by lazy {
        MockTokenService()
    }

    private val authRepository: AuthenticationRepository by lazy {
        FirebaseAuthenticationRepository()
    }

    private val userRepository: UserRepository by lazy {
        FirebaseUserRepository()
    }

    private val friendRequestRepository: FriendRequestRepository by lazy {
        CachedFriendRequestRepository(FirebaseFriendRequestRepository())
    }

    private val friendshipRepository: FriendshipRepository by lazy {
        CachedFriendshipRepository(FirebaseFriendshipRepository())
    }

    private val chatRepository: ChatRepository by lazy {
        CachedChatRepository(FirebaseChatRepository())
    }

    private val messageRepository: MessageRepository by lazy {
        CachedMessageRepository(FirebaseMessageRepository())
    }

    private val userEventRepository: UserEventRepository by lazy {
        FirebaseEventRepository()
    }

    private val externalEventRepository: ExternalEventRepository by lazy {
        CachedEventRepository(
            CompositeEventRepository(
                TicketmasterExternalEventApiService(),
                FirebaseEventRepository()
            )
        )
    }

    override val locationService: LocationService by lazy {
        AndroidLocationService(context)
    }

    override val imageUploadService: ImageUploadService by lazy {
        ImgBBImageUploadService(
            context = context,
            apiKey = BuildConfig.IMGBB_API_KEY
        )
    }
    override val getSentFriendRequestsUseCase: GetSentFriendRequestsUseCase by lazy {
        GetSentFriendRequestsUseCase(friendRequestRepository, userRepository)
    }
    override val cancelFriendRequestUseCase: CancelFriendRequestUseCase by lazy {
        CancelFriendRequestUseCase(friendRequestRepository)
    }
    override val updateProfileImageUseCase: UpdateProfileImageUseCase by lazy {
        UpdateProfileImageUseCase(imageUploadService, userRepository)
    }

    override val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        AuthenticateUserUseCase(authRepository, userRepository, tokenService)
    }

    override val registerUserUseCase: RegisterUserUseCase by lazy {
        RegisterUserUseCase(authRepository, userRepository)
    }

    override val getUserUseCase: GetUserUseCase by lazy {
        GetUserUseCase(userRepository)
    }

    override val updateUserProfileUseCase: UpdateUserProfileUseCase by lazy {
        UpdateUserProfileUseCase(userRepository)
    }

    override val deleteUserUseCase: DeleteUserUseCase by lazy {
        DeleteUserUseCase(userRepository)
    }

    override val searchUsersUseCase: SearchUsersUseCase by lazy {
        SearchUsersUseCase(userRepository)
    }

    override val sendFriendRequestUseCase: SendFriendRequestUseCase by lazy {
        SendFriendRequestUseCase(friendRequestRepository, friendshipRepository, userRepository)
    }

    override val getPendingFriendRequestsUseCase: GetPendingFriendRequestsUseCase by lazy {
        GetPendingFriendRequestsUseCase(friendRequestRepository, userRepository)
    }

    override val acceptFriendRequestUseCase: AcceptFriendRequestUseCase by lazy {
        AcceptFriendRequestUseCase(friendRequestRepository, friendshipRepository)
    }

    override val rejectFriendRequestUseCase: RejectFriendRequestUseCase by lazy {
        RejectFriendRequestUseCase(friendRequestRepository)
    }

    override val checkFriendshipStatusUseCase: CheckFriendshipStatusUseCase by lazy {
        CheckFriendshipStatusUseCase(friendshipRepository, friendRequestRepository)
    }

    override val getUserFriendsUseCase: GetUserFriendsUseCase by lazy {
        GetUserFriendsUseCase(friendshipRepository, userRepository)
    }

    override val removeFriendUseCase: RemoveFriendUseCase by lazy {
        RemoveFriendUseCase(friendshipRepository)
    }

    override val createOrGetChatUseCase: CreateOrGetChatUseCase by lazy {
        CreateOrGetChatUseCase(chatRepository, userRepository)
    }

    override val getUserChatsUseCase: GetUserChatsUseCase by lazy {
        GetUserChatsUseCase(chatRepository, userRepository)
    }

    override val getChatMessagesUseCase: GetChatMessagesUseCase by lazy {
        GetChatMessagesUseCase(messageRepository)
    }

    override val sendMessageUseCase: SendMessageUseCase by lazy {
        SendMessageUseCase(chatRepository, messageRepository)
    }

    override val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase by lazy {
        MarkMessagesAsReadUseCase(chatRepository, messageRepository)
    }

    override val searchNearbyEventsUseCase: SearchNearbyEventsUseCase by lazy {
        SearchNearbyEventsUseCase(externalEventRepository)
    }

    override val searchEventsByCategoryUseCase: SearchEventsByCategoryUseCase by lazy {
        SearchEventsByCategoryUseCase(externalEventRepository)
    }

    override val searchEventsByNameUseCase: SearchEventsByNameUseCase by lazy {
        SearchEventsByNameUseCase(externalEventRepository)
    }

    override val getEventByIdUseCase: GetEventByIdUseCase by lazy {
        GetEventByIdUseCase(externalEventRepository)
    }

    override val joinEventUseCase: JoinEventUseCase by lazy {
        JoinEventUseCase(externalEventRepository)
    }

    override val leaveEventUseCase: LeaveEventUseCase by lazy {
        LeaveEventUseCase(externalEventRepository)
    }

    override val getEventAttendeesUseCase: GetEventAttendeesUseCase by lazy {
        GetEventAttendeesUseCase(externalEventRepository)
    }

    override val getUserJoinedEventsUseCase: GetUserJoinedEventsUseCase by lazy {
        GetUserJoinedEventsUseCase(externalEventRepository)
    }

    override val getUserCreatedEventsUseCase: GetUserCreatedEventsUseCase by lazy {
        GetUserCreatedEventsUseCase(externalEventRepository)
    }

    override val createUserEventUseCase: CreateUserEventUseCase by lazy {
        CreateUserEventUseCase(externalEventRepository)
    }

    override val deleteUserEventUseCase: DeleteUserEventUseCase by lazy {
        DeleteUserEventUseCase(externalEventRepository)
    }

    override val updateUserEventUseCase: UpdateUserEventUseCase by lazy {
        UpdateUserEventUseCase(userEventRepository)
    }
}
