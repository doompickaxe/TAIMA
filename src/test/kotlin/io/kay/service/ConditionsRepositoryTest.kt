package io.kay.service

import io.kay.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ConditionsRepositoryTest {

    private val conditionsRepository = ConditionsRepository()

    @Before
    fun setup() {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            SchemaUtils.create(Users, Conditions)
        }
    }

    @Test
    fun returnsNothingWhenUserIsUnknown() {
        transaction {
            val returnList = conditionsRepository.findConditionsByUser("none")

            assertEquals(listOf(), returnList)
        }
    }

    @Test
    fun returnsConditionsOfUser() {
        val expected = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val returnList = conditionsRepository.findConditionsByUser("me@myself.test")

            assertEquals(listOf(expected), returnList)
        }
    }

    @Test
    fun returnsNoConditionsOnOutrangedDate() {
        transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val result = conditionsRepository.findConditionByDate("me@myself.test", LocalDate.parse("2018-11-11"))

            assertNull(result)
        }
    }

    @Test
    fun returnsConditionsByFirstDateOfCondition() {
        val expected = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val returnList = conditionsRepository.findConditionByDate("me@myself.test", LocalDate.parse("2019-12-30"))

            assertEquals(expected, returnList)
        }
    }

    private fun Condition.Companion.testCase(owner: User) = Condition.new {
        user = owner
        from = LocalDate(2019, 12, 30).toDateTimeAtStartOfDay()
        to = LocalDate(2019, 12, 31).toDateTimeAtStartOfDay()
        monday = LocalTime(5, 0).toDateTimeToday()
        tuesday = LocalTime(5, 0).toDateTimeToday()
        wednesday = LocalTime(5, 0).toDateTimeToday()
        thursday = LocalTime(5, 0).toDateTimeToday()
        friday = LocalTime(5, 0).toDateTimeToday()
        saturday = LocalTime(0, 0).toDateTimeToday()
        sunday = LocalTime(0, 0).toDateTimeToday()
        initialVacation = 16
        consumedVacation = 3
    }
}
