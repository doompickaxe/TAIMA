package io.kay.web.routes

import io.kay.service.ConditionsRepository
import io.kay.service.LogRepository
import io.kay.web.MailSession
import io.kay.web.dto.WorkPartDTO
import io.kay.web.validateDay
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import org.joda.time.LocalDate

fun Route.report(conditionsRepository: ConditionsRepository) {
    get {
        val session = call.sessions.get<MailSession>()!!
        if (call.parameters["from"] == null || call.parameters["to"] == null) {
            call.respond(HttpStatusCode.BadRequest, "Please set the query parameters from and to.")
            return@get
        }
        val from = validateDay(call.parameters["from"]!!)
        val to = validateDay(call.parameters["to"]!!)
        if (from == null || to == null) {
            call.respond(HttpStatusCode.BadRequest, "Parameter were not in format: yyyy-MM-dd.")
            return@get
        }

        val csv = createCSV(conditionsRepository, from, to, session.email)
        call.respondText(csv, ContentType.Text.CSV)
    }
}

fun createCSV(conditionsRepository: ConditionsRepository, from: LocalDate, to: LocalDate, email: String): String {
    val workPartsInTimespan = LogRepository.getWorkPartsByDay(
        from.toDateTimeAtStartOfDay(),
        to.toDateTimeAtStartOfDay(),
        email
    )

    val workPartsGroupedByDay = workPartsInTimespan.groupBy { it.day }
    val firstPart = workPartsGroupedByDay.values.firstOrNull()?.size
    val maxWorkPartsPerDay = workPartsGroupedByDay.values.fold(firstPart) { highest, next ->
        if (next.size > highest!!)
            next.size
        else
            highest
    }

    var csv = workPartsGroupedByDay.values
        .map { normalizeLine(it, maxWorkPartsPerDay!!) }
        .fold("") { acc, dto -> "$acc$dto\n" }

    val freePartsInTimespan = LogRepository.getFreePartsByDay(
        from.toDateTimeAtStartOfDay(),
        to.toDateTimeAtStartOfDay(),
        email
    ).fold("") { acc, dto ->
        "$acc${dto.day};${"".padEnd((maxWorkPartsPerDay ?: 0) * 2, ';')};${dto.reason};\n"
    }
    csv += freePartsInTimespan

    return csv
}

fun normalizeLine(parts: List<WorkPartDTO>, paddingSize: Int): String {
    var line = parts.fold("${parts.firstOrNull()?.day ?: ""};") { acc, part ->
        "$acc${part.start};${part.end ?: ""};"
    }

    for (x in 1..(paddingSize - parts.size))
        line += ";;"

    return line
}
