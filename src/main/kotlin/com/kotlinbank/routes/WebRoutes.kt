package com.kotlinbank.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val resetPasswordPage: String by lazy {
    object {}.javaClass.getResource("/web/reset-password.html")?.readText()
        ?: error("web/reset-password.html missing from resources")
}

fun Route.webRoutes() {
    get("/reset-password") {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        call.respondText(resetPasswordPage, ContentType.Text.Html)
    }
}
