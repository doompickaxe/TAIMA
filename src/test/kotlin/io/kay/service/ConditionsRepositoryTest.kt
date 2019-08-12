package io.kay.service

import io.kay.model.*
import io.kay.web.dto.ConditionsDTO
import io.kay.web.dto.UpsertConditionsDTO
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.*

class ConditionsRepositoryTest {

    private val conditionsRepository = ConditionsRepository()

    @Before
    fun setup() {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            SchemaUtils.create(Users, Conditions)
        }
    }

    @After
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Users, Conditions)
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
            val returnList = conditionsRepository.findConditionByDate("me@myself.test", LocalDate.parse("2019-12-29"))

            assertEquals(expected, returnList)
        }
    }

    @Test
    fun returnsConditionsByDateOfCondition() {
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

    @Test
    fun returnsConditionsByLastDateOfCondition() {
        val expected = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val returnList = conditionsRepository.findConditionByDate("me@myself.test", LocalDate.parse("2019-12-31"))

            assertEquals(expected, returnList)
        }
    }

    @Test
    fun doesNotAddConditionsIfItOverlaps() {
        val oldCondition = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val overlappingCondition = UpsertConditionsDTO(
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                oldCondition.from,
                oldCondition.from.plusDays(3),
                0,
                0
            )

            val result = conditionsRepository.addConditions("me@myself.test", overlappingCondition)

            assertNull(result)
        }
    }

    @Test
    fun setsIdAfterInsertingNewCondition() {
        val oldCondition = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val overlappingCondition = UpsertConditionsDTO(
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                oldCondition.to.plusDays(1),
                oldCondition.to.plusDays(3),
                0,
                0
            )

            val result = conditionsRepository.addConditions("me@myself.test", overlappingCondition)

            assertNotNull(result)
            assertNotNull(result.id)
        }
    }

    @Test
    fun returnsNullWhenConditionByIdNotFound() {
        transaction {
            val result = conditionsRepository.findConditionById(UUID.randomUUID().toString(), "me@myself.test")

            assertNull(result)
        }
    }

    @Test
    fun returnsNullWhenConditionIsOwnedBySomeoneElse() {
        val savedCondition = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val result = conditionsRepository.findConditionById(savedCondition.id.toString(), "someone@else.test")

            assertNull(result)
        }
    }

    @Test
    fun returnsCondition() {
        val savedCondition = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val result = conditionsRepository.findConditionById(savedCondition.id.toString(), "me@myself.test")

            assertEquals(savedCondition, result)
        }
    }

    @Test
    fun returnsNullWhenUpdateConditionNotFound() {
        transaction {
            val update = UpsertConditionsDTO(
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                0,
                0
            )

            val result = conditionsRepository.updateCondition(UUID.randomUUID().toString(), "me@myself.test", update)

            assertNull(result)
        }
    }

    @Test
    fun returnsNullWhenConditionIsOwnedBySomeoneElseOnUpdate() {
        val savedCondition = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val update = UpsertConditionsDTO(
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                0,
                0
            )

            val result = conditionsRepository.updateCondition(savedCondition.id.toString(), "someone@else.test", update)

            assertNull(result)
        }
    }

    @Test
    fun updatesCondition() {
        val savedCondition = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val update = UpsertConditionsDTO(
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                LocalTime(),
                savedCondition.from,
                savedCondition.to,
                0,
                0
            )

            val result = conditionsRepository.updateCondition(savedCondition.id.toString(), "me@myself.test", update)

            assertNotNull(result)
            assertEquals(update.monday, result.monday.toLocalTime())
            assertEquals(update.initialVacation, result.initialVacation)
            assertEquals(update.consumedVacation, result.consumedVacation)
        }
    }

    @Test
    fun returnsNoConditionsOnOutrangedDaterange() {
        transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val from = LocalDate.parse("2018-11-11")
            val to = LocalDate.parse("2018-11-13")
            val result = conditionsRepository.findConditionByDate("me@myself.test", from, to)

            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun returnsConditionsIfInDaterange() {
        val expected = transaction {
            val me = User.new {
                email = "me@myself.test"
                role = UserRole.ADMIN
            }
            Condition.testCase(me).toConditionsDTO()
        }

        transaction {
            val from = expected.from
            val to = from.plusDays(3)
            val returnList = conditionsRepository.findConditionByDate("me@myself.test", from, to)

            assertEquals(listOf(expected), returnList)
        }
    }

    private fun Condition.Companion.testCase(owner: User) = new {
        user = owner
        from = LocalDate(2019, 12, 29).toDateTimeAtStartOfDay()
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
