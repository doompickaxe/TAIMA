package io.kay.web

import io.kay.model.toConditionsDTO
import io.kay.service.ConditionsRepository
import io.kay.web.dto.ConditionsDTO
import io.kay.web.dto.UpsertConditionsDTO
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import org.joda.time.LocalDate

fun Route.conditions(conditionsRepository: ConditionsRepository) {
    route("/conditions") {
        get {
            val session = call.sessions.get<MailSession>()!!
            val conditions = conditionsRepository.findConditionsByUser(session.email)
            call.respond(conditions)
        }

        post {
            val session = call.sessions.get<MailSession>()!!
            val body = call.receive(UpsertConditionsDTO::class)

            if (body.to.isBefore(body.from)) {
                call.respond(HttpStatusCode.BadRequest, "To is before from.")
                return@post
            }

            val condition = conditionsRepository.addConditions(session.email, body)

            if (condition == null)
                call.respond(HttpStatusCode.BadRequest, "Overlaps with other conditions.")
            else
                call.respond(condition)
        }

        get("/current") {
            val session = call.sessions.get<MailSession>()!!
            val condition = conditionsRepository.findConditionByDate(session.email, LocalDate.now())
            if (condition == null)
                call.respond(HttpStatusCode.NotFound)
            else
                call.respond(condition)
        }

        get("/{day}") {
            val session = call.sessions.get<MailSession>()!!
            val day: LocalDate? = validateDay(call.parameters["day"]!!)
            if (day == null) {
                call.respond(HttpStatusCode.BadRequest, "Day was not in format 'yyyy-MM-dd'")
                return@get
            }

            val condition = conditionsRepository.findConditionByDate(session.email, day)
            if (condition == null)
                call.respond(HttpStatusCode.NotFound)
            else
                call.respond(condition)
        }

        route("/{id}") {
            get {
                val session = call.sessions.get<MailSession>()!!
                val condition = conditionsRepository.findConditionById(call.parameters["id"]!!, session.email)

                if (condition == null)
                    call.respond(HttpStatusCode.NotFound)
                else
                    call.respond(condition)
            }

            put {
                val session = call.sessions.get<MailSession>()!!
                val dto = call.receive(UpsertConditionsDTO::class)

                if (dto.to.isBefore(dto.from)) {
                    call.respond(HttpStatusCode.BadRequest, "To is before from.")
                    return@put
                }

                val condition = conditionsRepository.updateCondition(call.parameters["id"]!!, session.email, dto)
                if (condition == null)
                    call.respond(HttpStatusCode.NotFound)
                else
                    call.respond(condition.toConditionsDTO())
            }
        }
    }
}