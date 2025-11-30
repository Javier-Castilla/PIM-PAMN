package software.ulpgc.wherewhen.domain.services

interface PasswordService {
    fun verify(plainPassword: String, hashedPassword: String): Boolean
    fun hash(password: String): String
}
