package software.ulpgc.wherewhen.infrastructure.persistence

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.exceptions.user.InvalidCredentialsException
import software.ulpgc.wherewhen.domain.exceptions.user.UserAlreadyExistsException
import software.ulpgc.wherewhen.domain.exceptions.user.UserNotFoundException
import software.ulpgc.wherewhen.domain.ports.persistence.AuthenticationRepository
import software.ulpgc.wherewhen.domain.valueObjects.Email
import software.ulpgc.wherewhen.domain.valueObjects.UUID

class FirebaseAuthenticationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthenticationRepository {

    override suspend fun register(email: Email, password: String): Result<UUID> =
        runCatching {
            val authResult = auth
                .createUserWithEmailAndPassword(email.value, password)
                .await()

            authResult.user?.uid?.let { UUID.parse(it).getOrThrow() }
                ?: throw IllegalStateException("User ID is null after registration")
        }.recoverCatching { exception ->
            throw when (exception) {
                is FirebaseAuthUserCollisionException -> UserAlreadyExistsException(email)
                else -> exception
            }
        }

    override suspend fun login(email: Email, password: String): Result<UUID> =
        runCatching {
            val authResult = auth
                .signInWithEmailAndPassword(email.value, password)
                .await()

            authResult.user?.uid?.let { UUID.parse(it).getOrThrow() }
                ?: throw IllegalStateException("User ID is null after login")
        }.recoverCatching { exception ->
            throw when (exception) {
                is FirebaseAuthInvalidUserException -> UserNotFoundException(email)
                is FirebaseAuthInvalidCredentialsException -> InvalidCredentialsException()
                else -> exception
            }
        }

    override suspend fun exists(email: Email): Boolean =
        runCatching {
            val signInMethods = auth
                .fetchSignInMethodsForEmail(email.value)
                .await()
            !signInMethods.signInMethods.isNullOrEmpty()
        }.getOrDefault(false)
}
