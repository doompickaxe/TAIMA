package io.kay.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import java.util.*

data class UpsertConditionsDTO(
    @JsonProperty("monday") val monday: LocalTime,
    @JsonProperty("tuesday") val tuesday: LocalTime,
    @JsonProperty("wednesday") val wednesday: LocalTime,
    @JsonProperty("thursday") val thursday: LocalTime,
    @JsonProperty("friday") val friday: LocalTime,
    @JsonProperty("saturday") val saturday: LocalTime,
    @JsonProperty("sunday") val sunday: LocalTime,
    @JsonProperty("from") val from: LocalDate,
    @JsonProperty("to") val to: LocalDate,
    @JsonProperty("initialVacation") val initialVacation: Int,
    @JsonProperty("consumedVacation") val consumedVacation: Int,
    @JsonProperty("id") val id: UUID? = null
)