# 🏦 KotlinBank API - Fintech Africaine Gamifiée

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-2.3.4-orange.svg)](https://ktor.io)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **API bancaire moderne gamifiée spécialement conçue pour l'éducation financière en Afrique**

## 🌍 Vision

KotlinBank API est une solution fintech innovante qui combine **banking traditionnel** et **gamification** pour démocratiser l'éducation financière en Afrique. L'API propose des fonctionnalités culturellement adaptées comme les tontines, un marketplace local, et un système de récompenses pour encourager les bonnes habitudes financières.

## ✨ Fonctionnalités Principales

### 🏛️ **Core Banking**
- Gestion de comptes multiples (courant, épargne, business)
- Transactions sécurisées avec historique détaillé
- Cartes virtuelles avec contrôles avancés
- Notifications en temps réel

### 🎮 **Gamification**
- **Système de niveaux** : Du "Petit Épargnant" au "Diamant Ubuntu"
- **EcoCoins** : Monnaie virtuelle pour récompenser les bonnes actions
- **Achievements** culturels : "Baobab Protector", "Lion Investor", "Ubuntu Spirit"
- **Missions quotidiennes** avec rewards progressifs

### 🤝 **Fonctionnalités Africaines**
- **Tontines numériques** : Épargne collective modernisée
- **Marketplace local** : Produits et services africains authentiques
- **Insights culturels** : Conseils adaptés aux habitudes locales
- **Support multi-devises** : FCFA, USD, EUR et autres

### 📚 **Éducation Financière**
- **Modules interactifs** par niveau de difficulté
- **Quiz gamifiés** avec explications détaillées
- **Calculateurs financiers** adaptés au contexte africain
- **Conseils personnalisés** basés sur les dépenses

## 🚀 Installation & Démarrage

### **Prérequis**
- Java 17+ 
- Kotlin 1.9+
- Gradle 8+

### **Démarrage rapide**
```bash
# Cloner le repo
git clone https://github.com/username/kotlinbank-api.git
cd kotlinbank-api

# Installer les dépendances et démarrer
./gradlew clean build
./gradlew run

# L'API sera disponible sur http://localhost:8080
```

### **Vérification**
```bash
# Test de base
curl http://localhost:8080/

# Test endpoint gamification
curl http://localhost:8080/api/gamification/achievements
```

## 📡 Endpoints Disponibles

### 🏠 **Dashboard & Home**
```
GET  /api/dashboard                    # Dashboard utilisateur complet
GET  /api/dashboard/balance           # Soldes et statistiques
GET  /api/dashboard/recent-transactions # Transactions récentes
```

### 💳 **Wallet & Comptes**
```
GET  /api/wallet                      # Vue d'ensemble portefeuille
GET  /api/wallet/balance             # Détail des soldes
GET  /api/wallet/transactions        # Historique wallet
GET  /api/wallet/quick-actions       # Actions rapides
```

### 📊 **Transactions**
```
GET  /api/transactions               # Liste des transactions
GET  /api/transactions/{id}          # Détail d'une transaction
```

### 💳 **Cartes**
```
GET  /api/cards                      # Liste des cartes
GET  /api/cards/{id}                 # Détail d'une carte
```

### 🔔 **Notifications**
```
GET  /api/notifications              # Liste des notifications
GET  /api/notifications/{id}         # Détail d'une notification
```

### 👤 **Profil Utilisateur**
```
GET  /api/profile                    # Profil complet
GET  /api/profile/user              # Infos utilisateur
GET  /api/profile/stats             # Statistiques
GET  /api/profile/preferences       # Préférences
PUT  /api/profile/preferences       # Modifier préférences
```

### 🎮 **Gamification**
```
GET  /api/gamification/profile       # Profil gaming
GET  /api/gamification/levels        # Système de niveaux
GET  /api/gamification/achievements  # Liste des achievements
GET  /api/gamification/daily-missions # Missions quotidiennes
POST /api/gamification/complete-mission/{id} # Compléter mission
```

### 🤝 **Tontines (Épargne Collective)**
```
GET  /api/tontines                   # Liste des tontines
GET  /api/tontines/{id}             # Détail d'une tontine
```

### 🛒 **Marketplace Local**
```
GET  /api/marketplace/items          # Produits disponibles
GET  /api/marketplace/categories     # Catégories de produits
```

### 📚 **Éducation Financière**
```
GET  /api/education/content          # Modules éducatifs
GET  /api/education/insights         # Analyses et conseils
```

## 🏗️ Architecture

```
src/main/kotlin/com/kotlinbank/
├── Application.kt              # Point d'entrée serveur
├── models/                     # Data classes & enums
│   ├── TransactionDetail.kt
│   ├── BankNotification.kt
│   ├── UserGameProfile.kt
│   └── ...
├── data/                       # Couche données
│   └── MockData.kt            # Données de simulation
└── plugins/                    # Configuration modulaire
    ├── Routing.kt             # Définition endpoints
    ├── Serialization.kt       # Configuration JSON
    └── CORS.kt               # Support Android
```

## 🎯 Modèles de Données Clés

### **Profil Gaming**
```kotlin
data class UserGameProfile(
    val currentLevel: Int,
    val totalPoints: Int,
    val ecoCoins: Int,
    val streak: Int,
    val achievements: List<String>
)
```

### **Tontine Africaine**
```kotlin
data class Tontine(
    val name: String,
    val totalAmount: Double,
    val monthlyContribution: Double,
    val participants: List<TontineParticipant>,
    val nextPayoutDate: String
)
```

### **Achievement Culturel**
```kotlin
data class Achievement(
    val title: String,
    val description: String,
    val points: Int,
    val ecoCoins: Int,
    val category: AchievementCategory
)
```

## 🌍 Spécificités Africaines

### **Niveaux Culturels**
- 🌱 **Petit Épargnant** (0 points)
- 🌳 **Baobab Bronze** (500 points)  
- 🦁 **Lion Argent** (1500 points)
- 🦅 **Aigle Or** (3000 points)
- 💎 **Diamant Ubuntu** (6000 points)

### **Achievements Thématiques**
- 🌳 **Protecteur du Baobab** : 30 jours consécutifs
- 🤝 **Esprit Ubuntu** : Parrainer 5 amis
- 👑 **Chef de Tontine** : Créer sa première tontine
- 🛒 **Explorateur du Marché** : Acheter dans 5 catégories

### **Marketplace Local**
- Produits authentiques africains (pagnes wax, café local)
- Support des devises locales (FCFA, etc.)
- Promotion des artisans et producteurs locaux

## 🔧 Configuration

### **CORS pour Android**
```kotlin
install(CORS) {
    allowHost("10.0.2.2:8080")  // Émulateur
    allowHost("localhost:8080")  
    anyHost() // Développement uniquement
}
```

### **Serialization JSON**
```kotlin
install(ContentNegotiation) {
    gson {
        setPrettyPrinting()
        setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }
}
```

## 📱 Intégration Android

### **Base URL**
```kotlin
// Émulateur Android
private const val BASE_URL = "http://10.0.2.2:8080/"

// Appareil physique
private const val BASE_URL = "http://YOUR_IP:8080/"
```

### **Exemple d'appel**
```kotlin
interface ApiService {
    @GET("api/gamification/achievements")
    suspend fun getAchievements(): Response<List<Achievement>>
}
```

## 🚦 Statuts de Réponse

| Code | Signification | Exemple |
|------|---------------|---------|
| 200  | Succès | Données retournées |
| 400  | Requête invalide | Paramètre manquant |
| 404  | Ressource introuvable | Transaction inexistante |
| 500  | Erreur serveur | Erreur interne |

## 🧪 Tests

### **Tests manuels**
```bash
# Navigateur (GET seulement)
http://localhost:8080/api/dashboard

# Curl (tous types)
curl -X GET http://localhost:8080/api/gamification/profile
curl -X POST http://localhost:8080/api/gamification/complete-mission/save_today
```

### **Validation JSON**
Utilise [JSONLint](https://jsonlint.com/) pour valider les réponses

## 🔮 Roadmap

### **Phase 1** ✅
- [x] Core banking endpoints
- [x] Système de gamification
- [x] Tontines et marketplace
- [x] Éducation financière

### **Phase 2** 🚧
- [ ] Authentification JWT
- [ ] Base de données PostgreSQL
- [ ] Notifications push
- [ ] Tests automatisés

### **Phase 3** 📋
- [ ] Machine Learning pour conseils
- [ ] Blockchain pour tontines
- [ ] API externe (taux de change)
- [ ] Docker deployment

## 🤝 Contribution

1. **Fork** le projet
2. **Créer** une branche feature (`git checkout -b feature/amazing-feature`)
3. **Commit** tes changements (`git commit -m 'Add amazing feature'`)
4. **Push** vers la branche (`git push origin feature/amazing-feature`)
5. **Créer** une Pull Request

## 📄 License

Ce projet est sous licence MIT. Voir [LICENSE](LICENSE) pour plus de détails.

## 👥 Équipe

- **Développeur Principal** : [Ton Nom](https://github.com/ClichyMercury)
- **Spécialiste Fintech Africaine** : Expertise locale
- **UX Designer** : Focus gamification

## 🙏 Remerciements

- **Communauté Kotlin** pour le support
- **Ktor Team** pour le framework fantastique  
- **Fintech africaine** pour l'inspiration
- **Ubuntu Philosophy** pour l'esprit communautaire

---

## 📞 Support

- **Documentation** : [Wiki du projet](https://gs-dot-dev.vercel.app/)
- **Issues** : [GitHub Issues]([issues-link](https://gs-dot-dev.vercel.app/))
- **Contact** : gael.sassan@softskiils.ci

**Fait avec ❤️ pour l'Afrique** 🌍

---

*KotlinBank API - Démocratiser l'éducation financière par la gamification*
