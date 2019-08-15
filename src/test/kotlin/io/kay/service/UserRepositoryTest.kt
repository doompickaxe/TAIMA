package io.kay.service

import io.kay.model.User
import io.kay.model.UserRole
import io.kay.model.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

class UserRepositoryTest {

    private val repository = UserRepository()

    @Before
    fun setup() {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            SchemaUtils.create(Users)
        }
    }

    @After
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Users)
        }
    }

    @Test
    fun createsUser() {
        val user = repository.createUser("me@myself.test")

        assertNotNull(user)
        assertNotNull(user.id)
        assertEquals("me@myself.test", user.email)
        assertEquals(UserRole.USER, user.role)
    }

    @Test
    fun doesNotCreateUserWithDuplicatedEmail() {
        transaction {
            User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
        }

        val user = repository.createUser("me@myself.test")

        assertNull(user)
    }

    @Test
    fun findsUserByEmail() {
        val user = transaction {
            User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
        }

        val result = repository.findUserByEmail(user.email)

        assertNotNull(result)
        assertEquals(user.id, result.id)
    }

    @Test
    fun findUserByEmailReturnsNullWhenNotExist() {
        val result = repository.findUserByEmail("no@body.anywhere")

        assertNull(result)
    }

    @Test
    fun isAdminReturnsTrueWhenAdmin() {
        val user = transaction {
            User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
        }

        val result = repository.isAdmin(user.email)

        assertTrue(result)
    }

    @Test
    fun isAdminReturnsFalseWhenUser() {
        val user = transaction {
            User.new {
                email = "me@myself.test"
                role = UserRole.USER
            }
        }

        val result = repository.isAdmin(user.email)

        assertFalse(result)
    }

    @Test
    fun isAdminReturnsFalseWhenUserNotExist() {
        val result = repository.isAdmin("no@body.anywhere")

        assertFalse(result)
    }
}