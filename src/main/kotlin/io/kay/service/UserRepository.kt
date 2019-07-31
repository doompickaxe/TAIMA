package io.kay.service

import io.kay.model.User
import io.kay.model.UserRole
import io.kay.model.Users
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {
    fun createUser(emailParam: String) = transaction {
        if (findUserByEmail(emailParam) != null)
            return@transaction null

        User.new {
            email = emailParam
            role = UserRole.USER
        }
    }

    fun isAdmin(email: String) = transaction {
        User.find { Users.email eq email }.any { it.role == UserRole.ADMIN }
    }

    fun findUserByEmail(email: String) = transaction {
        User.find { Users.email eq email }.firstOrNull()
    }
}