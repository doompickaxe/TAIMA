package io.kay.web

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.kay.model.toDTO
import io.kay.service.ConditionsRepository
import io.kay.service.LogRepository
import io.kay.service.UserRepository
import io.kay.web.dto.UserDTO
import io.kay.web.routes.conditions
import io.kay.web.routes.report
import io.kay.web.routes.workday
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
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

fun Application.module() {
    mainWithDependencies(ConditionsRepository(), UserRepository(), LogRepository())
}

fun Application.mainWithDependencies(
    conditionsRepository: ConditionsRepository,
    userRepository: UserRepository,
    logRepository: LogRepository
) {
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
                workday(logRepository)
            }

            route("/report") {
                report(conditionsRepository, logRepository)
            }
        }
    }
}
