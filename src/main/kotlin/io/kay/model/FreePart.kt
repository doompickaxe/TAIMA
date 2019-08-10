package io.kay.model

import io.kay.web.dto.FreePartDTO
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.*

object FreeParts : UUIDTable() {
    val day = date("day")
    val user = reference("user", Users)
    val reason = enumerationByName("reason", 25, FreeType::class)
}

class FreePart(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FreePart>(FreeParts)

    var day by FreeParts.day
    var user by User referencedOn FreeParts.user
    var reason by FreeParts.reason
}

fun FreePart.toFreePartDTO() = FreePartDTO(
    this.reason,
    this.day.toLocalDate(),
    this.id.value
)