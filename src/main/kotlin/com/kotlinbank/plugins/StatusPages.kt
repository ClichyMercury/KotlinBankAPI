package com.kotlinbank.plugins

import com.kotlinbank.models.dto.ErrorResponse
import com.kotlinbank.services.ConflictException
import com.kotlinbank.services.NotFoundException
import com.kotlinbank.services.UnauthorizedException
import com.kotlinbank.services.ValidationException
import com.kotlinbank.services.monitoring.SentryReporter
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

private val statusPagesLog = LoggerFactory.getLogger("StatusPages")

private fun ApplicationCall.errorContext(): Map<String, String> = SentryReporter.requestContext(
    method = request.local.method.value,
    uri = request.local.uri,
    userId = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", cause.message ?: "Invalid input"))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("conflict", cause.message ?: "Resource conflict"))
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", cause.message ?: "Unauthorized"))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", cause.message ?: "Not found"))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", cause.message ?: "Bad request"))
        }
        exception<JsonConvertException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_json", cause.message ?: "Malformed JSON"))
        }
        exception<Throwable> { call, cause ->
            statusPagesLog.error("Unhandled exception", cause)
            SentryReporter.captureException(cause, call.errorContext())
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Something went wrong"))
        }
    }
}
