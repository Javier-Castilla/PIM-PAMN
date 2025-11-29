package software.ulpgc.wherewhen.infrastructure.services

import software.ulpgc.wherewhen.domain.services.PasswordService

class MockPasswordService : PasswordService {
    
    override fun verify(plainPassword: String, hashedPassword: String): Boolean {
        // Para testing, el hash es simplemente el password + "_hashed"
        return hashedPassword == hash(plainPassword)
    }
    
    override fun hash(password: String): String {
        // Simulación simple: agrega "_hashed" al final
        return "${password}_hashed"
    }
}
