package io.kay.service

import io.kay.model.*
import io.kay.web.dto.FreePartDTO
import io.kay.web.dto.WorkPartDTO
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class LogRepository {
    companion object {
        fun getWorkPartsByDay(day: DateTime, email: String) =
            transaction {
                WorkParts
                    .leftJoin(Users)
                    .select { Users.email.eq(email) and WorkParts.day.eq(day) }
                    .toWorkParts()
            }

        fun logWork(email: String, requestDay: DateTime, body: WorkPartDTO): WorkPartDTO? {
            return transaction {
                val hasFreeParts = FreeParts.leftJoin(Users)
                    .select { Users.email.eq(email) and FreeParts.day.eq(requestDay) }
                    .empty()

                if (!hasFreeParts) {
                    return@transaction null
                }

                WorkPart.new {
                    user = User.find { Users.email eq email }.first()
                    day = requestDay
                    start = body.start.toDateTimeToday()
                    end = body.end?.toDateTimeToday()
                }.toWorkPartDTO()
            }
        }

        fun updateWork(id: UUID, email: String, requestDay: DateTime, body: WorkPartDTO): WorkPartDTO? {
            return transaction {
                val work = WorkPart.findById(id) ?: return@transaction null

                if (work.user.email != email || work.day != requestDay)
                    return@transaction null

                with(work) {
                    start = body.start.toDateTimeToday()
                    end = body.end?.toDateTimeToday()
                    toWorkPartDTO()
                }
            }
        }

        fun deleteWorkById(id: UUID, email: String, requestDay: DateTime): Unit? {
            return transaction {
                val work = WorkPart.findById(id) ?: return@transaction null

                if (work.user.email != email || work.day != requestDay)
                    return@transaction null

                work.delete()
            }
        }

        fun hasFreePartByDay(day: DateTime, email: String) =
            transaction {
                !FreeParts
                    .leftJoin(Users)
                    .select { Users.email.eq(email) and FreeParts.day.eq(day) }
                    .empty()
            }

        fun getFreePartsByDay(day: DateTime, email: String) =
            transaction {
                FreeParts
                    .leftJoin(Users)
                    .select { Users.email.eq(email) and FreeParts.day.eq(day) }
                    .singleOrNull()
                    ?.toFreePart()
            }

        fun updateFreePart(id: UUID, email: String, requestDay: DateTime, body: FreePartDTO): FreePartDTO? {
            return transaction {
                val freePart = FreePart.findById(id)!!

                if (freePart.user.email != email || freePart.day != requestDay)
                    return@transaction null

                with(freePart) {
                    reason = body.reason
                    toFreePartDTO()
                }
            }
        }

        fun logFree(email: String, requestDay: DateTime, body: FreePartDTO): FreePartDTO? {
            return transaction {
                val hasWorkParts = WorkParts.leftJoin(Users)
                    .select { Users.email.eq(email) and WorkParts.day.eq(requestDay) }
                    .empty()

                if (!hasWorkParts) {
                    return@transaction null
                }

                FreePart.new {
                    user = User.find { Users.email eq email }.first()
                    day = requestDay
                    reason = body.reason
                }.toFreePartDTO()
            }
        }

        fun deleteFreePartById(id: UUID, email: String, requestDay: DateTime): Unit? {
            return transaction {
                val freePart = FreePart.findById(id) ?: return@transaction null

                if (freePart.user.email != email || freePart.day != requestDay)
                    return@transaction null

                freePart.delete()
            }
        }

        private fun ResultRow.toWorkPart() = WorkPartDTO(
            this[WorkParts.start].toLocalTime(),
            this[WorkParts.end]?.toLocalTime(),
            this[WorkParts.id].value
        )

        private fun Iterable<ResultRow>.toWorkParts(): List<WorkPartDTO> {
            return fold(mutableListOf<WorkPartDTO>()) { list, resultRow ->
                list.add(resultRow.toWorkPart())
                list
            }.toList()
        }

        private fun ResultRow.toFreePart() = FreePartDTO(
            this[FreeParts.reason],
            this[FreeParts.id].value
        )
    }
}