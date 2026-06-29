# Prise en main — dev local FinSim

Guide complet pour passer d'une machine vide (ou presque) à un environnement de dev fonctionnel sur **macOS**.

> Linux : les noms d'outils sont identiques, seules les commandes d'installation changent (apt/dnf/pacman au lieu de brew). Le reste s'applique tel quel.

---

## 1. Outils à installer

| Outil | Pourquoi | Obligatoire ? |
|---|---|---|
| **JDK 17** | Compiler et lancer Kotlin/Ktor | ✅ |
| **Docker Desktop** | Postgres + Redis en conteneurs | ✅ |
| **Git** | Cloner le repo | ✅ |
| **IntelliJ IDEA** (Community suffit) | IDE Kotlin de référence | Recommandé |
| **psql** (client PostgreSQL) | Inspecter la BDD en CLI | Recommandé |
| **httpie** ou **jq** | Tester l'API et parser JSON dans le terminal | Optionnel |
| **Postman / Insomnia / Bruno** | Tester l'API avec une UI | Optionnel |

### Installation sur macOS (Homebrew)

Si Homebrew n'est pas installé : https://brew.sh

```bash
# JDK 17 (Eclipse Temurin)
brew install --cask temurin@17

# Docker Desktop
brew install --cask docker

# IntelliJ IDEA Community
brew install --cask intellij-idea-ce

# Client PostgreSQL (juste le binaire psql, pas le serveur)
brew install libpq
brew link --force libpq

# Outils JSON terminal
brew install jq httpie

# Bruno (alternative open-source à Postman, recommandée)
brew install --cask bruno
```

### Vérifier les versions

```bash
java -version          # doit afficher 17.x
docker --version       # >= 24
docker compose version # >= 2.x
git --version          # >= 2.30
psql --version         # >= 14 (le client suffit)
```

> Si `java -version` montre une autre version, c'est que `JAVA_HOME` pointe ailleurs. Sur Mac : `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` dans ton `~/.zshrc`. Ou utilise [jenv](https://www.jenv.be/) pour switcher entre versions.

### Démarrer Docker Desktop

```bash
open -a Docker
```

Attendre que la baleine soit verte dans la barre de menu (1-2 min au premier lancement). Sans ça, `docker compose up` échouera.

---

## 2. Cloner et lancer le projet

```bash
# 1. Cloner
git clone https://github.com/ClichyMercury/KotlinBankAPI.git
cd KotlinBankAPI

# 2. Lancer Postgres (port 5434) + Redis (port 6380)
docker compose up -d

# 3. Vérifier que les containers tournent et sont healthy
docker compose ps

# 4. Démarrer l'API sur :8080 (premier lancement = 1-2 min, gradle télécharge tout)
./gradlew run
```

> **Raccourci** : un script de démarrage fait les étapes 2 à 4 d'un coup (pose `JAVA_HOME` en 17, lance Docker, attend que les containers soient healthy, démarre l'API).
>
> ```bash
> ./start.sh        # macOS / Linux  (1re fois : chmod +x start.sh)
> ```
> ```powershell
> .\start.ps1       # Windows (PowerShell)
> ```
>
> `Ctrl+C` pour arrêter. Sous Windows, si « l'exécution de scripts est désactivée » :
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` (une seule fois).

Au premier boot, tu dois voir dans les logs :
- `HikariPool-1 - Start completed`
- `Flyway ... Successfully applied 1 migration`
- `AssetSeeder - Seeded 5 crypto assets`
- `MarketDataService - Refreshed 5/5 crypto prices from CoinGecko`
- `Responding at http://0.0.0.0:8080`

### Tester que tout marche

Dans un autre terminal :

```bash
# Health check
curl -s http://localhost:8080/health | jq
# {"status":"ok","db":true,"redis":true}

# Liste des cryptos avec leur prix
curl -s http://localhost:8080/api/v1/market/assets | jq '.[].ticker'

# Créer un compte
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"toi@test.io","pseudo":"toi","password":"motdepasse123"}' | jq
```

Si tout passe → environnement OK 🎉

---

## 3. Configuration IntelliJ IDEA

1. **Ouvrir** : `File → Open` puis sélectionner le dossier `KotlinBankAPI`
2. IntelliJ détecte Gradle et propose d'importer → **Trust Project** + **Open as Project**
3. Attendre l'indexation (5-10 min au premier import — gros téléchargement de dépendances)
4. **Vérifier le JDK** : `File → Project Structure → Project SDK` doit être en **17** (Temurin)
5. **Lancer l'app depuis l'IDE** : ouvrir `Application.kt`, clic droit dans la fonction `main()` → `Run 'ApplicationKt'`

### Réglages utiles

- **Settings → Build, Execution, Deployment → Build Tools → Gradle** :
  - "Build and run using" : **IntelliJ IDEA** (build plus rapide)
  - "Gradle JVM" : 17
- **Settings → Editor → Code Style → Kotlin** : `Import` → "Kotlin official"

### Plugin recommandé (optionnel)

- **Kotlin** : bundled
- **Database Tools** (Ultimate uniquement) : pour inspecter Postgres depuis l'IDE

---

## 4. Workflows courants

### Lancer l'API

```bash
./start.sh                       # macOS/Linux : Docker + healthcheck + API (tout-en-un)
.\start.ps1                      # Windows : idem

./gradlew run                    # ou à la main (Docker doit déjà tourner) — Ctrl+C pour stopper
```

### Lancer les tests

```bash
./gradlew test                   # ⚠️ arrêter ./gradlew run avant (voir Troubleshooting)
```

### Recompiler sans lancer

```bash
./gradlew compileKotlin
```

### Tail des logs de l'API

Quand tu lances avec `./gradlew run`, les logs sont en stdout. Pour les filtrer :

```bash
./gradlew run 2>&1 | grep -E "ERROR|WARN|Refreshed"
```

### Se connecter à la BDD en CLI

```bash
PGPASSWORD=finsim_dev psql -h localhost -p 5434 -U finsim -d finsim
```

Commandes utiles dans psql :
```sql
\dt                              -- liste des tables
\d users                         -- détail d'une table
SELECT * FROM assets;            -- voir les prix actuels
SELECT * FROM ledger ORDER BY created_at DESC LIMIT 10;
\q                               -- quitter
```

### Reset complet de la BDD (table rase)

```bash
docker compose down -v           # supprime les containers ET les volumes
docker compose up -d             # recrée tout depuis zéro
./gradlew run                    # Flyway re-migre, seeder re-insère
```

### Arrêter Docker

```bash
docker compose stop              # garde les volumes (état préservé)
docker compose down              # supprime les containers (volumes intacts)
docker compose down -v           # tout supprimer (volumes inclus)
```

### Voir les logs des containers

```bash
docker compose logs -f postgres
docker compose logs -f redis
```

### Se reconnecter à Redis

```bash
docker compose exec redis redis-cli
> PING
> KEYS *
> QUIT
```

---

## 5. Workflow Git

```bash
# Récupérer les derniers changements
git pull

# Créer une branche pour ton feature
git checkout -b feat/order-sell

# Voir l'état
git status
git diff

# Commiter (à la main, ou demande à Claude)
git add <fichiers>
git commit -m "feat: implement SELL order"

# Pousser
git push -u origin feat/order-sell
```

---

## 6. Outils optionnels qui changent la vie

### Bruno / Postman / Insomnia

Pour tester l'API avec une UI au lieu de curl. Bruno est recommandé (gratuit, open-source, fichiers texte versionnable).

Crée une collection avec :
- `POST /api/v1/auth/register` (body JSON)
- `POST /api/v1/auth/login` → extraire `accessToken` et le stocker en variable
- `GET /api/v1/auth/me` (header `Authorization: Bearer {{accessToken}}`)
- etc.

### httpie

Plus lisible que curl pour le dev :

```bash
http POST :8080/api/v1/auth/login email=toi@test.io password=motdepasse123
http GET :8080/api/v1/portfolio Authorization:"Bearer $TOKEN"
```

### jq

Parser JSON dans le terminal :

```bash
curl -s http://localhost:8080/api/v1/market/assets | jq '.[] | {ticker, price: .lastPrice}'
```

### TablePlus / DBeaver

GUI pour PostgreSQL si tu n'aimes pas psql. TablePlus est plus joli (payant après essai), DBeaver est gratuit et complet.

Connexion :
- Host : `localhost`
- Port : `5434`
- Database : `finsim`
- User : `finsim`
- Password : `finsim_dev`

---

## 7. Troubleshooting

### "Could not bind to port 5432/6379"

Tu as déjà un Postgres ou un Redis natif qui tourne sur le port standard.

Notre `docker-compose.yml` utilise volontairement **5434** et **6380** pour éviter ce conflit. Si tu as un autre service sur ces ports aussi, change-les dans `docker-compose.yml` + `AppConfig.kt`.

Pour voir ce qui occupe un port :
```bash
lsof -nP -iTCP:5432 -sTCP:LISTEN
```

### "role finsim does not exist" au boot Kotlin

C'est le piège #1 sur Mac : ton JDBC tape sur un **Postgres natif** au lieu du container Docker, parce que `localhost` résout sur le natif s'il existe.

→ Vérifie que `DATABASE_URL` pointe bien sur le port **5434** (pas 5432).

### `./gradlew test` échoue avec "Could not find a valid Docker environment"

Bug connu Docker Desktop 29.x + Testcontainers. Notre test E2E utilise la DB dev (port 5434) au lieu de Testcontainers, donc cette erreur ne devrait pas apparaître. Si elle apparaît, vérifie que tu es sur la dernière version du code.

### `./gradlew run` et `./gradlew test` lancés en parallèle

**Ne pas faire.** Le `PriceRefreshJob` du `run` va écraser le prix BTC pinné par le test → assertions cassent.

Workflow :
1. Stop le `gradlew run` (Ctrl+C)
2. Lance le `gradlew test`
3. Relance `gradlew run` après

### "Address already in use: 8080"

Une autre instance de l'API tourne. Trouve-la :
```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```
Puis kill le PID.

### Le port CoinGecko répond 429 (rate limited)

Free tier CoinGecko : ~10-30 req/min. Notre job tape toutes les 30s sur 1 endpoint, ça passe largement. Si tu te fais ban, attends 1 min ou get une API key gratuite sur https://www.coingecko.com/en/api/pricing et mets-la dans `COINGECKO_API_KEY`.

### IntelliJ ne reconnaît pas les imports Kotlin

`File → Invalidate Caches → Invalidate and Restart`. Puis attendre la réindexation.

### Gradle daemon coincé

```bash
./gradlew --stop
```

---

## 8. Cheat sheet — commandes les plus utilisées

```bash
# Démarrer la stack complète (tout-en-un)
./start.sh                       # macOS/Linux   (.\start.ps1 sous Windows)

# ... ou à la main
docker compose up -d && ./gradlew run

# Run + filter logs sur les events importants
./gradlew run 2>&1 | grep -E "ERROR|WARN|Refreshed|Responding"

# Test
./gradlew test

# Voir les assets et leurs prix
curl -s :8080/api/v1/market/assets | jq

# Health
curl -s :8080/health | jq

# Reset complet BDD
docker compose down -v && docker compose up -d && ./gradlew run

# Psql vite fait
PGPASSWORD=finsim_dev psql -h localhost -p 5434 -U finsim -d finsim

# Voir l'historique d'un utilisateur
PGPASSWORD=finsim_dev psql -h localhost -p 5434 -U finsim -d finsim \
  -c "SELECT type, amount, balance_after, created_at FROM ledger WHERE user_id = (SELECT id FROM users WHERE email='toi@test.io') ORDER BY created_at;"

# Stop tout
docker compose stop
```

---

## 9. Pour aller plus loin

- Lire le `README.md` à la racine — détail de l'avancement, dette technique, roadmap par sprint
- Consulter le plan PDF original (Plan de Reprise Backend) pour le contexte produit
- Le test `EndToEndFlowTest.kt` est le meilleur point d'entrée pour comprendre le flow complet

Bon dev 🚀
