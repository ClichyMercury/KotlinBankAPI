# Brief — front web FinSim

Tout ce qu'il faut pour construire `finsim.wharpe.com` et le brancher sur l'API existante.
Ce document est la source de vérité du contrat : l'API est **déjà déployée et figée**, le front
s'adapte à elle.

| | |
|---|---|
| **Front à construire** | `https://finsim.wharpe.com` |
| **API existante** | `https://api-finsim.wharpe.com` |
| **Dépôt API** | `ClichyMercury/KotlinBankAPI` |

---

## 1. Avant d'écrire une ligne — deux choses à faire sur l'API

Sans ça, le front ne pourra pas parler à l'API. Les deux prennent une minute.

### a) Autoriser l'origine du front (CORS)

Le front et l'API sont sur deux sous-domaines différents : **toutes** les requêtes seront
cross-origin. Dans Dokploy → application de l'API → Environment :

```
CORS_ALLOWED_HOSTS=finsim.wharpe.com
```

puis redéployer. Sans ça, chaque appel revient en `403`. Host nu : pas de `https://`, pas de
slash final — l'API refuse de démarrer sinon.

### b) Créer le DNS de `finsim.wharpe.com`

⚠️ **Le wildcard `*.wharpe.com` ne couvre plus ce nom.** Depuis qu'on a ajouté les
enregistrements Resend sous `send.finsim.wharpe.com`, `finsim.wharpe.com` est devenu un nœud
intermédiaire et le wildcard ne le synthétise plus (comportement DNS normal, RFC 4592).

Chez Namecheap → Advanced DNS → Add New Record :

```
Type  : A
Host  : finsim
Value : 62.171.167.176
TTL   : Automatic
```

Vérification en ligne de commande : `dig +short A finsim.wharpe.com` doit renvoyer l'IP.

---

## 2. Conventions de l'API

### Les nombres sont des chaînes

Tous les montants, quantités et prix sont sérialisés en **chaînes JSON**, jamais en nombres :
`"quantity": "0.1"`, `"price": "83814.00000000"`.

C'est délibéré : ce sont des `BigDecimal` côté serveur. **Ne les parse jamais en `Number`
JavaScript** — `0.1 + 0.2` suffit à produire un écart sur de la monnaie. Utilise
`decimal.js`, `big.js` ou `Intl.NumberFormat` sur la chaîne d'origine.

Les dates sont des chaînes ISO-8601 UTC. Les identifiants sont des UUID en chaîne.

### Format d'erreur

Toute erreur renvoie la même forme :

```json
{ "error": "validation_error", "message": "Insufficient balance: need 7000.00, have 3000.00" }
```

| `error` | HTTP | Quand |
|---|---|---|
| `validation_error` | 400 | entrée invalide, solde ou quantité insuffisants |
| `bad_request` / `invalid_json` | 400 | corps malformé |
| `unauthorized` | 401 | token absent, invalide ou expiré |
| `forbidden` | 403 | origine non autorisée — voir §1a |
| `not_found` | 404 | ressource inexistante |
| `conflict` | 409 | email ou pseudo déjà pris |
| `rate_limited` | 429 | quota dépassé, l'en-tête `Retry-After` donne le délai |
| `internal_error` | 500 | erreur serveur |

Affiche toujours `message` à l'utilisateur plutôt qu'un texte générique : il est rédigé pour
être lisible. Le seul cas à traduire toi-même est le `429`.

### Rate limit

**10 requêtes par minute et par IP**, partagées entre `register`, `login`, `refresh`,
`forgot-password` et `reset-password`. C'est serré — connu et tracé comme dette #16 côté API.
Concrètement : ne déclenche pas de `refresh` en rafale, et sérialise-les (voir §3).

---

## 3. Authentification

### Le modèle

| Token | Durée | Rôle |
|---|---|---|
| `accessToken` (JWT) | 1 h | en-tête `Authorization: Bearer …` sur toute route 🔒 |
| `refreshToken` (opaque) | 30 j | corps de `POST /auth/refresh` uniquement |

`register`, `login` et `refresh` renvoient la paire :

```json
{
  "accessToken": "eyJhbGciOi…",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "refreshToken": "kJ8xQ2mV…",
  "refreshExpiresInSeconds": 2592000,
  "user": { "id": "…", "email": "…", "pseudo": "…", "createdAt": "…" }
}
```

*(`refresh` renvoie la même chose sans le bloc `user`.)*

### Rotation à usage unique

Chaque appel à `/auth/refresh` **consomme** le refresh token et en renvoie un nouveau.
Écrase systématiquement la valeur stockée.

Rejouer un token déjà consommé est traité comme une fuite : l'API révoque **toutes** les
sessions et répond `401 Refresh token reuse detected, all sessions revoked`.

> **Conséquence directe pour le front** : deux `/auth/refresh` concurrents déconnectent
> l'utilisateur. Un onglet qui lance trois requêtes expirées en parallèle suffit. Il **faut**
> un verrou : un seul refresh en vol, les autres requêtes attendent son résultat.

### Distinguer les deux 401 de `/auth/refresh`

| Message | Cause | Ce que voit l'utilisateur |
|---|---|---|
| `Invalid refresh token` | session fermée normalement (logout, reset password) | reconnexion simple |
| `Refresh token reuse detected…` | rejeu d'un token consommé | reconnexion + avertissement de sécurité justifié |

N'affiche l'avertissement que sur le second.

### Stockage dans un navigateur

L'API renvoie le refresh token dans le corps JSON, elle ne pose pas de cookie. Le front doit
donc le stocker lui-même, et **tout stockage accessible au JavaScript est exposé au XSS**.

Arbitrage pour le MVP : `accessToken` **en mémoire** (variable de module, perdu au rechargement,
récupéré par un refresh), `refreshToken` dans `localStorage`. C'est le compromis habituel, il
est acceptable tant que le front n'affiche aucun contenu tiers. Si un jour tu ajoutes du
contenu utilisateur ou des scripts externes, il faudra passer le refresh token en cookie
`HttpOnly` côté API — ce serait un changement d'API, à demander explicitement.

Ne logge jamais un token, et ne le mets jamais dans une URL.

### Intercepteur HTTP

```
1. Requête 🔒 avec l'access token courant
2. Réponse 401 ?
   ├── non  -> retourner la réponse
   └── oui  -> prendre le verrou de refresh
               ├── un refresh est déjà en vol -> attendre son résultat
               └── sinon POST /auth/refresh { refreshToken stocké }
                    ├── 200 -> écraser les DEUX tokens, rejouer la requête UNE fois
                    └── 401 -> purger le stockage, aller à l'écran de connexion
```

Ne rejoue la requête **qu'une seule fois**, sinon un 401 venant d'autre chose (compte
désactivé) provoque une boucle infinie.

---

## 4. Endpoints

🔒 = requiert `Authorization: Bearer <accessToken>`

### Auth — `/api/v1/auth`

| Méthode | Chemin | Corps | Réponse |
|---|---|---|---|
| POST | `/register` | `{ email, pseudo, password }` | 201 + paire de tokens |
| POST | `/login` | `{ email, password }` | 200 + paire de tokens |
| POST | `/refresh` | `{ refreshToken }` | 200 + nouvelle paire |
| POST | `/logout` | `{ refreshToken }` | 200 `{ message }` |
| POST | `/logout-all` 🔒 | — | 200 `{ message }` |
| GET | `/me` 🔒 | — | 200 `UserResponse` |
| POST | `/forgot-password` | `{ email }` | 200 générique |
| POST | `/reset-password` | `{ token, newPassword }` | 200 `{ message }` |

Règles de validation : email au format standard, pseudo `[a-zA-Z0-9_]{3,50}`, mot de passe
≥ 8 caractères.

`forgot-password` renvoie **toujours** le même message, que l'email existe ou non — c'est
volontaire (pas d'énumération de comptes). Affiche-le tel quel, n'invente pas de « compte
introuvable ».

### Marché — `/api/v1/market`

| Méthode | Chemin | Réponse |
|---|---|---|
| GET | `/assets` (`?type=CRYPTO\|STOCK\|FOREX`) | `AssetResponse[]` |
| GET | `/assets/{uuid}` | `AssetResponse` |
| GET | `/assets/{uuid}/candles?days=1\|7\|30\|365` | `CandleResponse[]` |

```ts
AssetResponse  = { id, ticker, name, type, market, lastPrice?, lastPriceUpdatedAt? }
CandleResponse = { timestamp, open, high, low, close }
```

Les prix sont rafraîchis côté serveur toutes les 30 s. Un `lastPrice` à `null` signifie
qu'aucun prix n'est encore disponible — l'achat sera refusé, grise le bouton.

Les endpoints marché sont **publics** : tu peux afficher les cours avant connexion.

### Portefeuille et ordres

| Méthode | Chemin | Corps | Réponse |
|---|---|---|---|
| GET | `/api/v1/portfolio` 🔒 | — | `PortfolioResponse` |
| POST | `/api/v1/orders/buy` 🔒 | `{ assetId, quantity }` | 201 `OrderResponse` |
| POST | `/api/v1/orders/sell` 🔒 | `{ assetId, quantity }` | 201 `OrderResponse` |
| GET | `/api/v1/orders` 🔒 | — | `OrderResponse[]` (récent en premier) |

```ts
PortfolioResponse = {
  balanceFictif, assetsValue, totalValue,
  assets: [{ assetId, ticker, name, quantity, avgBuyPrice,
             currentPrice, currentValue, unrealizedPnl }]
}

OrderResponse = { id, assetId, type, quantity, price, total,
                  status, createdAt, executedAt?, realizedPnl? }
```

`realizedPnl` n'est renseigné **que sur un SELL** : c'est `(prix de vente − avgBuyPrice) × quantité`.
C'est le chiffre à mettre en avant après une vente, vert ou rouge selon le signe. Sur un BUY il
vaut `null`.

Une position soldée à zéro **disparaît** de `assets` — ne cherche pas une ligne à quantité nulle.

Chaque utilisateur démarre avec **10 000 $ fictifs**.

### Système

| Méthode | Chemin | Usage |
|---|---|---|
| GET | `/health` | `{ status, db, redis, sentry }` |
| GET | `/reset-password?token=…` | page de reset servie par l'API — voir §6 |

---

## 5. Identité visuelle

Il n'existe pour l'instant qu'une seule surface FinSim, la page de reset password servie par
l'API. Reprends sa palette pour rester cohérent — c'est aujourd'hui la définition de la marque.

```css
:root {
  --bg:           #f5f6f8;
  --card:         #ffffff;
  --text:         #1a1a1a;
  --muted:        #6b7280;
  --border:       #d8dbe0;
  --accent:       #1a7f5a;   /* vert financier, boutons et liens */
  --accent-text:  #ffffff;
  --error:        #b42318;
}

@media (prefers-color-scheme: dark) {
  :root {
    --bg:          #14161a;
    --card:        #1d2025;
    --text:        #f2f3f5;
    --muted:       #9aa1ab;
    --border:      #333842;
    --accent:      #2ea37a;
    --accent-text: #08120d;
    --error:       #f28b82;
  }
}
```

À ajouter pour le trading, absent de la palette actuelle : une couleur de **gain** et une de
**perte**. `--accent` fait office de vert, prends un rouge distinct de `--error` pour ne pas
confondre « tu perds de l'argent » et « tu as fait une erreur ».

Autres partis pris de la page existante, à conserver : rayons de 9 à 14 px, cartes blanches
sur fond gris, pile de polices système (`-apple-system, Segoe UI, Roboto, sans-serif`), thème
clair/sombre automatique via `prefers-color-scheme`.

Il n'y a **pas encore de logo**.

---

## 6. Écrans minimum

1. **Accueil / marché** — liste des actifs avec prix, publique
2. **Détail actif** — chandeliers OHLC (`?days=1|7|30|365`), bouton acheter
3. **Inscription / connexion**
4. **Portefeuille** — solde, valeur totale, positions avec PnL latent
5. **Passer un ordre** — achat et vente, avec le `realizedPnl` mis en avant après une vente
6. **Historique des ordres**
7. **Mot de passe oublié** — formulaire qui appelle `/auth/forgot-password`

### Le cas particulier du reset password

L'email envoyé pointe aujourd'hui vers `https://api-finsim.wharpe.com/reset-password?token=…`,
une page HTML autonome servie par l'API.

Quand ton écran de reset existera sur le front, il suffira de changer **une variable d'env**
côté API, sans toucher au code :

```
PASSWORD_RESET_URL=https://finsim.wharpe.com/reset-password?token={token}
```

Le placeholder `{token}` est obligatoire. Ton écran lit le token dans l'URL et le poste sur
`POST /api/v1/auth/reset-password` avec le nouveau mot de passe.

Deux détails repris de la page existante, à ne pas perdre : valide la longueur du mot de passe
**avant** d'appeler l'API (un mot de passe trop court renvoie 400 sans consommer le token,
l'utilisateur peut réessayer avec le même lien), et ne réfléchis jamais le token dans le HTML —
lis-le depuis `location.search`.

---

## 7. Choix technique

Aucune contrainte imposée par l'API : c'est du REST + JSON, tout stack fait l'affaire.

Deux orientations qui ont du sens ici :

- **Statique (Vite + React/Vue/Svelte)** — le plus simple à déployer, un conteneur nginx qui
  sert des fichiers. Suffisant tant qu'il n'y a pas de SEO à travailler.
- **SSR (Next.js, Nuxt, SvelteKit)** — utile si les pages marché doivent être indexées par
  Google. Plus lourd à déployer.

Pour un MVP qui double une app mobile, **le statique suffit**.

---

## 8. Déployer sur `finsim.wharpe.com` avec Dokploy

Dans l'ordre. Les étapes 1 et 2 sont celles du §1, à faire une seule fois.

**1. DNS** — enregistrement A `finsim` → `62.171.167.176` chez Namecheap (§1b).

**2. CORS** — `CORS_ALLOWED_HOSTS=finsim.wharpe.com` sur l'application API, puis redéployer (§1a).

**3. Dockerfile** dans le dépôt du front. Pour un build statique :

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

Avec un `nginx.conf` qui renvoie `index.html` sur les routes inconnues, sinon un rechargement
sur une route profonde donne un 404 :

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  location / { try_files $uri $uri/ /index.html; }
}
```

**4. Application Dokploy** — même projet que l'API :
Create Service → Application → source GitHub, dépôt du front, branche `main`,
Build Type **Dockerfile**.

**5. Variables d'environnement** — le front a besoin de l'adresse de l'API. Avec Vite :

```
VITE_API_BASE_URL=https://api-finsim.wharpe.com
```

⚠️ Une variable de front est **compilée dans le bundle** et donc publique. N'y mets jamais de
secret : l'URL de l'API, oui ; une clé d'API, jamais.

**6. Domaine** — onglet Domains → Add Domain :

| Champ | Valeur |
|---|---|
| Host | `finsim.wharpe.com` |
| Path | `/` |
| Container Port | `80` (nginx) — `3000` si tu pars sur du SSR Node |
| HTTPS | activé (Let's Encrypt) |

**7. Deploy**, puis vérifier :

```bash
curl -I https://finsim.wharpe.com
curl -s https://api-finsim.wharpe.com/health
```

### Si ça ne marche pas

| Symptôme | Cause quasi certaine |
|---|---|
| `403` sur tous les appels API | `CORS_ALLOWED_HOSTS` oublié ou mal écrit (§1a) |
| Le domaine ne répond pas | enregistrement A absent (§1b) — le wildcard ne couvre pas ce nom |
| Certificat non généré | le DNS ne pointait pas encore sur le serveur au moment du déploiement |
| `404` en rechargeant une route profonde | `try_files` manquant dans `nginx.conf` |
| Montants faux de quelques centimes | des `BigDecimal` parsés en `Number` — voir §2 |

---

## 9. Référence

- API déployée : `https://api-finsim.wharpe.com`
- Contrat détaillé auth et sessions : [`MOBILE_AUTH.md`](MOBILE_AUTH.md)
- Contrat détaillé ordres : [`MOBILE_ORDERS.md`](MOBILE_ORDERS.md)
- Collection Postman : `FinSim.postman_collection.json` à la racine du dépôt API
- Dette technique et roadmap : `README.md` du dépôt API
