package io.kay.model

import io.kay.web.dto.UserDTO
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.*

object Users : UUIDTable() {
    val email = varchar("email", 255).uniqueIndex()
    val role = enumerationByName("role", 10, UserRole::class)
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var email by Users.email
    var role by Users.role
}

fun User.toDTO() = UserDTO(this.email, this.id.value)