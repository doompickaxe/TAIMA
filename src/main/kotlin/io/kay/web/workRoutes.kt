package io.kay.web

import io.kay.service.LogRepository
import io.kay.web.dto.FreePartDTO
import io.kay.web.dto.WorkPartDTO
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.util.*

fun Route.workday() {
    get {
        val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
        val day: LocalDate? = validateDay(call.parameters["day"]!!)

        if (day == null) {
            call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
            return@get
        }

        val workParts = LogRepository.getWorkPartsByDay(day.toDateTimeAtStartOfDay(), session.email)

        if (workParts.isEmpty()) {
            val freePart = LogRepository.hasFreePartByDay(day.toDateTimeAtStartOfDay(), session.email)
            if (freePart) {
                call.respondRedirect(call.request.uri + "/free")
                return@get
            }
        }

        call.respond(workParts)
    }

    get("/free") {
        val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
        val day: LocalDate? = validateDay(call.parameters["day"]!!)

        if (day == null) {
            call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
            return@get
        }

        val freePart = LogRepository.getFreePartsByDay(day.toDateTimeAtStartOfDay(), session.email)

        if (freePart == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(freePart)
        }
    }

    post("/work") {
        val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
        val requestDay = validateDay(call.parameters["day"]!!)

        if (requestDay == null) {
            call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
            return@post
        }

        val dto = call.receive(WorkPartDTO::class)
        if (dto.end != null && dto.end.isBefore(dto.start)) {
            call.respond(HttpStatusCode.BadRequest, "End cannot be before start.")
            return@post
        }

        val response = LogRepository.logWork(session.email, requestDay.toDateTimeAtStartOfDay(), dto)
        if (response == null)
            call.respond(
                HttpStatusCode.BadRequest,
                "Reason not to work is already logged for this day."
            )
        else
            call.respond(response)
    }

    post("/free") {
        val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
        val requestDay = LocalDate.parse(call.parameters["day"], DateTimeFormat.forPattern("yyyy-MM-dd"))
        val dto = call.receive(FreePartDTO::class)

        val response = LogRepository.logFree(session.email, requestDay.toDateTimeAtStartOfDay(), dto)
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
            val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
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

            val response = LogRepository.updateWork(
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
            val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
            val workId = call.parameters["id"]!!
            val requestDay = validateDay(call.parameters["day"]!!)
            if (requestDay == null) {
                call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
                return@delete
            }

            val response = LogRepository.deleteWorkById(
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
            val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
            val freeId = call.parameters["id"]!!
            val requestDay = validateDay(call.parameters["day"]!!)
            if (requestDay == null) {
                call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
                return@put
            }

            val dto = call.receive(FreePartDTO::class)
            val response = LogRepository.updateFreePart(
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
            val session = /*call.sessions.get<MailSession>()*/ MailSession("me@myself.test")
            val freeId = call.parameters["id"]!!
            val requestDay = validateDay(call.parameters["day"]!!)
            if (requestDay == null) {
                call.respond(HttpStatusCode.BadRequest, "Pattern 'yyyy-MM-dd' did not match.")
                return@delete
            }

            val response = LogRepository.deleteFreePartById(
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

fun validateDay(day: String): LocalDate? {
    return try {
        LocalDate.parse(day, DateTimeFormat.forPattern("yyyy-MM-dd"))
    }
    catch (e: Exception) {
        null
    }
}