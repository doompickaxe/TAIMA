package io.kay.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import java.util.*

data class WorkPartDTO(
    @JsonProperty("start") val start: LocalTime,
    @JsonProperty("end") val end: LocalTime? = null,
    @JsonProperty("day") val day: LocalDate,
    @JsonProperty("id") val id: UUID
)