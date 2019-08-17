package io.kay.service

import io.kay.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LogRepositoryTest {

    private val repository = LogRepository()

    @Before
    fun setup() {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            SchemaUtils.create(FreeParts, WorkParts, Users)
        }
    }

    @After
    fun cleanUp() {
        transaction {
            SchemaUtils.drop(FreeParts, WorkParts, Users)
        }
    }

    @Test
    fun findWorkPartsByDayReturnsEmptyListIfUserUnknown() {
        transaction {
            val otherUser = User.new {
                email = "some@us.er"
                role = UserRole.USER
            }
            WorkPart.new {
                day = DateTime()
                start = DateTime()
                end = DateTime()
                user = otherUser
            }
        }

        val result = repository.findWorkPartsByDay(DateTime.now(), "me@myself.test")

        assertEquals(listOf(), result)
    }

    @Test
    fun findWorkPartsByDayReturnsEmptyListIfNothingIsInDatabase() {
        transaction {
            User.new {
                email = "me@myself.test"
                role = UserRole.USER
            }
        }

        val result = repository.findWorkPartsByDay(DateTime.now(), "me@myself.test")

        assertEquals(listOf(), result)
    }

    @Test
    fun findWorkPartsByDay() {
        val expected = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.USER
            }
            WorkPart.new {
                day = DateTime()
                start = DateTime()
                end = DateTime()
                user = me
            }
        }

        val result = repository.findWorkPartsByDay(DateTime.now(), "me@myself.test")

        assertEquals(listOf(expected.toWorkPartDTO()), result)
    }
}