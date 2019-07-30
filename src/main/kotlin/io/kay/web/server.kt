package io.kay.web

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.kay.config.Config
import io.kay.service.ConditionsRepository
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.auth.OAuthServerSettings
import io.ktor.features.ContentNegotiation
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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
    mainWithDependencies(ConditionsRepository())
}

fun Application.mainWithDependencies(conditionsRepository: ConditionsRepository) {
//    install(Sessions) {
//        cookie<MailSession>("sessionId") {
//            val key = hex("abcd")
//            transform(SessionTransportTransformerMessageAuthentication(key))
//        }
//    }
//    install(Authentication) {
//        oauth("google-oauth") {
//            client = HttpClient(Apache)
//            providerLookup = { googleOAuthProvider() }
//            urlProvider = { redirectUrl("/login") }
//        }
//    }
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
        //        authenticate("google-oauth") {
//            route("/login") {
//                handle {
//                    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
//                        ?: error("No principal")
//
//                    val json = HttpClient(Apache).get<String>("https://www.googleapis.com/userinfo/v2/me") {
//                        header("Authorization", "Bearer ${principal.accessToken}")
//                    }
//
//                    val data = ObjectMapper().readTree(json)
//                    val id = data["id"].textValue()
//
//                    if (id != null) {
//                        call.sessions.set(MailSession(data["email"].textValue()))
//                    }
//                    call.respondRedirect("/work")
//                }
//            }
//        }

        route("/rest/user") {
            conditions(conditionsRepository)

            route("/{day}") {
                workday()
            }
        }
    }
}

