package software.ulpgc.wherewhen

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import software.ulpgc.wherewhen.domain.ports.persistence.AuthenticationRepository
import software.ulpgc.wherewhen.domain.ports.persistence.UserRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendRequestRepository
import software.ulpgc.wherewhen.domain.ports.persistence.FriendshipRepository
import software.ulpgc.wherewhen.domain.ports.persistence.ChatRepository
import software.ulpgc.wherewhen.domain.ports.persistence.EventRepository
import software.ulpgc.wherewhen.domain.ports.persistence.MessageRepository
import software.ulpgc.wherewhen.domain.services.TokenService
import software.ulpgc.wherewhen.domain.usecases.user.*
import software.ulpgc.wherewhen.domain.usecases.friendship.*
import software.ulpgc.wherewhen.domain.usecases.chat.*
import software.ulpgc.wherewhen.domain.usecases.events.GetEventAttendeesUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventByIdUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetUserCreatedEventsUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetUserJoinedEventsUseCase
import software.ulpgc.wherewhen.domain.usecases.events.JoinEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.LeaveEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.SearchEventsByCategoryUseCase
import software.ulpgc.wherewhen.domain.usecases.events.SearchEventsByNameUseCase
import software.ulpgc.wherewhen.domain.usecases.events.SearchNearbyEventsUseCase
import software.ulpgc.wherewhen.infrastructure.api.TicketmasterExternalEventApiService
import software.ulpgc.wherewhen.infrastructure.persistence.CompositeEventRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseAuthenticationRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseUserRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseFriendRequestRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseFriendshipRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseChatRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseEventRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseMessageRepository
import software.ulpgc.wherewhen.infrastructure.services.MockTokenService

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

    val searchNearbyEvents: SearchNearbyEventsUseCase
    val searchEventsByCategory: SearchEventsByCategoryUseCase
    val searchEventsByName: SearchEventsByNameUseCase
    val getEventById: GetEventByIdUseCase
    val joinEvent: JoinEventUseCase
    val leaveEvent: LeaveEventUseCase
    val getEventAttendees: GetEventAttendeesUseCase
    val getUserJoinedEvents: GetUserJoinedEventsUseCase
    val getUserCreatedEvents: GetUserCreatedEventsUseCase
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
        FirebaseFriendRequestRepository()
    }

    private val friendshipRepository: FriendshipRepository by lazy {
        FirebaseFriendshipRepository()
    }

    private val chatRepository: ChatRepository by lazy {
        FirebaseChatRepository()
    }

    private val messageRepository: MessageRepository by lazy {
        FirebaseMessageRepository()
    }

    private val eventRepository: EventRepository by lazy {
        CompositeEventRepository(TicketmasterExternalEventApiService(), FirebaseEventRepository())
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
        CreateOrGetChatUseCase(chatRepository)
    }

    override val getUserChatsUseCase: GetUserChatsUseCase by lazy {
        GetUserChatsUseCase(chatRepository, userRepository)
    }

    override val getChatMessagesUseCase: GetChatMessagesUseCase by lazy {
        GetChatMessagesUseCase(messageRepository)
    }

    override val sendMessageUseCase: SendMessageUseCase by lazy {
        SendMessageUseCase(messageRepository, chatRepository)
    }

    override val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase by lazy {
        MarkMessagesAsReadUseCase(messageRepository, chatRepository)
    }
    override val searchNearbyEvents: SearchNearbyEventsUseCase by lazy {
        SearchNearbyEventsUseCase(eventRepository)
    }
    override val searchEventsByCategory: SearchEventsByCategoryUseCase by lazy {
        SearchEventsByCategoryUseCase(eventRepository)
    }
    override val searchEventsByName: SearchEventsByNameUseCase by lazy {
        SearchEventsByNameUseCase(eventRepository)
    }

    override val getEventById: GetEventByIdUseCase by lazy {
        GetEventByIdUseCase(eventRepository)
    }

    override val joinEvent: JoinEventUseCase by lazy {
        JoinEventUseCase(eventRepository)
    }

    override val leaveEvent: LeaveEventUseCase by lazy {
        LeaveEventUseCase(eventRepository)
    }

    override val getEventAttendees: GetEventAttendeesUseCase by lazy {
        GetEventAttendeesUseCase(eventRepository)
    }

    override val getUserJoinedEvents: GetUserJoinedEventsUseCase by lazy {
        GetUserJoinedEventsUseCase(eventRepository)
    }

    override val getUserCreatedEvents: GetUserCreatedEventsUseCase by lazy {
        GetUserCreatedEventsUseCase(eventRepository)
    }
}
