package insa.eda.database.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import insa.eda.database.models.User

import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val createdAt: Date?,
    val isSynced: Boolean = false,
    val lastSyncTimeStamp: Long = System.currentTimeMillis()
) {
    fun toUser(): User {
        return User(
            uid = uid,
            name = name,
            email = email,
            phone = phone,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromUser(user: User): UserEntity {
            return UserEntity(
                uid = user.uid!!,
                name = user.name,
                email = user.email,
                phone = user.phone,
                createdAt = user.createdAt
            )
        }
    }
}
