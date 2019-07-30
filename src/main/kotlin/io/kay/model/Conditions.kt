package io.kay.model

import io.kay.web.dto.ConditionsDTO
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.*

object Conditions : UUIDTable() {
    val user = reference("user", Users)
    val monday = datetime("monday")
    val tuesday = datetime("tuesday")
    val wednesday = datetime("wednesday")
    val thursday = datetime("thursday")
    val friday = datetime("friday")
    val saturday = datetime("saturday")
    val sunday = datetime("sunday")
    val from = date("from")
    val to = date("to")
    val initialVacation = integer("initialVacation")
    val consumedVacation = integer("consumedVacation")
}

class Condition(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Condition>(Conditions)

    var user by User referencedOn Conditions.user
    var monday by Conditions.monday
    var tuesday by Conditions.tuesday
    var wednesday by Conditions.wednesday
    var thursday by Conditions.thursday
    var friday by Conditions.friday
    var saturday by Conditions.saturday
    var sunday by Conditions.sunday
    var from by Conditions.from
    var to by Conditions.to
    var initialVacation by Conditions.initialVacation
    var consumedVacation by Conditions.consumedVacation
}

fun Condition.toConditionsDTO() =
    ConditionsDTO(
        this.monday.toLocalTime(),
        this.tuesday.toLocalTime(),
        this.wednesday.toLocalTime(),
        this.thursday.toLocalTime(),
        this.friday.toLocalTime(),
        this.saturday.toLocalTime(),
        this.sunday.toLocalTime(),
        this.from.toLocalDate(),
        this.to.toLocalDate(),
        this.initialVacation - this.consumedVacation,
        this.id.value
    )