# Intégration mobile — Orders (BUY / SELL)

Guide d'intégration côté app mobile pour passer des ordres d'achat et de vente sur FinSim.
À jour avec l'ajout de l'**ordre SELL** (`POST /api/v1/orders/sell`).

---

## 1. Conventions générales

- **Base URL (dev)** : `http://localhost:8080`
- **Préfixe** : tous les endpoints métier sont sous `/api/v1`
- **Auth** : les routes orders/portfolio exigent l'en-tête
  `Authorization: Bearer <accessToken>` (token obtenu via `/auth/login` ou `/auth/register`).
- **Content-Type** : `application/json` sur tout POST avec body.

### ⚠️ Les nombres sont des **strings** JSON

Tous les montants/quantités (`BigDecimal`), les `UUID` et les dates (`Instant`) sont
sérialisés en **chaînes**, pas en nombres. Exemple : `"quantity": "0.1"`, `"price": "70000.00"`.

> Côté mobile, parse ces champs avec un type décimal exact (`BigDecimal` Kotlin / `Decimal` Swift),
> **jamais** un `Double` (risque d'erreur d'arrondi sur la money). Les dates sont au format ISO-8601 UTC.

### Format d'erreur commun

Toute erreur renvoie ce corps :

```json
{ "error": "validation_error", "message": "Insufficient quantity: trying to sell 100, hold 0.05" }
```

| `error`            | HTTP | Quand |
|--------------------|------|-------|
| `validation_error` | 400  | input invalide, solde/quantité insuffisant, prix indisponible |
| `invalid_json`     | 400  | JSON malformé |
| `unauthorized`     | 401  | token absent / invalide / expiré |
| `not_found`        | 404  | asset ou portfolio introuvable |
| `conflict`         | 409  | conflit (ex. email déjà pris à l'inscription) |
| `internal_error`   | 500  | erreur serveur inattendue |

---

## 2. `POST /api/v1/orders/buy` 🔒

Achète une quantité d'un asset au `last_price` courant.

**Request**
```json
{ "assetId": "a1b2c3d4-....", "quantity": "0.1" }
```

**Response `201 Created`**
```json
{
  "id": "....",
  "assetId": "a1b2c3d4-....",
  "type": "BUY",
  "quantity": "0.1",
  "price": "70000.00",
  "total": "7000.00",
  "status": "EXECUTED",
  "createdAt": "2026-06-29T16:05:28.909Z",
  "executedAt": "2026-06-29T16:05:28.909Z",
  "realizedPnl": null
}
```

Effets : débite le solde (`quantity × price`), crée/augmente la position
(recalcul du `avgBuyPrice` pondéré), écrit une entrée ledger `BUY` (montant négatif).

Erreurs probables : `validation_error` si `quantity ≤ 0`, solde insuffisant,
ou asset sans prix ; `not_found` si l'asset n'existe pas.

---

## 3. `POST /api/v1/orders/sell` 🔒 — **nouveau**

Vend une quantité d'un asset détenu au `last_price` courant.

**Request**
```json
{ "assetId": "a1b2c3d4-....", "quantity": "0.05" }
```

**Response `201 Created`**
```json
{
  "id": "....",
  "assetId": "a1b2c3d4-....",
  "type": "SELL",
  "quantity": "0.05",
  "price": "80000.00",
  "total": "4000.00",
  "status": "EXECUTED",
  "createdAt": "2026-06-29T16:10:00.000Z",
  "executedAt": "2026-06-29T16:10:00.000Z",
  "realizedPnl": "500.00"
}
```

### Spécificités SELL (à exploiter côté UI)

- **`realizedPnl`** = `(price − avgBuyPrice) × quantity`, arrondi à 2 décimales.
  - Présent uniquement sur un SELL (toujours `null` pour un BUY).
  - Positif = plus-value réalisée, négatif = moins-value. Idéal pour afficher un toast
    « +500 $ réalisés 🎉 » après la vente.
- **Solde** crédité de `total` (`quantity × price`).
- **Position** : la quantité détenue est décrémentée ; si elle tombe à **0**, la ligne
  disparaît du portfolio (l'asset ne sera plus dans `portfolio.assets`).
- Le **`avgBuyPrice`** ne change **pas** lors d'une vente (c'est un coût d'acquisition).

### Erreurs probables

| Cas | Réponse |
|---|---|
| `quantity ≤ 0` | `400 validation_error` — "Quantity must be strictly positive" |
| Aucune position sur cet asset | `400 validation_error` — "No <TICKER> position to sell" |
| Quantité vendue > quantité détenue | `400 validation_error` — "Insufficient quantity: trying to sell X, hold Y" |
| Asset sans prix courant | `400 validation_error` |
| Asset inexistant | `404 not_found` |
| Token manquant/expiré | `401 unauthorized` |

---

## 4. Rafraîchir l'écran après un ordre — `GET /api/v1/portfolio` 🔒

Après un BUY/SELL, recharge le portfolio pour mettre à jour soldes et positions.

```json
{
  "balanceFictif": "7000.00",
  "assetsValue": "4000.00",
  "totalValue": "11000.00",
  "assets": [
    {
      "assetId": "a1b2c3d4-....",
      "ticker": "BTC",
      "name": "Bitcoin",
      "quantity": "0.05",
      "avgBuyPrice": "70000.00000000",
      "currentPrice": "80000.00",
      "currentValue": "4000.00",
      "unrealizedPnl": "500.00"
    }
  ]
}
```

- `unrealizedPnl` = plus/moins-value **latente** par position (au prix courant).
- `realizedPnl` (renvoyé par le SELL) = plus/moins-value **réalisée** au moment de la vente.
  Les deux sont distincts : l'un est « sur le papier », l'autre est encaissé.

---

## 5. `GET /api/v1/orders` 🔒 — historique

Renvoie les ordres de l'utilisateur (BUY et SELL mélangés), le plus récent d'abord :
liste d'objets au même format que la réponse de buy/sell (le `realizedPnl` est renseigné
sur les lignes SELL).

---

## 6. Flow type côté mobile

```
1. POST /auth/login              -> récupère accessToken (à stocker en secure storage)
2. GET  /market/assets           -> liste des assets + prix ; l'utilisateur choisit un assetId
3. POST /orders/buy  {assetId, quantity}
4. GET  /portfolio               -> rafraîchit soldes + positions
5. POST /orders/sell {assetId, quantity}   -> affiche realizedPnl
6. GET  /portfolio               -> position décrémentée (ou disparue si soldée)
```

> Le token expire après 60 min → sur un `401 unauthorized`, relancer un `/auth/login`
> (le refresh token n'est pas encore implémenté, cf. roadmap Sprint 2).

---

## 7. Tester rapidement

La collection **Postman** versionnée à la racine (`FinSim.postman_collection.json`) contient
déjà les requêtes **Buy order** et **Sell order** prêtes à l'emploi (variables `baseUrl`,
`accessToken` auto-rempli au login, `assetId`).
