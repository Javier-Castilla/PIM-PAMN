package software.ulpgc.wherewhen

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import software.ulpgc.wherewhen.domain.model.User
import software.ulpgc.wherewhen.domain.persistence.repositories.AuthenticationRepository
import software.ulpgc.wherewhen.domain.persistence.repositories.UserRepository
import software.ulpgc.wherewhen.domain.services.PasswordService
import software.ulpgc.wherewhen.domain.services.TokenService
import software.ulpgc.wherewhen.domain.usecases.user.*
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseAuthenticationRepository
import software.ulpgc.wherewhen.infrastructure.persistence.FirebaseUserRepository
import software.ulpgc.wherewhen.infrastructure.persistence.InMemoryAuthenticationRepository
import software.ulpgc.wherewhen.infrastructure.persistence.InMemoryUserRepository
import software.ulpgc.wherewhen.infrastructure.services.MockPasswordService
import software.ulpgc.wherewhen.infrastructure.services.MockTokenService
import java.time.LocalDateTime

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
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val passwordService: PasswordService by lazy {
        MockPasswordService()
    }

    private val tokenService: TokenService by lazy {
        MockTokenService()
    }

    private val authRepository: AuthenticationRepository by lazy {
        FirebaseAuthenticationRepository()
    }

    private val userRepository: UserRepository by lazy {
        FirebaseUserRepository()
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
}
