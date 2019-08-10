package io.kay.web

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

fun validateDay(day: String): LocalDate? {
    return try {
        LocalDate.parse(day, DateTimeFormat.forPattern("yyyy-MM-dd"))
    }
    catch (e: Exception) {
        null
    }
}