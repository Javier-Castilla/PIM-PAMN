package software.ulpgc.wherewhen.domain.ports.persistence

import software.ulpgc.wherewhen.domain.model.friendship.Friendship
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface FriendshipRepository {
    suspend fun create(friendship: Friendship): Result<Friendship>
    suspend fun getById(id: UUID): Result<Friendship>
    suspend fun getFriendshipsForUser(userId: UUID): Result<List<Friendship>>
    suspend fun existsBetweenUsers(user1Id: UUID, user2Id: UUID): Result<Boolean>
    suspend fun deleteBetweenUsers(user1Id: UUID, user2Id: UUID): Result<Unit>
}
