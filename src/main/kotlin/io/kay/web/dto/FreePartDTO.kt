package io.kay.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.kay.model.FreeType
import org.joda.time.LocalTime
import java.util.*

data class FreePartDTO(
    @JsonProperty("reason") val reason: FreeType,
    @JsonProperty("id") val id: UUID? = null
)