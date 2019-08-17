package io.kay.web.routes

import io.kay.service.LogRepository
import io.kay.web.MailSession
import io.kay.web.dto.CreateWorkPartDTO
import io.kay.web.dto.FreePartDTO
import io.kay.web.dto.WorkPartDTO
import io.kay.web.validateDay
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.util.*

fun Route.workday(logRepository: LogRepository) {
    get {
        val session = call.sessions.get<MailSession>()!!
        val day: LocalDate? = validateDay(call.parameters["day"]!!)

        if (day == null) {
            call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
            return@get
        }

        val workParts = logRepository.findWorkPartsByDay(day.toDateTimeAtStartOfDay(), session.email)

        if (workParts.isEmpty()) {
            val freePart = logRepository.hasFreePartByDay(day.toDateTimeAtStartOfDay(), session.email)
            if (freePart) {
                call.respondRedirect(call.request.uri + "/free")
                return@get
            }
        }

        call.respond(workParts)
    }

    get("/free") {
        val session = call.sessions.get<MailSession>()!!
        val day: LocalDate? = validateDay(call.parameters["day"]!!)

        if (day == null) {
            call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
            return@get
        }

        val freePart = logRepository.findFreePartsByDay(day.toDateTimeAtStartOfDay(), session.email)

        if (freePart == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(freePart)
        }
    }

    post("/work") {
        val session = call.sessions.get<MailSession>()!!
        val requestDay = validateDay(call.parameters["day"]!!)

        if (requestDay == null) {
            call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
            return@post
        }

        val dto = call.receive(CreateWorkPartDTO::class)
        if (dto.end != null && dto.end.isBefore(dto.start)) {
            call.respond(HttpStatusCode.BadRequest, "End cannot be before start.")
            return@post
        }

        val response = logRepository.logWork(session.email, requestDay.toDateTimeAtStartOfDay(), dto)
        if (response == null)
            call.respond(
                HttpStatusCode.BadRequest,
                "Reason not to work is already logged for this day."
            )
        else
            call.respond(response)
    }

    post("/free") {
        val session = call.sessions.get<MailSession>()!!
        val requestDay = LocalDate.parse(call.parameters["day"], DateTimeFormat.forPattern("yyyy-MM-dd"))
        val dto = call.receive(FreePartDTO::class)

        val response = logRepository.logFreePart(session.email, requestDay.toDateTimeAtStartOfDay(), dto)
        if (response == null)
            call.respond(
                HttpStatusCode.BadRequest,
                "Reason not to work is already logged for this day."
            )
        else
            call.respond(response)
    }

    route("/work/{id}") {
        put {
            val session = call.sessions.get<MailSession>()!!
            val workId = call.parameters["id"]!!
            val requestDay = validateDay(call.parameters["day"]!!)
            if (requestDay == null) {
                call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
                return@put
            }

            val dto = call.receive(WorkPartDTO::class)
            if (dto.end != null && dto.end.isBefore(dto.start)) {
                call.respond(HttpStatusCode.BadRequest, "End cannot be before start.")
                return@put
            }

            val response = logRepository.updateWork(
                UUID.fromString(workId),
                session.email,
                requestDay.toDateTimeAtStartOfDay(),
                dto
            )

            if (response == null)
                call.respond(
                    HttpStatusCode.BadRequest,
                    "The given ID did not match with the given date."
                )
            else
                call.respond(response)
        }

        delete {
            val session = call.sessions.get<MailSession>()!!
            val workId = call.parameters["id"]!!
            val requestDay = validateDay(call.parameters["day"]!!)
            if (requestDay == null) {
                call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
                return@delete
            }

            val response = logRepository.deleteWorkById(
                UUID.fromString(workId),
                session.email,
                requestDay.toDateTimeAtStartOfDay()
            )

            if (response == null)
                call.respond(
                    HttpStatusCode.BadRequest,
                    "The given ID was either not found or did not match the given date."
                )
            else
                call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/free/{id}") {
        put {
            val session = call.sessions.get<MailSession>()!!
            val freeId = call.parameters["id"]!!
            val requestDay = validateDay(call.parameters["day"]!!)
            if (requestDay == null) {
                call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
                return@put
            }

            val dto = call.receive(FreePartDTO::class)
            val response = logRepository.updateFreePart(
                UUID.fromString(freeId),
                session.email,
                requestDay.toDateTimeAtStartOfDay(),
                dto
            )

            if (response == null)
                call.respond(
                    HttpStatusCode.BadRequest,
                    "The given ID did not match with the given date."
                )
            else
                call.respond(response)
        }

        delete {
            val session = call.sessions.get<MailSession>()!!
            val freeId = call.parameters["id"]!!
            val requestDay = validateDay(call.parameters["day"]!!)
            if (requestDay == null) {
                call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
                return@delete
            }

            val response = logRepository.deleteFreePartById(
                UUID.fromString(freeId),
                session.email,
                requestDay.toDateTimeAtStartOfDay()
            )

            if (response == null)
                call.respond(
                    HttpStatusCode.BadRequest,
                    "The given ID was either not found or did not match the given date."
                )
            else
                call.respond(HttpStatusCode.NoContent)
        }
    }
}
