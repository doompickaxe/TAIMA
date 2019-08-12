package io.kay.service

import io.kay.model.*
import io.kay.web.dto.ConditionsDTO
import io.kay.web.dto.UpsertConditionsDTO
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Interval
import org.joda.time.LocalDate
import java.util.*

class ConditionsRepository {

    fun findConditionsByUser(email: String): List<ConditionsDTO> {
        return transaction {
            Conditions
                .leftJoin(Users)
                .select { Users.email eq email }
                .toConditions()
        }
    }

    fun findConditionByDate(email: String, date: LocalDate): ConditionsDTO? {
        return transaction {
            Conditions
                .leftJoin(Users)
                .select {
                    Users.email.eq(email) and
                            Conditions.from.lessEq(date.toDateTimeAtStartOfDay()) and
                            Conditions.to.greaterEq(date.toDateTimeAtStartOfDay())
                }
                .toConditions()
                .firstOrNull()
        }
    }

    fun findConditionByDate(email: String, from: LocalDate, to: LocalDate): List<ConditionsDTO> {
        return transaction {
            Conditions
                .leftJoin(Users)
                .select {
                    Users.email.eq(email) and
                            Conditions.from.lessEq(to.toDateTimeAtStartOfDay()) and
                            Conditions.to.greaterEq(from.toDateTimeAtStartOfDay())
                }
                .orderBy(Conditions.from)
                .toConditions()
        }
    }

    fun addConditions(email: String, body: UpsertConditionsDTO): ConditionsDTO? {
        val hasCollisions = findConditionsByUser(email)
            .sortedBy { Conditions.from }
            .any { overlaps(it, body) }

        return transaction {
            if (hasCollisions)
                null
            else
                Condition.new {
                    user = User.find { Users.email like email }.first()
                    monday = body.monday.toDateTimeToday()
                    tuesday = body.tuesday.toDateTimeToday()
                    wednesday = body.wednesday.toDateTimeToday()
                    thursday = body.thursday.toDateTimeToday()
                    friday = body.friday.toDateTimeToday()
                    saturday = body.saturday.toDateTimeToday()
                    sunday = body.sunday.toDateTimeToday()
                    from = body.from.toDateTimeAtStartOfDay()
                    to = body.to.toDateTimeAtStartOfDay()
                    initialVacation = body.initialVacation
                    consumedVacation = body.consumedVacation
                }.toConditionsDTO()
        }
    }

    fun findConditionById(id: String, email: String) = transaction {
        val condition = Condition.findById(UUID.fromString(id)) ?: return@transaction null

        if (condition.user.email != email)
            return@transaction null

        condition.toConditionsDTO()
    }

    fun updateCondition(id: String, email: String, body: UpsertConditionsDTO) = transaction {
        val condition = Condition.findById(UUID.fromString(id)) ?: return@transaction null

        if (condition.user.email != email)
            return@transaction null

        replaceCondition(body, condition)
    }

    private fun replaceCondition(dto: UpsertConditionsDTO, condition: Condition) = with(condition) {
        monday = dto.monday.toDateTimeToday()
        tuesday = dto.tuesday.toDateTimeToday()
        wednesday = dto.wednesday.toDateTimeToday()
        thursday = dto.thursday.toDateTimeToday()
        friday = dto.friday.toDateTimeToday()
        saturday = dto.saturday.toDateTimeToday()
        sunday = dto.sunday.toDateTimeToday()
        from = dto.from.toDateTimeAtStartOfDay()
        to = dto.to.toDateTimeAtStartOfDay()
        initialVacation = dto.initialVacation
        consumedVacation = dto.consumedVacation
        this
    }

    private fun ResultRow.toCondition() = ConditionsDTO(
        this[Conditions.monday].toLocalTime(),
        this[Conditions.tuesday].toLocalTime(),
        this[Conditions.wednesday].toLocalTime(),
        this[Conditions.thursday].toLocalTime(),
        this[Conditions.friday].toLocalTime(),
        this[Conditions.saturday].toLocalTime(),
        this[Conditions.sunday].toLocalTime(),
        this[Conditions.from].toLocalDate(),
        this[Conditions.to].toLocalDate(),
        this[Conditions.initialVacation] - this[Conditions.consumedVacation],
        this[Conditions.id].value
    )

    private fun Iterable<ResultRow>.toConditions() = fold(mutableListOf<ConditionsDTO>()) { list, resultRow ->
        list.add(resultRow.toCondition())
        list
    }.toList()

    private fun overlaps(a: ConditionsDTO, b: UpsertConditionsDTO): Boolean {
        return Interval(a.from.toDateTimeAtStartOfDay(), a.to.toDateTimeAtStartOfDay())
            .overlaps(Interval(b.from.toDateTimeAtStartOfDay(), b.to.toDateTimeAtStartOfDay()))
    }
}