package io.kay.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class UserDTO(
    @JsonProperty("email") val email: String,
    @JsonProperty("id") val id: UUID? = null
)