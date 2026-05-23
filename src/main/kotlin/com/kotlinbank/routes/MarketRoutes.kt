package com.kotlinbank.routes

import com.kotlinbank.models.AssetType
import com.kotlinbank.models.dto.toResponse
import com.kotlinbank.services.NotFoundException
import com.kotlinbank.services.ValidationException
import com.kotlinbank.services.market.MarketDataService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.marketRoutes() {
    route("/api/v1/market") {

        get("/assets") {
            val type = call.parameters["type"]?.let { raw ->
                runCatching { AssetType.valueOf(raw.uppercase()) }.getOrElse {
                    throw ValidationException("Invalid type: must be STOCK, FOREX or CRYPTO")
                }
            }
            val assets = MarketDataService.listAssets(type)
            call.respond(assets.map { it.toResponse() })
        }

        get("/assets/{id}") {
            val id = call.parameters["id"]?.let { raw ->
                runCatching { UUID.fromString(raw) }.getOrElse {
                    throw ValidationException("Invalid asset id (must be a UUID)")
                }
            } ?: throw ValidationException("Missing asset id")

            val asset = MarketDataService.getAsset(id)
                ?: throw NotFoundException("Asset not found")
            call.respond(asset.toResponse())
        }
    }
}
