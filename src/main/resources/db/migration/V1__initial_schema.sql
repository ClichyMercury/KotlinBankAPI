CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    pseudo          VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE assets (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticker                  VARCHAR(50) NOT NULL UNIQUE,
    name                    VARCHAR(255) NOT NULL,
    type                    VARCHAR(20) NOT NULL CHECK (type IN ('STOCK', 'FOREX', 'CRYPTO')),
    market                  VARCHAR(50) NOT NULL,
    external_id             VARCHAR(100),
    last_price              NUMERIC(20, 8),
    last_price_updated_at   TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assets_type ON assets(type);

CREATE TABLE portfolios (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance_fictif  NUMERIC(20, 2) NOT NULL DEFAULT 10000,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE portfolio_assets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id    UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    asset_id        UUID NOT NULL REFERENCES assets(id),
    quantity        NUMERIC(20, 8) NOT NULL DEFAULT 0,
    avg_buy_price   NUMERIC(20, 8) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(portfolio_id, asset_id)
);

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_id        UUID NOT NULL REFERENCES assets(id),
    type            VARCHAR(10) NOT NULL CHECK (type IN ('BUY', 'SELL')),
    quantity        NUMERIC(20, 8) NOT NULL CHECK (quantity > 0),
    price           NUMERIC(20, 8) NOT NULL CHECK (price > 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'EXECUTED', 'FAILED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    executed_at     TIMESTAMPTZ
);

CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

CREATE TABLE ledger (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT', 'BUY', 'SELL', 'FEE')),
    amount          NUMERIC(20, 2) NOT NULL,
    order_id        UUID REFERENCES orders(id),
    balance_after   NUMERIC(20, 2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_user_created ON ledger(user_id, created_at DESC);
