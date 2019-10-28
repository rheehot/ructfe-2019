package managers

import messages.RegisterData
import messages.UserPair
import models.Info
import models.User
import models.Users
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

class UserManager {
    private val hasher = MessageDigest.getInstance("SHA-256")

    fun createNewUser(regData: RegisterData) = transaction {
        val (schema, content) = regData.rawInfoContent.split(":")
        val info = Info.new {
            this.schema = Base64.getDecoder().decode(schema)
            this.content = content.toByteArray()
        }

        User.new {
            this.name = regData.name
            this.passwordHash = hasher.digest(regData.password.toByteArray())
            this.coordinates = Random.nextInt(0, 255) * 256 + Random.nextInt(0, 255)
            this.color = regData.color
            this.info = info
        }
    }

    fun isUserExists(name: String): Boolean = transaction {
        !User.find { Users.name eq name }.empty()
    }

    fun validate(userPair: UserPair): Boolean = transaction {
        User.find { Users.name eq userPair.name }.firstOrNull()?.let { dbUser ->
            hasher.digest(userPair.password.toByteArray())!!.contentEquals(dbUser.passwordHash)
        } ?: false
    }

    fun userByName(name: String): User? = transaction {
        User.find { Users.name eq name }.firstOrNull()
    }

    val users: List<User>
        get() = transaction {
            User.all().toList()
        }

    fun info(user: User): Info = transaction {
        user.info
    }
}
