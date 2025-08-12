package com.kotlinbank.models

// Modèles existants...
data class TransactionDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,     // Format: "+$3,450.00" ou "-$15.99"
    val isIncome: Boolean,
    val date: String,       // "Today", "Yesterday", etc.
    val time: String,       // "2:30 PM"
    val category: TransactionCategory,
    val status: TransactionStatus = TransactionStatus.COMPLETED
)

enum class TransactionCategory(val displayName: String, val color: String) {
    FOOD("Food", "#FF9800"),
    TRANSPORT("Transport", "#2196F3"),
    SHOPPING("Shopping", "#9C27B0"),
    ENTERTAINMENT("Entertainment", "#E91E63"),
    SALARY("Salary", "#4CAF50"),
    TRANSFER("Transfer", "#00BCD4"),
    BILLS("Bills", "#F44336"),
    OTHER("Other", "#607D8B")
}

enum class TransactionStatus {
    COMPLETED, PENDING, FAILED
}

data class BankNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: String,  // "5 min ago"
    val isRead: Boolean = false,
    val isImportant: Boolean = false,
    val amount: String? = null,
    val actionRequired: Boolean = false
)

enum class NotificationType(val displayName: String, val color: String) {
    TRANSACTION("Transaction", "#2196F3"),
    SECURITY("Security", "#F44336"),
    PROMOTION("Promotion", "#4CAF50"),
    REMINDER("Reminder", "#FF9800"),
    SYSTEM("System", "#607D8B"),
    CARD("Card", "#9C27B0")
}

data class CardItem(
    val id: String,
    val cardType: String,
    val cardNumber: String,
    val cardName: String,
    val balance: Double,
    val gradientColors: List<String>  // ["#1E3A8A", "#3B82F6"]
)

// NOUVEAUX MODÈLES POUR HOME
data class HomeDashboard(
    val userName: String,
    val greeting: String,
    val totalBalance: String,
    val income: String,
    val expenses: String,
    val unreadNotifications: Int,
    val recentTransactions: List<RecentTransaction>,
    val quickActions: List<QuickAction>
)

data class RecentTransaction(
    val id: String,
    val title: String,
    val amount: String,
    val time: String,
    val isIncome: Boolean
)

data class QuickAction(
    val id: String,
    val title: String,
    val icon: String,
    val color: String,
    val route: String? = null
)

// MODÈLES WALLET
data class WalletData(
    val totalBalance: Double,
    val income: Double,
    val expenses: Double,
    val savings: Double,
    val transactions: List<WalletTransaction>,
    val quickActions: List<WalletQuickAction>
)

data class WalletTransaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val isIncome: Boolean,
    val time: String
)

data class WalletQuickAction(
    val id: String,
    val title: String,
    val icon: String,
    val backgroundColor: String
)

// MODÈLES PROFILE
data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val accountNumber: String,
    val memberSince: String,
    val accountType: String,
    val profileImageUrl: String? = null
)

data class ProfileData(
    val userProfile: UserProfile,
    val quickStats: ProfileQuickStats,
    val preferences: ProfilePreferences
)

data class ProfileQuickStats(
    val activeCards: Int,
    val accounts: Int,
    val totalTransactions: Int
)

data class ProfilePreferences(
    val notificationsEnabled: Boolean,
    val biometricEnabled: Boolean,
    val darkModeEnabled: Boolean
)

// MODÈLES GAMIFICATION
data class UserGameProfile(
    val userId: String,
    val currentLevel: Int,
    val totalPoints: Int,
    val ecoCoins: Int,
    val streak: Int, // Jours consécutifs d'utilisation
    val achievements: List<String>, // IDs des achievements débloqués
    val dailyMissionsCompleted: Int,
    val referralCount: Int
)

data class UserLevel(
    val level: Int,
    val name: String,
    val pointsRequired: Int,
    val benefits: List<String>,
    val badge: String,
    val color: String
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val points: Int,
    val ecoCoins: Int,
    val category: AchievementCategory,
    val isRare: Boolean = false
)

enum class AchievementCategory {
    SAVINGS, TRANSACTIONS, EDUCATION, COMMUNITY, SPECIAL
}

data class DailyMission(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Double,
    val currentProgress: Double,
    val reward: MissionReward,
    val isCompleted: Boolean,
    val expiresAt: String
)

data class MissionReward(
    val points: Int,
    val ecoCoins: Int,
    val specialReward: String? = null
)

// MODÈLES COMMUNAUTÉ AFRICAINE
data class Tontine(
    val id: String,
    val name: String,
    val description: String,
    val totalAmount: Double,
    val monthlyContribution: Double,
    val participants: List<TontineParticipant>,
    val currentRound: Int,
    val totalRounds: Int,
    val nextPayoutDate: String,
    val status: TontineStatus
)

data class TontineParticipant(
    val userId: String,
    val userName: String,
    val hasPaidThisMonth: Boolean,
    val totalContributed: Double,
    val joinDate: String
)

enum class TontineStatus {
    ACTIVE, COMPLETED, PENDING
}

data class MarketplaceItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String,
    val category: String,
    val imageUrl: String,
    val seller: String,
    val location: String,
    val isLocal: Boolean,
    val discountPercent: Int = 0
)

data class EducationalContent(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val category: EducationCategory,
    val difficulty: Int, // 1-5
    val estimatedTime: Int, // minutes
    val points: Int,
    val quizQuestions: List<QuizQuestion>
)

enum class EducationCategory {
    BASICS, SAVINGS, CREDIT, INVESTMENT, BUDGETING, ENTREPRENEURSHIP
}

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

// ANALYTICS CULTURELS
data class SpendingInsight(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val trend: String, // "up", "down", "stable"
    val culturalTip: String // Conseil adapté au contexte africain
)