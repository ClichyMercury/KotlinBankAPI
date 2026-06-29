package com.kotlinbank.routes

import com.kotlinbank.models.dto.BuyOrderRequest
import com.kotlinbank.models.dto.SellOrderRequest
import com.kotlinbank.plugins.AUTH_JWT
import com.kotlinbank.services.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes() {
    authenticate(AUTH_JWT) {
        route("/api/v1/orders") {

            get {
                call.respond(OrderService.listUserOrders(call.userId()))
            }

            post("/buy") {
                val req = call.receive<BuyOrderRequest>()
                val order = OrderService.placeBuyOrder(call.userId(), req)
                call.respond(HttpStatusCode.Created, order)
            }

            post("/sell") {
                val req = call.receive<SellOrderRequest>()
                val order = OrderService.placeSellOrder(call.userId(), req)
                call.respond(HttpStatusCode.Created, order)
            }
        }
    }
}
