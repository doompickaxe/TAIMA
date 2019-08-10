package io.kay.web

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.kay.config.Config
import io.kay.model.toDTO
import io.kay.service.ConditionsRepository
import io.kay.service.LogRepository
import io.kay.service.UserRepository
import io.kay.web.dto.UserDTO
import io.kay.web.dto.WorkPartDTO
import io.ktor.application.*
import io.ktor.auth.OAuthServerSettings
import io.ktor.features.ContentNegotiation
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.thymeleaf.Thymeleaf
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.text.SimpleDateFormat

class Server {
    companion object {
        fun createServer(): ApplicationEngine {
            return embeddedServer(
                Netty,
                port = 8080,
                module = Application::module
            )
        }
    }
}

fun googleOAuthProvider(): OAuthServerSettings.OAuth2ServerSettings {
    val config = Config.readAuthConfig()
    return OAuthServerSettings.OAuth2ServerSettings(
        name = "google",
        authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
        accessTokenUrl = "https://www.googleapis.com/oauth2/v3/token",
        requestMethod = HttpMethod.Post,

        clientId = "${config.clientId}.apps.googleusercontent.com",
        clientSecret = config.clientSecret,
        defaultScopes = listOf("email")
    )
}

fun ApplicationCall.redirectUrl(path: String): String {
    val defaultPort = if (request.origin.scheme == "http") 80 else 443
    val hostPort = request.host() + request.port().let { port -> if (port == defaultPort) "" else ":$port" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort$path"
}

fun Application.module() {
    mainWithDependencies(ConditionsRepository(), UserRepository())
}

fun Application.mainWithDependencies(conditionsRepository: ConditionsRepository, userRepository: UserRepository) {
    install(Sessions) {
        header<MailSession>("sessionId")
    }
    install(ContentNegotiation) {
        jackson {
            registerModules(KotlinModule(), JodaModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            dateFormat = SimpleDateFormat("yyyy-MM-dd")
        }
    }
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }

    routing {
        intercept(ApplicationCallPipeline.Features) {
            call.sessions.set(MailSession("me@myself.test"));
        }
        route("/rest/user") {
            post {
                val session = call.sessions.get<MailSession>()!!

                if (!userRepository.isAdmin(session.email)) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }

                val body = call.receive(UserDTO::class)
                val user = userRepository.createUser(body.email)

                if (user == null)
                    call.respond(HttpStatusCode.BadRequest, "User already exists.")
                else
                    call.respond(user.toDTO())
            }

            conditions(conditionsRepository)

            route("/{day}") {
                workday()
            }

            route("/report") {
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

                    val workPartsInTimespan = LogRepository.getWorkPartsByDay(
                        from.toDateTimeAtStartOfDay(),
                        to.toDateTimeAtStartOfDay(),
                        session.email
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
                        session.email
                    ).fold("") { acc, dto ->
                        "$acc${dto.day};${"".padEnd((maxWorkPartsPerDay ?: 0) * 2, ';')};${dto.reason};\n"
                    }

                    csv += freePartsInTimespan

                    call.respondText(csv, ContentType.Text.CSV)
                }
            }
        }
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
