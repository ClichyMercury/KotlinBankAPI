# FinSim — Backend API

> Simulation de trading & éducation financière. Backend Ktor / Kotlin.

API REST pour une app mobile d'éducation financière gamifiée : portfolios fictifs, ordres d'achat simulés sur des actifs réels (crypto, plus tard actions et BRVM), prix de marché en temps réel via CoinGecko.

## Stack

| Composant | Choix |
|---|---|
| Framework | Ktor 2.3.4 (Netty) |
| Langage | Kotlin 1.9, Java 17 |
| ORM | Exposed DSL 0.44 + HikariCP |
| Base de données | PostgreSQL 16 |
| Cache | Redis 7 (Lettuce) |
| Migrations | Flyway |
| Auth | JWT (HMAC256) + BCrypt cost 12 |
| Market data | CoinGecko (free tier) |
| Tests | JUnit 5 + Ktor testApplication |

Architecture **layered** simple : `routes/` → `services/` → `db/repositories/` → `db/tables/`.

---

## État d'avancement (slice vertical — 7 jours, terminé)

### ✅ J1 — Infra & dépendances
- Docker Compose (Postgres 16 + Redis 7, healthchecks)
- Dockerfile multi-stage (gradle build → temurin jre)
- `AppConfig` lit env vars puis system properties (overridable pour tests)
- `DatabaseFactory` (HikariCP + Exposed + Flyway au boot)
- `RedisFactory` (Lettuce pool, shutdown propre)
- Migration de Gson → kotlinx.serialization
- Shutdown hook qui ferme DB, Redis, HTTP client, job

### ✅ J2 — Schéma BDD & repositories
- Migration `V1__initial_schema.sql` : `users`, `assets`, `portfolios`, `portfolio_assets`, `orders`, `ledger`
- Contraintes `CHECK`, FKs `ON DELETE CASCADE`, index composites sur lectures fréquentes
- Tous les montants en `NUMERIC(20,8)` (`BigDecimal` côté Kotlin — jamais Double)
- 6 Exposed Tables (DSL, pas DAO)
- 5 repositories (User, Asset, Portfolio, Order, Ledger) avec `newSuspendedTransaction`
- `AssetSeeder` : insère BTC/ETH/USDT/SOL/ADA avec leur `external_id` CoinGecko si table vide

### ✅ J3 — Auth JWT
- `PasswordHasher` (BCrypt cost 12)
- `JwtService` (HMAC256, issuer/audience/exp 1h)
- `AuthService.register` en transaction : crée user + portfolio (10 000$ fictifs) + ledger DEPOSIT atomiquement
- Validation inputs : email regex, pseudo `[a-zA-Z0-9_]{3,50}`, password ≥ 8
- Hiérarchie `DomainException` (Validation / Conflict / Unauthorized / NotFound)
- Plugin `StatusPages` mappe les exceptions → JSON typé `ErrorResponse`
- Plugin `Auth` (Ktor JWT bearer + challenge JSON 401)
- Routes `POST /api/v1/auth/register | login`, `GET /api/v1/auth/me 🔒`
- Extension `ApplicationCall.userId()` pour extraire l'UUID du token

### ✅ J4 — Market data CoinGecko
- `CoinGeckoClient` (Ktor client CIO, timeout 10s, UserAgent, support clé API optionnelle)
- `MarketDataService.refreshPrices()` : batch call CoinGecko `/coins/markets`, met à jour `last_price`
- `PriceRefreshJob` : coroutine SupervisorJob, ticker 30s, catch les erreurs sans crasher
- Routes `GET /api/v1/market/assets` (filtrable `?type=CRYPTO`) + `/assets/{uuid}`
- DTOs avec serializers custom (UUID String, BigDecimal String, Instant ISO)

### ✅ J5 — Portfolio & Order BUY
- `PortfolioService.getPortfolio` : balance + assets valorisés au `last_price` + PnL non réalisé par asset + total
- `OrderService.placeBuyOrder` en transaction unique :
  1. Vérifie solde ≥ `quantity × current_price`
  2. Crée order EXECUTED avec snapshot du prix
  3. Upsert `portfolio_asset` (recalcul `avg_buy_price` pondéré)
  4. Décrémente `portfolio.balance_fictif`
  5. Insère ledger `BUY` (montant négatif, `balance_after` consistant)
- Routes `GET /portfolio 🔒`, `POST /orders/buy 🔒`, `GET /orders 🔒`
- Précision : `MathContext` géré (scale 2 pour money, 8 pour quantité/prix, `RoundingMode.HALF_UP`)

### ✅ J6 — Test E2E
- `EndToEndFlowTest` (JUnit 5 + Ktor `testApplication`)
- Couvre : register → login → /me → /portfolio vide → market/assets → buy 0.1 BTC → /portfolio après → /orders → insufficient funds → unauthorized
- **Limitation** : tourne sur la DB dev (port 5434), pas Testcontainers (cf. dette technique)
- Run en ~1 seconde

### ✅ J7 — Polish
- **Rate limit** Ktor 10 req/min/IP sur `/auth/login` + `/auth/register` (anti-bruteforce → 429)
- **CallLogging** sur `/api/*` + `/health` (`POST /api/v1/auth/login -> 401`)
- **CORS** : `anyHost()` en dev, allowlist explicite en prod (`ENVIRONMENT=production`)
- **Email normalisé** `lowercase()` en register et login (évite doublons `Foo@x` / `foo@x`)
- **`/health`** retourne `HealthResponse` typé (`{status, db, redis}`)
- **README** réécrit pour FinSim

---

## Démarrage

> Première fois ? Lire **[SETUP.md](SETUP.md)** — guide complet d'installation (JDK, Docker, IntelliJ, psql, workflows dev, troubleshooting).

### Prérequis
- Docker Desktop
- JDK 17

### Lancer la stack
```bash
# 1. Postgres (5434) + Redis (6380)
docker compose up -d

# 2. API sur :8080
./gradlew run
```

Au boot : Flyway migre, seeder insère 5 cryptos, job refresh CoinGecko démarre (toutes les 30s).

### Variables d'env (defaults dev OK)

```bash
PORT=8080
ENVIRONMENT=development              # production -> CORS strict
DATABASE_URL=jdbc:postgresql://localhost:5434/finsim
DATABASE_USER=finsim
DATABASE_PASSWORD=finsim_dev
REDIS_URL=redis://localhost:6380
JWT_SECRET=...                       # OBLIGATOIRE en prod
JWT_ISSUER=finsim-api
JWT_AUDIENCE=finsim-clients
JWT_EXPIRATION_MINUTES=60
COINGECKO_API_KEY=                   # optionnel
```

Voir `.env.example`.

### Note ports (macOS)

Les ports Docker sont décalés (**5434** Postgres, **6380** Redis) pour ne pas entrer en conflit avec un éventuel Postgres/Redis natif (Postgres.app, brew).

---

## Endpoints

Tous préfixés `/api/v1`. 🔒 = `Authorization: Bearer <token>`.

### Auth (rate limited : 10 req/min/IP)
```
POST /api/v1/auth/register     { email, pseudo, password }
POST /api/v1/auth/login        { email, password }
GET  /api/v1/auth/me           🔒
```

### Market
```
GET  /api/v1/market/assets                       (?type=CRYPTO|STOCK|FOREX)
GET  /api/v1/market/assets/{uuid}
GET  /api/v1/market/assets/{uuid}/candles        ?days=1|7|30|365   (OHLC, cache Redis)
```

### Portfolio & Orders
```
GET  /api/v1/portfolio         🔒
POST /api/v1/orders/buy        🔒  { assetId, quantity }
GET  /api/v1/orders            🔒
```

### Système
```
GET /              accueil
GET /health        { status, db, redis }
```

---

## Modèle BDD

```
users           id, email UNIQUE, pseudo UNIQUE, password_hash, is_active, created_at
assets          id, ticker UNIQUE, name, type, market, external_id, last_price, last_price_updated_at
portfolios      id, user_id UNIQUE→users, balance_fictif NUMERIC(20,2) DEFAULT 10000
portfolio_assets id, portfolio_id→, asset_id→, quantity, avg_buy_price, UNIQUE(portfolio_id, asset_id)
orders          id, user_id→, asset_id→, type, quantity, price, status, created_at, executed_at
ledger          id, user_id→, type DEPOSIT|BUY|SELL|FEE, amount (signé), order_id, balance_after, created_at
                  INDEX(user_id, created_at DESC)  -- audit/append-only
```

Migrations dans `src/main/resources/db/migration/`.

---

## Tests

```bash
./gradlew test
```

> **Attention** : ne pas lancer `./gradlew run` et `./gradlew test` en parallèle (cf. dette technique).

---

## Dette technique connue

À adresser dans les sprints suivants ou quand l'environnement le permet.

| # | Sujet | Détail | Sévérité |
|---|---|---|---|
| 1 | **Testcontainers KO** | Docker Desktop 29.4.2 a une régression : son daemon renvoie un body vide + label "redirect" cassé sur `/info`, que Testcontainers 1.19 et 1.20 ne savent pas suivre. Test E2E utilise la DB dev en attendant. Fix attendu : Docker Desktop 30.x ou CI Linux. | Moyenne |
| 2 | **Asymétrie scale BigDecimal** | Le débit ledger est arrondi à 2 décimales, mais `currentValue` du portfolio garde la précision 8 → micro-écart (~0.0005$ par achat) entre `totalValue` et `balanceFictif + assetsValue` recalculé manuellement. Cosmétique mais visible. | Faible |
| 3 | **Pas de refresh token JWT** | Token 1h expire et il faut se relogger. OK pour MVP, à ajouter dès qu'on a un client mobile sérieux. | Moyenne |
| 4 | **Order SELL absent** | Seul BUY implémenté. SELL nécessite gestion du PnL réalisé et update du ledger en sens inverse. | Haute |
| 5 | **Pas de tests unitaires** | Seul un test E2E. Les services (`AuthService`, `OrderService`, `PortfolioService`) gagneraient des tests isolés avec mocks. | Moyenne |
| 6 | **Pas de CI/CD** | `./gradlew test` doit être lancé manuellement. À mettre dans GitHub Actions avec build + test à chaque push. | Moyenne |
| 7 | **RedisFactory.init() pas idempotent** | Si appelé 2× (cas tests multi-classes plus tard), la première connexion fuit. À ajouter un guard. | Faible |
| 8 | **Pas de gestion d'erreur CoinGecko persistante** | Si l'API CoinGecko tombe 30 min, `last_price` devient stale mais l'API continue à servir l'ancien prix sans warning. Ajouter un seuil "stale price" qui rejette les BUY. | Moyenne |
| 9 | **Pas de versioning de breaking changes** | Le `/v1` est en place mais on n'a pas de mécanique pour faire vivre `/v2`. À designer quand on aura besoin. | Faible |
| 10 | **Logs en plain text** | `logback.xml` fait du `🏦 HH:mm:ss [thread] LEVEL ...` lisible mais pas indexable. À passer en JSON quand on aura un agrégateur (Loki, Datadog…). | Faible |
| 11 | **Pas de monitoring** | Aucun endpoint `/metrics`, pas de Sentry/Bugsnag pour les erreurs. À ajouter avant la prod. | Haute (pour prod) |
| 12 | **Secret JWT en env var simple** | Suffit en dev/Railway, mais à passer dans un secret manager dédié quand on grossit. | Moyenne |

---

## Roadmap

### 🚧 Sprint 2 (estimation : 1 semaine)
- [ ] **Order SELL** + recalcul PnL réalisé + ledger entry SELL positif
- [ ] **Refresh token JWT** + `POST /auth/refresh` + `POST /auth/logout` (invalidate refresh en Redis)
- [ ] **Email verification** : générer un code, envoyer (mock SMTP au début), endpoint `/auth/verify`
- [ ] **Reset password** : `POST /auth/forgot` + `/auth/reset`
- [ ] **Tests unitaires** services (AuthService, OrderService, PortfolioService) avec MockK
- [ ] **Seuil "stale price"** : rejeter un BUY si `last_price_updated_at` > 5 min

### 🚧 Sprint 3 (estimation : 1-2 semaines)
- [ ] **WebSocket** `/ws/market/prices` : streaming temps réel des prix via SharedFlow Kotlin
- [ ] **Auth WebSocket** (token en sub-protocole, pas en query param)
- [ ] **Rate limit WebSocket** : max 100 connexions / instance
- [ ] **Gamification** : tables `user_xp`, `badges`, `user_badges`, `challenges`
- [ ] Service de calcul XP (à chaque BUY/SELL exécuté)
- [ ] Endpoints `/gamification/profile|leaderboard|badges|challenges`

### 🚧 Sprint 4 (estimation : 1-2 semaines)
- [ ] **Provider Alpha Vantage** pour actions internationales (gérer 5 req/min freemium → batch + cache plus agressif)
- [ ] **Provider Yahoo Finance** en fallback (via lib non-officielle)
- [ ] **BRVM** : scraping ou RSS pour les actions UEMOA (à valider juridiquement)
- [ ] **Multi-devises** : taux de change FCFA/USD/EUR via Open Exchange Rates
- [ ] **CI GitHub Actions** : build + test à chaque push, déploiement auto sur Railway/Render à chaque merge `main`
- [ ] **Tests d'intégration** Testcontainers (si Docker Desktop est fixé)

### 🚧 Sprint 5 — Polish prod (estimation : 1 semaine)
- [ ] **Monitoring** : Sentry pour erreurs + Prometheus `/metrics`
- [ ] **Logs JSON structurés** (logback)
- [ ] **Backup PostgreSQL** automatique (Railway gère, sinon cron `pg_dump`)
- [ ] **Cache Redis sur reads** : `/market/assets` cacheable 30s
- [ ] **Disclaimer légal** dans toutes les réponses portfolio/order ("simulation pédagogique, pas un conseil")
- [ ] **RGPD** : suppression compte (`DELETE /auth/me`), export données (`GET /auth/me/export`)
- [ ] **i18n** : contenu pédagogique en FR/EN

### 📋 Plus loin
- [ ] Contenu pédagogique (modules + quiz + calculateurs)
- [ ] Tontines numériques (cf. plan PDF original)
- [ ] Marketplace local UEMOA
- [ ] Machine learning : conseils personnalisés sur les habitudes de dépense
- [ ] Notifications push (FCM)

---

## Layout

```
src/main/kotlin/com/kotlinbank/
├── Application.kt              boot + shutdown hooks + jobs
├── config/
│   ├── AppConfig.kt            env + system properties
│   ├── DatabaseFactory.kt      HikariCP + Exposed + Flyway
│   └── RedisFactory.kt         Lettuce
├── plugins/
│   ├── Auth.kt                 JWT bearer
│   ├── CallLogging.kt          HTTP request/response logs
│   ├── CORS.kt                 par environnement
│   ├── RateLimit.kt            par IP sur /auth
│   ├── Routing.kt              orchestrateur
│   ├── Serialization.kt        kotlinx.serialization JSON
│   └── StatusPages.kt          mapping exceptions → JSON
├── routes/
│   ├── AuthRoutes.kt
│   ├── MarketRoutes.kt
│   ├── PortfolioRoutes.kt
│   └── OrderRoutes.kt
├── services/
│   ├── AuthService.kt
│   ├── JwtService.kt
│   ├── PasswordHasher.kt
│   ├── PortfolioService.kt
│   ├── OrderService.kt
│   ├── Exceptions.kt
│   └── market/
│       ├── CoinGeckoClient.kt
│       ├── MarketDataService.kt
│       └── PriceRefreshJob.kt
├── db/
│   ├── AssetSeeder.kt
│   ├── tables/                 6 Exposed Tables DSL
│   └── repositories/           5 repos avec newSuspendedTransaction
└── models/
    ├── User / Asset / Portfolio / Order / LedgerEntry
    └── dto/
        ├── Serializers.kt      UUID, Instant, BigDecimal
        ├── AuthDto.kt
        ├── MarketDto.kt
        ├── PortfolioDto.kt
        ├── OrderDto.kt
        └── HealthDto.kt

src/test/kotlin/com/kotlinbank/
└── EndToEndFlowTest.kt         register → buy → portfolio → orders
```

---

## License & contact

Gael Sassan — WHARPE Corp.
