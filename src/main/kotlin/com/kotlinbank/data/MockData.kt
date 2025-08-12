package com.kotlinbank.data

import com.kotlinbank.models.*

object MockData {

    val transactions = listOf(
        TransactionDetail("1", "Netflix Subscription", "Monthly payment", "-$15.99", false, "Today", "2:30 PM", TransactionCategory.ENTERTAINMENT),
        TransactionDetail("2", "Uber Ride", "To downtown", "-$12.50", false, "Today", "10:15 AM", TransactionCategory.TRANSPORT),
        TransactionDetail("3", "Salary Deposit", "Monthly salary", "+$3,450.00", true, "Yesterday", "9:00 AM", TransactionCategory.SALARY),
        TransactionDetail("4", "Amazon Purchase", "Electronics", "-$67.85", false, "Yesterday", "3:45 PM", TransactionCategory.SHOPPING),
        TransactionDetail("5", "McDonald's", "Fast food", "-$8.99", false, "2 days ago", "12:30 PM", TransactionCategory.FOOD),
        TransactionDetail("6", "Transfer from John", "Personal transfer", "+$120.00", true, "2 days ago", "6:20 PM", TransactionCategory.TRANSFER),
        TransactionDetail("7", "Electricity Bill", "Monthly bill", "-$89.50", false, "3 days ago", "11:00 AM", TransactionCategory.BILLS),
        TransactionDetail("8", "Freelance Payment", "Design work", "+$850.00", true, "3 days ago", "4:15 PM", TransactionCategory.SALARY),
        TransactionDetail("9", "Spotify Premium", "Music subscription", "-$9.99", false, "4 days ago", "7:00 AM", TransactionCategory.ENTERTAINMENT, TransactionStatus.PENDING),
        TransactionDetail("10", "Grocery Store", "Weekly shopping", "-$125.43", false, "5 days ago", "6:30 PM", TransactionCategory.FOOD),
    )

    val notifications = listOf(
        BankNotification(
            id = "1",
            title = "Payment Received",
            message = "You received a payment from John Doe",
            type = NotificationType.TRANSACTION,
            timestamp = "5 min ago",
            amount = "+$1,250.00",
            isRead = false
        ),
        BankNotification(
            id = "2",
            title = "Security Alert",
            message = "New device login detected from iPhone. If this wasn't you, please secure your account immediately.",
            type = NotificationType.SECURITY,
            timestamp = "1 hour ago",
            isRead = false,
            isImportant = true,
            actionRequired = true
        ),
        BankNotification(
            id = "3",
            title = "Card Payment",
            message = "Payment processed at Amazon.com",
            type = NotificationType.TRANSACTION,
            timestamp = "2 hours ago",
            amount = "-$67.99",
            isRead = false
        ),
        BankNotification(
            id = "4",
            title = "Special Offer",
            message = "Get 2% cashback on all purchases this month! Limited time offer.",
            type = NotificationType.PROMOTION,
            timestamp = "3 hours ago",
            isRead = true
        ),
        BankNotification(
            id = "5",
            title = "Bill Reminder",
            message = "Your electricity bill of $89.50 is due tomorrow",
            type = NotificationType.REMINDER,
            timestamp = "1 day ago",
            isRead = false,
            actionRequired = true
        ),
        BankNotification(
            id = "6",
            title = "Card Expiring Soon",
            message = "Your Visa card ending in 5154 expires next month. Order a replacement card.",
            type = NotificationType.CARD,
            timestamp = "2 days ago",
            isRead = true,
            actionRequired = true
        ),
        BankNotification(
            id = "7",
            title = "System Maintenance",
            message = "Scheduled maintenance on Sunday 2-4 AM. Some services may be temporarily unavailable.",
            type = NotificationType.SYSTEM,
            timestamp = "3 days ago",
            isRead = true
        ),
        BankNotification(
            id = "8",
            title = "Large Transaction",
            message = "Large transaction detected: $850.00 transfer to your account",
            type = NotificationType.TRANSACTION,
            timestamp = "5 days ago",
            amount = "+$850.00",
            isRead = true,
            isImportant = true
        )
    )

    // NOUVELLES DONNÉES POUR HOME
    val homeDashboard = HomeDashboard(
        userName = "John Doe",
        greeting = getGreeting(),
        totalBalance = "$ 77,584",
        income = "+$12,450",
        expenses = "-$8,239",
        unreadNotifications = notifications.count { !it.isRead },
        recentTransactions = listOf(
            RecentTransaction("1", "Netflix", "-$15.99", "2h ago", false),
            RecentTransaction("2", "Salary", "+$3,450", "1 day ago", true),
            RecentTransaction("3", "Amazon", "-$67.85", "2 days ago", false),
            RecentTransaction("4", "Transfer from John", "+$120.00", "3 days ago", true),
            RecentTransaction("5", "McDonald's", "-$8.99", "5 days ago", false)
        ),
        quickActions = listOf(
            QuickAction("transfer", "Transfer", "swap_horiz", "#2196F3"),
            QuickAction("pay_bills", "Pay Bills", "receipt", "#4CAF50"),
            QuickAction("top_up", "Top Up", "add", "#FF9800"),
            QuickAction("more", "More", "grid_view", "#9C27B0")
        )
    )

    // DONNÉES WALLET
    val walletData = WalletData(
        totalBalance = 77584.0,
        income = 12450.0,
        expenses = 8239.0,
        savings = 45320.0,
        transactions = listOf(
            WalletTransaction("1", "Netflix Subscription", "Monthly payment", "-$15.99", false, "2h ago"),
            WalletTransaction("2", "Salary Deposit", "Monthly salary", "+$3,450.00", true, "1 day ago"),
            WalletTransaction("3", "Amazon Purchase", "Online shopping", "-$67.85", false, "2 days ago"),
            WalletTransaction("4", "Transfer from John", "Personal transfer", "+$120.00", true, "3 days ago"),
            WalletTransaction("5", "Grocery Store", "Food & beverages", "-$45.32", false, "4 days ago"),
            WalletTransaction("6", "Freelance Payment", "Design work", "+$850.00", true, "5 days ago")
        ),
        quickActions = listOf(
            WalletQuickAction("send", "Send", "arrow_upward", "#4CAF50"),
            WalletQuickAction("receive", "Receive", "arrow_downward", "#2196F3"),
            WalletQuickAction("add_card", "Add Card", "add", "#9C27B0"),
            WalletQuickAction("more", "More", "more_vert", "#607D8B")
        )
    )

    // DONNÉES PROFILE
    val profileData = ProfileData(
        userProfile = UserProfile(
            name = "John Doe",
            email = "john.doe@email.com",
            phone = "+1 (555) 123-4567",
            accountNumber = "****5154",
            memberSince = "March 2020",
            accountType = "Premium Account"
        ),
        quickStats = ProfileQuickStats(
            activeCards = 3,
            accounts = 2,
            totalTransactions = 127
        ),
        preferences = ProfilePreferences(
            notificationsEnabled = true,
            biometricEnabled = false,
            darkModeEnabled = false
        )
    )

    private fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val cards = listOf(
        CardItem(
            id = "card1",
            cardType = "Visa",
            cardNumber = "**** **** **** 1234",
            cardName = "JOHN DOE",
            balance = 1250.50,
            gradientColors = listOf("#1E3A8A", "#3B82F6")
        ),
        CardItem(
            id = "card2",
            cardType = "Mastercard",
            cardNumber = "**** **** **** 5678",
            cardName = "JOHN DOE",
            balance = 850.30,
            gradientColors = listOf("#DC2626", "#EF4444")
        )
    )

    // DONNÉES GAMIFICATION
    val userLevels = listOf(
        UserLevel(1, "Petit Épargnant", 0, listOf("Accès de base", "1 compte"), "🌱", "#4CAF50"),
        UserLevel(2, "Baobab Bronze", 500, listOf("Tontines", "2 comptes"), "🌳", "#8D6E63"),
        UserLevel(3, "Lion Argent", 1500, listOf("Marketplace", "Cashback 1%"), "🦁", "#9E9E9E"),
        UserLevel(4, "Aigle Or", 3000, listOf("Investissements", "Cashback 2%"), "🦅", "#FFD700"),
        UserLevel(5, "Diamant Ubuntu", 6000, listOf("VIP Support", "Cashback 3%"), "💎", "#E91E63")
    )

    val achievements = listOf(
        Achievement("first_transaction", "Premier Pas", "Effectuez votre première transaction", "🎯", 50, 10, AchievementCategory.TRANSACTIONS),
        Achievement("save_master", "Maître Épargnant", "Économisez 10,000 FCFA", "💰", 200, 50, AchievementCategory.SAVINGS),
        Achievement("tontine_leader", "Chef de Tontine", "Créez votre première tontine", "👑", 300, 75, AchievementCategory.COMMUNITY),
        Achievement("quiz_champion", "Champion du Savoir", "Réussissez 10 quiz d'éducation financière", "🧠", 150, 30, AchievementCategory.EDUCATION),
        Achievement("baobab_protector", "Protecteur du Baobab", "Utilisez l'app 30 jours consécutifs", "🌳", 500, 100, AchievementCategory.SPECIAL, true),
        Achievement("ubuntu_spirit", "Esprit Ubuntu", "Aidez 5 amis à rejoindre l'app", "🤝", 400, 80, AchievementCategory.COMMUNITY),
        Achievement("market_explorer", "Explorateur du Marché", "Achetez dans 5 catégories différentes", "🛒", 250, 60, AchievementCategory.TRANSACTIONS)
    )

    val userGameProfile = UserGameProfile(
        userId = "user1",
        currentLevel = 3,
        totalPoints = 2750,
        ecoCoins = 456,
        streak = 12,
        achievements = listOf("first_transaction", "save_master", "quiz_champion"),
        dailyMissionsCompleted = 8,
        referralCount = 3
    )

    val dailyMissions = listOf(
        DailyMission(
            id = "save_today",
            title = "Épargnez Aujourd'hui",
            description = "Transférez au moins 1,000 FCFA vers votre compte épargne",
            targetValue = 1000.0,
            currentProgress = 500.0,
            reward = MissionReward(100, 20),
            isCompleted = false,
            expiresAt = "2024-08-13T23:59:59"
        ),
        DailyMission(
            id = "learn_finance",
            title = "Apprenez Quelque Chose",
            description = "Terminez un module d'éducation financière",
            targetValue = 1.0,
            currentProgress = 0.0,
            reward = MissionReward(75, 15),
            isCompleted = false,
            expiresAt = "2024-08-13T23:59:59"
        ),
        DailyMission(
            id = "local_purchase",
            title = "Achat Local",
            description = "Effectuez un achat chez un marchand local",
            targetValue = 1.0,
            currentProgress = 1.0,
            reward = MissionReward(150, 30),
            isCompleted = true,
            expiresAt = "2024-08-13T23:59:59"
        )
    )

    val tontines = listOf(
        Tontine(
            id = "tontine1",
            name = "Tontine des Amis",
            description = "Épargne collective entre amis d'université",
            totalAmount = 500000.0,
            monthlyContribution = 50000.0,
            participants = listOf(
                TontineParticipant("user1", "John Doe", true, 150000.0, "2024-01-15"),
                TontineParticipant("user2", "Marie Kouadio", true, 150000.0, "2024-01-15"),
                TontineParticipant("user3", "Ahmed Diallo", false, 100000.0, "2024-02-01")
            ),
            currentRound = 3,
            totalRounds = 10,
            nextPayoutDate = "2024-09-01",
            status = TontineStatus.ACTIVE
        )
    )

    val marketplaceItems = listOf(
        MarketplaceItem(
            id = "item1",
            name = "Pagne Wax Premium",
            description = "Tissu wax authentique, motifs traditionnels",
            price = 15000.0,
            currency = "FCFA",
            category = "Mode & Textile",
            imageUrl = "https://example.com/pagne.jpg",
            seller = "Mama Adjoua",
            location = "Abidjan, Côte d'Ivoire",
            isLocal = true,
            discountPercent = 10
        ),
        MarketplaceItem(
            id = "item2",
            name = "Café Robusta Local",
            description = "Café premium de la région de Man",
            price = 8500.0,
            currency = "FCFA",
            category = "Alimentation",
            imageUrl = "https://example.com/cafe.jpg",
            seller = "Coopérative des Planteurs",
            location = "Man, Côte d'Ivoire",
            isLocal = true
        )
    )

    val educationalContent = listOf(
        EducationalContent(
            id = "basics1",
            title = "Qu'est-ce qu'un Budget ?",
            description = "Apprenez les bases de la gestion budgétaire",
            content = "Un budget est un plan qui vous aide à gérer votre argent...",
            category = EducationCategory.BASICS,
            difficulty = 1,
            estimatedTime = 5,
            points = 50,
            quizQuestions = listOf(
                QuizQuestion(
                    id = "q1",
                    question = "Quelle part de vos revenus devriez-vous épargner ?",
                    options = listOf("5%", "10-20%", "50%", "Rien"),
                    correctAnswer = 1,
                    explanation = "Les experts recommandent d'épargner 10-20% de vos revenus."
                )
            )
        )
    )

    val spendingInsights = listOf(
        SpendingInsight(
            category = "Alimentation",
            amount = 45000.0,
            percentage = 35.0,
            trend = "up",
            culturalTip = "Pensez aux marchés locaux pour économiser sur les légumes frais !"
        ),
        SpendingInsight(
            category = "Transport",
            amount = 25000.0,
            percentage = 20.0,
            trend = "stable",
            culturalTip = "Les taxis collectifs peuvent être plus économiques que les taxis individuels."
        )
    )
}