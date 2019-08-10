package io.kay.model

import io.kay.web.dto.WorkPartDTO
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.*

object WorkParts : UUIDTable() {
    val day = date("day")
    val start = datetime("start")
    val end = datetime("end").nullable()
    val user = reference("user", Users)
}

class WorkPart(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<WorkPart>(WorkParts)

    var day by WorkParts.day
    var start by WorkParts.start
    var end by WorkParts.end
    var user by User referencedOn WorkParts.user
}

fun WorkPart.toWorkPartDTO() = WorkPartDTO(
    this.start.toLocalTime(),
    this.end?.toLocalTime(),
    this.day.toLocalDate(),
    this.id.value
)