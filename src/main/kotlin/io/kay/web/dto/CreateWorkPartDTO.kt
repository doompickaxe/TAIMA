package io.kay.web.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateWorkPartDTO(
    @JsonProperty("start") val start: LocalTime,
    @JsonProperty("end") val end: LocalTime? = null
)
