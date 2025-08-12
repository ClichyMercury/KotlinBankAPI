package com.kotlinbank.plugins

import com.kotlinbank.data.MockData
import com.kotlinbank.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {

        // Route de test
        get("/") {
            call.respondText("""
        🏦 KotlinBank API is running!
        
        📱 Available Endpoints:
        
        🏠 DASHBOARD:
        - GET /api/dashboard
        - GET /api/dashboard/balance
        - GET /api/dashboard/recent-transactions
        
        💳 WALLET:
        - GET /api/wallet
        - GET /api/wallet/balance
        - GET /api/wallet/transactions
        - GET /api/wallet/quick-actions
        
        📊 TRANSACTIONS:
        - GET /api/transactions
        - GET /api/transactions/{id}
        
        🔔 NOTIFICATIONS:
        - GET /api/notifications
        - GET /api/notifications/{id}
        
        💳 CARDS:
        - GET /api/cards
        - GET /api/cards/{id}
        
        👤 PROFILE:
        - GET /api/profile
        - GET /api/profile/user
        - GET /api/profile/stats
        - GET /api/profile/preferences
        - PUT /api/profile/preferences
        
        🎮 GAMIFICATION:
        - GET /api/gamification/profile
        - GET /api/gamification/levels
        - GET /api/gamification/achievements
        - GET /api/gamification/daily-missions
        - POST /api/gamification/complete-mission/{missionId}
        
        🤝 TONTINES (Communauté):
        - GET /api/tontines
        - GET /api/tontines/{id}
        
        🛒 MARKETPLACE:
        - GET /api/marketplace/items
        - GET /api/marketplace/categories
        
        📚 ÉDUCATION:
        - GET /api/education/content
        - GET /api/education/insights
        
        🌍 Total: 28 endpoints disponibles
        🎯 Spécialement conçu pour l'Afrique avec gamification !
    """.trimIndent())
        }

        // Routes API
        route("/api") {

            // HOME DASHBOARD
            get("/dashboard") {
                println("🏠 GET /api/dashboard appelé")
                call.respond(MockData.homeDashboard)
            }

            get("/dashboard/balance") {
                println("💰 GET /api/dashboard/balance appelé")
                call.respond(mapOf(
                    "totalBalance" to MockData.homeDashboard.totalBalance,
                    "income" to MockData.homeDashboard.income,
                    "expenses" to MockData.homeDashboard.expenses
                ))
            }

            get("/dashboard/recent-transactions") {
                println("📊 GET /api/dashboard/recent-transactions appelé")
                call.respond(MockData.homeDashboard.recentTransactions)
            }

            // Transactions
            get("/transactions") {
                println("📊 GET /api/transactions appelé")
                call.respond(MockData.transactions)
            }

            get("/transactions/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "ID manquant")
                )

                println("🔍 GET /api/transactions/$id appelé")
                val transaction = MockData.transactions.find { it.id == id }
                if (transaction != null) {
                    call.respond(transaction)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Transaction non trouvée")
                    )
                }
            }

            // Cards
            get("/cards") {
                println("💳 GET /api/cards appelé")
                call.respond(MockData.cards)
            }

            get("/cards/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "ID manquant")
                )

                println("🔍 GET /api/cards/$id appelé")
                val card = MockData.cards.find { it.id == id }
                if (card != null) {
                    call.respond(card)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Carte non trouvée")
                    )
                }
            }

            // Notifications
            get("/notifications") {
                println("🔔 GET /api/notifications appelé")
                call.respond(MockData.notifications)
            }

            get("/notifications/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "ID manquant")
                )

                println("🔍 GET /api/notifications/$id appelé")
                val notification = MockData.notifications.find { it.id == id }
                if (notification != null) {
                    call.respond(notification)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Notification non trouvée")
                    )
                }
            }

            // WALLET ROUTES
            get("/wallet") {
                println("💳 GET /api/wallet appelé")
                call.respond(MockData.walletData)
            }

            get("/wallet/balance") {
                println("💰 GET /api/wallet/balance appelé")
                call.respond(mapOf(
                    "totalBalance" to MockData.walletData.totalBalance,
                    "income" to MockData.walletData.income,
                    "expenses" to MockData.walletData.expenses,
                    "savings" to MockData.walletData.savings
                ))
            }

            get("/wallet/transactions") {
                println("📊 GET /api/wallet/transactions appelé")
                call.respond(MockData.walletData.transactions)
            }

            get("/wallet/quick-actions") {
                println("⚡ GET /api/wallet/quick-actions appelé")
                call.respond(MockData.walletData.quickActions)
            }

            // PROFILE ROUTES
            get("/profile") {
                println("👤 GET /api/profile appelé")
                call.respond(MockData.profileData)
            }

            get("/profile/user") {
                println("👤 GET /api/profile/user appelé")
                call.respond(MockData.profileData.userProfile)
            }

            get("/profile/stats") {
                println("📊 GET /api/profile/stats appelé")
                call.respond(MockData.profileData.quickStats)
            }

            get("/profile/preferences") {
                println("⚙️ GET /api/profile/preferences appelé")
                call.respond(MockData.profileData.preferences)
            }

            put("/profile/preferences") {
                println("⚙️ PUT /api/profile/preferences appelé")
                // TODO: Récupérer les données du body et sauvegarder
                call.respond(mapOf("success" to true, "message" to "Preferences updated"))
            }

            // GAMIFICATION ROUTES
            get("/gamification/profile") {
                println("🎮 GET /api/gamification/profile appelé")
                call.respond(MockData.userGameProfile)
            }

            get("/gamification/levels") {
                println("📊 GET /api/gamification/levels appelé")
                call.respond(MockData.userLevels)
            }

            get("/gamification/achievements") {
                println("🏆 GET /api/gamification/achievements appelé")
                call.respond(MockData.achievements)
            }

            get("/gamification/daily-missions") {
                println("📋 GET /api/gamification/daily-missions appelé")
                call.respond(MockData.dailyMissions)
            }

            post("/gamification/complete-mission/{missionId}") {
                val missionId = call.parameters["missionId"]
                println("✅ POST /api/gamification/complete-mission/$missionId appelé")
                call.respond(mapOf("success" to true, "pointsEarned" to 100, "ecoCoinsEarned" to 20))
            }

// TONTINES ROUTES
            get("/tontines") {
                println("🤝 GET /api/tontines appelé")
                call.respond(MockData.tontines)
            }

            get("/tontines/{id}") {
                val id = call.parameters["id"]
                println("🤝 GET /api/tontines/$id appelé")
                val tontine = MockData.tontines.find { it.id == id }
                if (tontine != null) {
                    call.respond(tontine)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tontine non trouvée"))
                }
            }

// MARKETPLACE ROUTES
            get("/marketplace/items") {
                println("🛒 GET /api/marketplace/items appelé")
                call.respond(MockData.marketplaceItems)
            }

            get("/marketplace/categories") {
                println("📂 GET /api/marketplace/categories appelé")
                val categories = MockData.marketplaceItems.map { it.category }.distinct()
                call.respond(categories)
            }

// EDUCATION ROUTES
            get("/education/content") {
                println("📚 GET /api/education/content appelé")
                call.respond(MockData.educationalContent)
            }

            get("/education/insights") {
                println("💡 GET /api/education/insights appelé")
                call.respond(MockData.spendingInsights)
            }
        }
    }
    println("✅ Routes configurées")
}