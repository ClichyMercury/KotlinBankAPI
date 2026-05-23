package com.kotlinbank.routes

import com.kotlinbank.plugins.AUTH_JWT
import com.kotlinbank.services.PortfolioService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.portfolioRoutes() {
    authenticate(AUTH_JWT) {
        route("/api/v1/portfolio") {
            get {
                call.respond(PortfolioService.getPortfolio(call.userId()))
            }
        }
    }
}
