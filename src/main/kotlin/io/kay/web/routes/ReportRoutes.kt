package io.kay.web.routes

import io.kay.service.ConditionsRepository
import io.kay.service.LogRepository
import io.kay.web.MailSession
import io.kay.web.dto.ConditionsDTO
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
import org.joda.time.DateTimeConstants
import org.joda.time.Interval
import org.joda.time.LocalDate

fun Route.report(conditionsRepository: ConditionsRepository, logRepository: LogRepository) {
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

        val csv = createCSV(conditionsRepository, logRepository, from, to, session.email)
        call.respondText(csv, ContentType.Text.CSV)
    }
}

fun createCSV(
    conditionsRepository: ConditionsRepository,
    logRepository: LogRepository,
    from: LocalDate,
    to: LocalDate,
    email: String
): String {
    val workPartsInTimespan = logRepository.findWorkPartsByDay(
        from.toDateTimeAtStartOfDay(),
        to.toDateTimeAtStartOfDay(),
        email
    )
    val freePartsInTimespan = logRepository.findFreePartsByDay(
        from.toDateTimeAtStartOfDay(),
        to.toDateTimeAtStartOfDay(),
        email
    )
    val conditionsInTimespan = conditionsRepository.findConditionByDate(email, from, to)

    val workPartsGroupedByDay = workPartsInTimespan.groupBy { it.day }
    val maxWorkParts = getMaxWorkParts(workPartsGroupedByDay)

    var csv = workPartsGroupedByDay
        .map { Pair(normalizeLine(it.value, maxWorkParts!!), it.key) }
        .map { "${it.first}${addExpectedTime(it.second, conditionsInTimespan)}" }
        .fold("") { acc, dto -> "$acc$dto\n" }

    val freePartsCSV = freePartsInTimespan
        .fold("") { acc, dto ->
            "$acc${dto.day};${"".padEnd((maxWorkParts ?: 0) * 2, ';')};${dto.reason};\n"
        }
    csv += freePartsCSV

    return "${buildHeaderLine(maxWorkParts)}$csv"
}

private fun buildHeaderLine(maxWorkParts: Int?): String {
    var header = "Day;"
    for (c in 0..(maxWorkParts ?: 0) step 2) {
        val number = c / 2 + 1
        header += "Start $number;End $number;"
    }
    return "${header}Time Goal;\n"
}

private fun addExpectedTime(day: LocalDate, conditions: List<ConditionsDTO>): String {
    val condition = conditions.find {
        Interval(it.from.toDateTimeAtStartOfDay(), it.to.toDateTimeAtStartOfDay())
            .contains(day.toDateTimeAtStartOfDay())
    }!!

    return when (day.dayOfWeek) {
        DateTimeConstants.MONDAY -> condition.monday.toString()
        DateTimeConstants.TUESDAY -> condition.tuesday.toString()
        DateTimeConstants.WEDNESDAY -> condition.wednesday.toString()
        DateTimeConstants.THURSDAY -> condition.thursday.toString()
        DateTimeConstants.FRIDAY -> condition.friday.toString()
        DateTimeConstants.SATURDAY -> condition.saturday.toString()
        DateTimeConstants.SUNDAY -> condition.sunday.toString()
        else -> throw IllegalStateException("Unknown constant in dayOfWeek")
    }
}

private fun getMaxWorkParts(workPartsGroupedByDay: Map<LocalDate, List<WorkPartDTO>>): Int? {
    val firstPart = workPartsGroupedByDay.values.firstOrNull()?.size
    return workPartsGroupedByDay.values.fold(firstPart) { highest, next ->
        if (next.size > highest!!)
            next.size
        else
            highest
    }
}

fun normalizeLine(parts: List<WorkPartDTO>, paddingSize: Int): String {
    var line = parts.fold("${parts.firstOrNull()?.day ?: ""};") { acc, part ->
        "$acc${part.start};${part.end ?: ""};"
    }

    for (x in 1..(paddingSize - parts.size))
        line += ";;"

    return line
}
