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

## Ajouts post-slice (en cours)

- ✅ **Order SELL** — `POST /api/v1/orders/sell` : vente en transaction unique, **PnL réalisé**
  (`realizedPnl`), ledger `SELL` positif, suppression de la position si soldée. (PR #1 — clôt la dette #4)
- ✅ **Candles OHLC** — `GET /api/v1/market/assets/{id}/candles?days=...` avec cache Redis
- ✅ **Outillage dev** — scripts de lancement `start.sh` / `start.ps1`, collection Postman
  versionnée (`FinSim.postman_collection.json`), docs d'intégration mobile (`docs/MOBILE_ORDERS.md`,
  `docs/MOBILE_AUTH.md`)

- ✅ **Fail-fast config prod** — l'API refuse de démarrer en `ENVIRONMENT=production` si `JWT_SECRET`
  est absent/trop court, si `DATABASE_PASSWORD` est celui de dev, ou si `DATABASE_URL` n'est pas une
  URL JDBC. Évite de tourner en prod avec le secret de dev publié dans le repo.
- ✅ **Reset password** — `POST /auth/forgot-password` / `/auth/reset-password` : token opaque hashé,
  usage unique, TTL 30 min, réponse générique (pas d'énumération d'emails), révoque toutes les
  sessions après changement.
- ✅ **Sentry** — exceptions non gérées (avec méthode, chemin **sans query string**, `user.id` si
  authentifié), échecs d'envoi Resend, et alerte après 3 échecs consécutifs du refresh CoinGecko.
  Sans `SENTRY_DSN`, tout est no-op.
- ✅ **Page de reset servie par l'API** — `GET /reset-password?token=…`, HTML autonome, cible de
  `PASSWORD_RESET_URL` en attendant le front web.
- ✅ **Emails via Resend** — `ResendMailSender` (template FR HTML + texte) ; `MAIL_PROVIDER=log`
  en dev écrit le token dans les logs. Setup DNS/clé : [Configurer Resend](#configurer-resend-emails-de-reset-password).
- ✅ **Refresh token** — `POST /auth/refresh` / `/auth/logout` / `/auth/logout-all` : tokens opaques
  (32 bytes aléatoires) stockés hashés SHA-256, rotation à usage unique, détection de réutilisation
  qui révoque toutes les sessions. (clôt la dette #3)

**Prochaines pistes** (cf. [Roadmap](#roadmap) ci-dessous) : seuil *stale price* sur les ordres
(dette #8), tests unitaires services avec MockK (dette #5).

---

## Démarrage

> Première fois ? Lire **[SETUP.md](SETUP.md)** — guide complet d'installation (JDK, Docker, IntelliJ, psql, workflows dev, troubleshooting).
>
> Intégration côté app mobile : auth & refresh token → **[docs/MOBILE_AUTH.md](docs/MOBILE_AUTH.md)**,
> ordres BUY/SELL → **[docs/MOBILE_ORDERS.md](docs/MOBILE_ORDERS.md)**.
>
> Construire le front web → **[docs/FRONTEND_BRIEF.md](docs/FRONTEND_BRIEF.md)** (contrat d'API,
> palette, déploiement Dokploy sur `finsim.wharpe.com`).

### Prérequis
- Docker Desktop
- JDK 17

### Lancer la stack

Tout-en-un (Docker + healthcheck + API) :
```bash
./start.sh        # macOS / Linux
.\start.ps1       # Windows (PowerShell)
```

Ou à la main :
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
JWT_REFRESH_EXPIRATION_DAYS=30
PASSWORD_RESET_EXPIRATION_MINUTES=30
MAIL_PROVIDER=log                    # log (dev) | resend (obligatoire en prod)
RESEND_API_KEY=                      # re_... si MAIL_PROVIDER=resend
MAIL_FROM=FinSim <no-reply@tondomaine.com>
PASSWORD_RESET_URL=https://api.tondomaine.com/reset-password?token={token}
CORS_ALLOWED_HOSTS=                  # prod : hosts web autorisés, séparés par des virgules
TRUST_PROXY_HEADERS=                 # défaut : true en production, false ailleurs
SENTRY_DSN=                          # vide = reporting désactivé (warning au boot en prod)
SENTRY_RELEASE=                      # optionnel, ex. le SHA du commit déployé
SENTRY_TRACES_SAMPLE_RATE=0.0        # 0.0 = pas de tracing, seulement les erreurs
COINGECKO_API_KEY=                   # optionnel
```

Voir `.env.example`.

**`TRUST_PROXY_HEADERS`** doit rester à `true` derrière un reverse proxy (Traefik, nginx, Dokploy,
Railway…). Sans ça l'API croit que toutes les requêtes arrivent en `http` depuis l'IP du proxy,
ce qui a deux conséquences graves : CORS rejette en 403 les requêtes de nos propres pages, et le
rate limit compte **tous les utilisateurs comme un seul client**. À l'inverse, si l'API est un
jour exposée directement à Internet, mettre `false` — sinon n'importe qui peut usurper son IP
via `X-Forwarded-For` et contourner le rate limit.

**`CORS_ALLOWED_HOSTS`** ne sert qu'aux navigateurs : un client mobile natif n'envoie pas
d'en-tête `Origin`, donc laisser la variable vide est le bon réglage tant qu'il n'y a pas de
front web. Format : hosts nus séparés par des virgules (`finsim.wharpe.com,app.wharpe.com`),
sans schéma ni slash final — la validation de prod refuse le boot sinon. En dev, toutes les
origines sont acceptées.

Le host de `PASSWORD_RESET_URL` est **ajouté automatiquement** à la liste : la page de reset que
l'API sert elle-même ne doit jamais être bloquée par nos propres règles CORS.

### Configurer Resend (emails de reset password)

Nécessaire uniquement pour la prod — en dev, `MAIL_PROVIDER=log` écrit le token dans les logs.

1. Créer un compte sur [resend.com](https://resend.com) (gratuit : 3 000 emails/mois, 100/jour)
2. **Domains → Add Domain**, saisir le domaine d'envoi (ex. `finsim.app`)
3. Ajouter chez le registrar les enregistrements DNS affichés (MX + TXT SPF, TXT DKIM).
   La vérification prend de quelques minutes à quelques heures.
4. **API Keys → Create API Key** (permission *Sending access*), copier la clé `re_...`
5. Renseigner les variables :

```bash
MAIL_PROVIDER=resend
RESEND_API_KEY=re_...
MAIL_FROM=FinSim <no-reply@finsim.app>      # doit être sur le domaine vérifié
PASSWORD_RESET_URL=https://api.tondomaine.com/reset-password?token={token}
```

`PASSWORD_RESET_URL` est le lien cliqué dans l'email. Tant qu'il n'y a pas de front web,
pointez-le sur la page servie par l'API elle-même :
`https://api.tondomaine.com/reset-password?token={token}`. Le placeholder `{token}` est
obligatoire.

> Éviter un deep link à schéma personnalisé (`finsim://…`) dans un email : Gmail et Outlook
> le dépouillent ou le rendent non cliquable. Pour ouvrir l'app depuis le lien, il faudra
> passer par des **App Links** (Android) / **Universal Links** (iOS), qui restent des URL
> `https://` et demandent de servir `/.well-known/assetlinks.json` et
> `/.well-known/apple-app-site-association` sur le domaine du lien, sans redirection.
> Le jour venu, ces fichiers se posent sur le domaine de l'API et `PASSWORD_RESET_URL` ne
> change pas.

> Tant que le domaine n'est pas vérifié, Resend n'accepte que l'expéditeur bac à sable
> `onboarding@resend.dev`, qui ne délivre qu'à l'adresse du compte Resend. La validation de
> config refuse ce cas en production, justement pour ne pas déployer un reset qui n'arrive
> à personne.

### Déployer

L'image est construite par le `Dockerfile` à la racine (multi-stage, JRE 17, `EXPOSE 8080`).
Au boot, l'API valide sa config (voir plus bas), Flyway applique les migrations, le seeder
insère les assets manquants et le job de prix démarre.

Variables minimales en production :

```bash
ENVIRONMENT=production
JWT_SECRET=<32+ caractères aléatoires>
DATABASE_URL=jdbc:postgresql://<host>:5432/finsim
DATABASE_USER=finsim
DATABASE_PASSWORD=<mot de passe réel>
REDIS_URL=redis://<host>:6379
MAIL_PROVIDER=resend
RESEND_API_KEY=re_...
MAIL_FROM=FinSim <no-reply@tondomaine.com>
PASSWORD_RESET_URL=https://api.tondomaine.com/reset-password?token={token}
CORS_ALLOWED_HOSTS=                  # vide tant qu'il n'y a pas de front web
SENTRY_DSN=https://...@....ingest.sentry.io/...
```

L'API **refuse de démarrer** si l'une de ces valeurs est absente ou incohérente, et liste tous
les problèmes d'un coup. C'est volontaire : mieux vaut un déploiement qui échoue bruyamment
qu'une API en ligne avec le secret de dev ou un reset password qui n'arrive à personne.

⚠️ **Une seule instance.** `PriceRefreshJob` tourne dans chaque process : deux replicas doublent
les appels CoinGecko et déclenchent des 429.

⚠️ **`DATABASE_URL` doit être une URL JDBC.** Les PaaS fournissent en général
`postgres://user:pass@host/db` — à convertir en `jdbc:postgresql://host:5432/db` avec
`DATABASE_USER` / `DATABASE_PASSWORD` séparés.

### Monitoring (Sentry)

`GET /health` expose l'état du reporting à côté de `db` et `redis` :

```json
{ "status": "ok", "db": true, "redis": true, "sentry": true }
```

`sentry: false` signifie qu'aucun DSN n'est configuré. Ce champ **n'entre pas** dans le calcul
de `status` : perdre l'observabilité ne doit pas faire passer l'instance en `degraded` et
déclencher un redémarrage par l'orchestrateur.

`SENTRY_DSN` vide = tout est no-op, rien n'est envoyé. En production, l'absence de DSN produit
un warning au boot mais ne bloque pas le démarrage — contrairement aux erreurs de config
sensibles, une panne d'observabilité ne justifie pas de refuser de servir.

Ce qui remonte :

| Événement | Niveau | Pourquoi |
|---|---|---|
| Exception non gérée (`StatusPages`) | error | méthode, chemin, `user.id` si authentifié |
| Échec d'envoi Resend | error | l'utilisateur ne reçoit rien et la réponse HTTP reste générique — sans ça, invisible |
| 3 échecs consécutifs du refresh CoinGecko | error | les prix deviennent stale sans que rien ne le signale |

⚠️ **Le chemin est envoyé sans query string.** L'URL `/reset-password?token=...` porte un token
de réinitialisation : `SentryReporter.scrubUrl()` le retire avant tout envoi, et un test le
vérifie. Même raison pour `isSendDefaultPii = false`.

### Note ports (macOS)

Les ports Docker sont décalés (**5434** Postgres, **6380** Redis) pour ne pas entrer en conflit avec un éventuel Postgres/Redis natif (Postgres.app, brew).

---

## Endpoints

Tous préfixés `/api/v1`. 🔒 = `Authorization: Bearer <token>`.

### Auth (rate limited : 10 req/min/IP)
```
POST /api/v1/auth/register     { email, pseudo, password }
POST /api/v1/auth/login        { email, password }
POST /api/v1/auth/refresh      { refreshToken }          -> nouvelle paire (rotation)
POST /api/v1/auth/forgot-password  { email }            -> 200 générique (pas d'énumération)
POST /api/v1/auth/reset-password   { token, newPassword }
POST /api/v1/auth/logout       { refreshToken }          -> révoque ce refresh token
GET  /api/v1/auth/me           🔒
POST /api/v1/auth/logout-all   🔒                        -> révoque toutes les sessions
```

`register` / `login` / `refresh` renvoient `accessToken` (JWT 1h) **et** `refreshToken`
(opaque, 30j). Le refresh token est **à usage unique** : chaque appel à `/refresh` le révoque
et en émet un nouveau. Rejouer un token déjà consommé = fuite présumée → **toutes** les
sessions de l'utilisateur sont révoquées (401 `Refresh token reuse detected`).

Le reset password consomme un token à usage unique valable 30 min, puis **révoque toutes les
sessions** de l'utilisateur. L'email part via **Resend** quand `MAIL_PROVIDER=resend` ; en dev
(`MAIL_PROVIDER=log`, défaut) le token est simplement écrit dans les logs de l'API.

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
POST /api/v1/orders/sell       🔒  { assetId, quantity }   -> renvoie realizedPnl
GET  /api/v1/orders            🔒
```

### Pages web servies par l'API
```
GET  /reset-password?token=...   page de saisie du nouveau mot de passe
```

Page HTML autonome (aucune dépendance, aucun build front) servie depuis
`src/main/resources/web/`. C'est la cible de `PASSWORD_RESET_URL` tant qu'il n'y a pas de
front web : le lien de l'email fonctionne sur tous les appareils, avec ou sans l'app installée.
Le token n'est jamais injecté côté serveur — la page le lit dans `location.search`.

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
refresh_tokens  id, user_id→, token_hash CHAR(64) UNIQUE (SHA-256), expires_at, revoked_at, created_at
                  INDEX(user_id), INDEX(expires_at)  -- valeur brute jamais stockée
password_reset_tokens
                id, user_id→, token_hash CHAR(64) UNIQUE (SHA-256), expires_at, used_at, created_at
                  INDEX(user_id), INDEX(expires_at)  -- usage unique, TTL 30 min
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
| ~~3~~ | ~~**Pas de refresh token JWT**~~ | ✅ Fait : `POST /auth/refresh` (rotation + détection de réutilisation), `/auth/logout`, `/auth/logout-all`. Stockage en Postgres (table `refresh_tokens`, hash SHA-256) et **pas en Redis** comme prévu initialement : une session ne doit pas disparaître au redémarrage de Redis, et la révocation gagne à être auditable. Tradeoff assumé : un aller-retour DB par refresh (rare, non critique). | ~~Moyenne~~ |
| ~~4~~ | ~~**Order SELL absent**~~ | ✅ Fait : `POST /orders/sell` (transaction unique, PnL réalisé, ledger SELL positif, suppression de la position si soldée). | ~~Haute~~ |
| 5 | **Peu de tests unitaires** | 1 test unitaire (`AppConfigValidationTest`) + 3 tests E2E. Les services (`AuthService`, `OrderService`, `PortfolioService`) gagneraient des tests isolés avec mocks. | Moyenne |
| 6 | **Pas de CI/CD** | `./gradlew test` doit être lancé manuellement. À mettre dans GitHub Actions avec build + test à chaque push. | Moyenne |
| 7 | **RedisFactory.init() pas idempotent** | Si appelé 2× (cas tests multi-classes plus tard), la première connexion fuit. À ajouter un guard. | Faible |
| 8 | **Pas de gestion d'erreur CoinGecko persistante** | Si l'API CoinGecko tombe 30 min, `last_price` devient stale mais l'API continue à servir l'ancien prix sans warning. Ajouter un seuil "stale price" qui rejette les BUY. | Moyenne |
| 9 | **Pas de versioning de breaking changes** | Le `/v1` est en place mais on n'a pas de mécanique pour faire vivre `/v2`. À designer quand on aura besoin. | Faible |
| 10 | **Logs en plain text** | `logback.xml` fait du `🏦 HH:mm:ss [thread] LEVEL ...` lisible mais pas indexable. À passer en JSON quand on aura un agrégateur (Loki, Datadog…). | Faible |
| ~~11~~ | ~~**Pas de monitoring**~~ | ✅ Partiellement : Sentry capture les exceptions non gérées, les échecs d'envoi Resend et les pannes prolongées de CoinGecko. Reste à faire : endpoint `/metrics` (Prometheus). | ~~Haute~~ → Faible |
| 12 | **Secret JWT en env var simple** | Suffit en dev/Railway, mais à passer dans un secret manager dédié quand on grossit. | Moyenne |
| 13 | **Purge des refresh + reset tokens au boot uniquement** | Les tokens expirés sont supprimés au démarrage de l'API. Sur une instance qui tourne des mois, la table grossit entre deux redémarrages. À passer en job périodique si le volume devient visible. | Faible |
| ~~14~~ | ~~**Aucun provider mail branché**~~ | ✅ Fait : `ResendMailSender` (API HTTP Resend, template FR HTML + texte). `MAIL_PROVIDER=log` reste le défaut en dev. La prod refuse de démarrer si le provider n'est pas `resend`, si la clé manque, ou si `MAIL_FROM` utilise encore le domaine bac à sable `resend.dev`. | ~~Haute~~ |
| 16 | **Rate limit à 10 req/min sur tout `/auth`** | Le quota couvre register, login, refresh, forgot et reset ensemble. Maintenant qu'il est bien par IP, 10/min reste serré pour un utilisateur qui se trompe de mot de passe puis demande un reset. À relever ou à découper par endpoint. | Moyenne |
| 15 | **Pas de retry sur l'envoi d'email** | Si Resend renvoie une erreur (domaine non vérifié, quota, panne), l'erreur est loggée et l'utilisateur ne reçoit rien — sans le savoir, puisque la réponse HTTP reste générique pour éviter l'énumération de comptes. Acceptable au démarrage, à doubler d'une file de retry + alerte quand le volume grimpe. | Moyenne |

---

## Roadmap

### 🚧 Sprint 2 (estimation : 1 semaine)
- [x] **Order SELL** + recalcul PnL réalisé + ledger entry SELL positif
- [x] **Refresh token JWT** + `POST /auth/refresh` + `POST /auth/logout` + `/auth/logout-all`
      (tokens opaques hashés en Postgres, rotation à usage unique, détection de réutilisation)
- [ ] **Email verification** : générer un code, envoyer (mock SMTP au début), endpoint `/auth/verify`
- [x] **Reset password** : `POST /auth/forgot-password` + `/auth/reset-password`
      (token usage unique 30 min, révoque toutes les sessions — reste à brancher un provider mail, dette #14)
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
│   ├── WebRoutes.kt            page /reset-password
│   ├── MarketRoutes.kt
│   ├── PortfolioRoutes.kt
│   └── OrderRoutes.kt
├── services/
│   ├── AuthService.kt
│   ├── JwtService.kt
│   ├── RefreshTokenService.kt
│   ├── PasswordResetService.kt
│   ├── PasswordHasher.kt
│   ├── PortfolioService.kt
│   ├── OrderService.kt
│   ├── Exceptions.kt
│   ├── monitoring/
│   │   └── SentryReporter.kt   init + captures, no-op sans DSN
│   ├── mail/
│   │   ├── MailSender.kt       interface + LogMailSender (dev)
│   │   └── ResendMailSender.kt API Resend (prod)
│   └── market/
│       ├── CoinGeckoClient.kt
│       ├── MarketDataService.kt
│       └── PriceRefreshJob.kt
├── db/
│   ├── AssetSeeder.kt
│   ├── tables/                 8 Exposed Tables DSL
│   └── repositories/           7 repos avec newSuspendedTransaction
└── models/
    ├── User / Asset / Portfolio / Order / LedgerEntry / RefreshToken / PasswordResetToken
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
