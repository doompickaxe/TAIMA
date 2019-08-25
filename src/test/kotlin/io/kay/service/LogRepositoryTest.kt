package io.kay.service

import io.kay.model.*
import io.kay.web.dto.CreateWorkPartDTO
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.math.exp
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    @Test
    fun returnsNoWorkPartsOnOutrangedDate() {
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

        val result = repository.findWorkPartsByDay(expected.start.minusDays(1), expected.end!!.minusDays(1), "me@myself.test")

        assertEquals(listOf(), result)
    }

    @Test
    fun containsResultIfInDateRange() {
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

        val result = repository.findWorkPartsByDay(expected.start.minusDays(1), expected.end!!.plusDays(1), "me@myself.test")

        assertEquals(listOf(expected.toWorkPartDTO()), result)
    }

    @Test
    fun doesNotLogWorkIfFreepartIsLogged() {
        transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.USER
            }
            FreePart.new {
                day = DateTime()
                user = me
                reason = FreeType.ILL
            }
        }

        val result = repository.logWork("me@myself.test", DateTime(), CreateWorkPartDTO(LocalTime()))

        assertNull(result)
    }

    @Test
    fun savesWork() {
        val me = transaction {
            User.new {
                email = "me@myself.test"
                role = UserRole.USER
            }
        }

        val result = repository.logWork("me@myself.test", DateTime(), CreateWorkPartDTO(LocalTime()))

        assertNotNull(result)
        assertNotNull(result.id)
    }
}
