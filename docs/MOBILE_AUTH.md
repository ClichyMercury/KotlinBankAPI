# Intégration mobile — Auth & sessions (refresh token)

Guide d'intégration côté app mobile pour l'authentification FinSim : obtention de la paire
de tokens, renouvellement silencieux, déconnexion.

Pour les ordres BUY/SELL → **[MOBILE_ORDERS.md](MOBILE_ORDERS.md)**.

---

## 1. Le modèle de session

Deux tokens, deux rôles :

| Token | Format | Durée | Où il sert |
|---|---|---|---|
| `accessToken` | JWT signé HMAC256 | **1h** (`JWT_EXPIRATION_MINUTES`) | en-tête `Authorization: Bearer <...>` sur toute route 🔒 |
| `refreshToken` | chaîne opaque (32 bytes base64url) | **30j** (`JWT_REFRESH_EXPIRATION_DAYS`) | body de `POST /auth/refresh` uniquement |

L'access token n'est **pas** révocable : il reste valide jusqu'à son expiration. Le refresh
token, lui, est stocké côté serveur (hashé SHA-256) et révocable à tout moment.

### Rotation à usage unique

Chaque appel à `/auth/refresh` **consomme** le refresh token envoyé et en renvoie un nouveau.
L'ancien devient immédiatement invalide — il faut donc **écraser** la valeur stockée à chaque
refresh réussi.

### Détection de réutilisation

Rejouer un refresh token déjà consommé est traité comme une fuite : le serveur révoque
**toutes** les sessions de l'utilisateur et renvoie `401 Refresh token reuse detected,
all sessions revoked`. L'app doit alors purger ses tokens et renvoyer l'utilisateur sur
l'écran de login.

> Conséquence pratique : deux appels `/auth/refresh` concurrents avec le même token
> déconnectent l'utilisateur. Sérialise les refresh derrière un mutex / un seul appel en vol.

---

## 2. Endpoints

### `POST /api/v1/auth/register` → 201

```json
{ "email": "toi@test.io", "pseudo": "toi", "password": "motdepasse123" }
```

### `POST /api/v1/auth/login` → 200

```json
{ "email": "toi@test.io", "password": "motdepasse123" }
```

Les deux renvoient la même forme :

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "refreshToken": "kJ8xQ2mV...",
  "refreshExpiresInSeconds": 2592000,
  "user": { "id": "...", "email": "toi@test.io", "pseudo": "toi", "createdAt": "2026-09-25T10:12:00Z" }
}
```

### `POST /api/v1/auth/refresh` → 200

```json
{ "refreshToken": "kJ8xQ2mV..." }
```

Réponse (même forme, **sans** le bloc `user`) :

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "refreshToken": "nouveau-token-opaque",
  "refreshExpiresInSeconds": 2592000
}
```

### `POST /api/v1/auth/logout` → 200

```json
{ "refreshToken": "kJ8xQ2mV..." }
```

Révoque ce seul refresh token (cet appareil). Réponse `{ "message": "Logged out" }`.
Idempotent : un token inconnu ou déjà révoqué renvoie quand même 200.

### `POST /api/v1/auth/logout-all` 🔒 → 200

Sans body, avec `Authorization: Bearer <accessToken>`. Révoque **toutes** les sessions de
l'utilisateur (tous ses appareils). Réponse `{ "message": "All sessions revoked" }`.

> Les access tokens déjà émis restent valides jusqu'à 1h après un `logout` / `logout-all`.
> Pour une déconnexion visuellement immédiate, l'app doit aussi effacer son access token local.

---

## 3. Erreurs

| HTTP | `error` | `message` | Que faire côté app |
|---|---|---|---|
| 400 | `validation_error` | `Refresh token is required` | bug client : token vide envoyé |
| 401 | `unauthorized` | `Invalid refresh token` | purger les tokens → écran login |
| 401 | `unauthorized` | `Refresh token expired` | purger les tokens → écran login |
| 401 | `unauthorized` | `Refresh token reuse detected, all sessions revoked` | purger les tokens → écran login + prévenir l'utilisateur |
| 401 | `unauthorized` | `Account disabled` | compte désactivé, login impossible |
| 429 | — | — | rate limit auth : 10 req/min/IP (register + login + refresh + logout partagent le compteur) |

---

## 4. Flow recommandé (intercepteur HTTP)

```
1. Requête 🔒 avec l'access token courant
2. Réponse 401 ?
   ├── non  -> retourner la réponse
   └── oui  -> prendre le mutex refresh
               ├── un refresh est déjà en vol -> attendre son résultat
               └── sinon POST /auth/refresh { refreshToken stocké }
                    ├── 200 -> écraser accessToken ET refreshToken, rejouer la requête une fois
                    └── 401 -> purger le stockage, naviguer vers login
```

Points d'attention :

- **Ne rejoue la requête qu'une seule fois** après un refresh réussi, sinon boucle infinie sur
  un 401 qui viendrait d'autre chose (compte désactivé, route mal appelée).
- **Un seul refresh en vol** (mutex) — voir la détection de réutilisation ci-dessus.
- **Refresh proactif** possible : rafraîchir quand il reste < 5 min sur `expiresInSeconds`
  évite un 401 visible, mais l'intercepteur reste nécessaire (app en veille, horloge décalée).

### Stockage

| Plateforme | Où mettre le `refreshToken` |
|---|---|
| Android | `EncryptedSharedPreferences` (Jetpack Security) |
| iOS | Keychain (`kSecAttrAccessibleAfterFirstUnlock`) |

Jamais en `SharedPreferences` brut, `UserDefaults`, ni dans les logs — c'est une clé de
session de 30 jours.

---

## 5. Tester rapidement

```bash
# 1. Login -> récupérer les deux tokens
curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"toi@test.io","password":"motdepasse123"}'

# 2. Rotation
curl -s -X POST localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<le refreshToken reçu>"}'

# 3. Rejouer l'ancien token -> 401 reuse detected, toutes les sessions tombent
curl -s -X POST localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<le MÊME token qu'à l'étape 2>"}'
```

La collection Postman (`FinSim.postman_collection.json`) contient les requêtes
`Auth > Refresh / Logout / Logout all`, avec sauvegarde automatique des deux tokens dans les
variables de collection.
