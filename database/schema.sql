CREATE SCHEMA IF NOT EXISTS insightflow;

CREATE TABLE IF NOT EXISTS insightflow.customers (
  customer_id TEXT PRIMARY KEY,
  customer_unique_id TEXT NOT NULL,
  customer_zip_code_prefix TEXT,
  customer_city TEXT,
  customer_state TEXT
);

CREATE TABLE IF NOT EXISTS insightflow.orders (
  order_id TEXT PRIMARY KEY,
  customer_id TEXT NOT NULL REFERENCES insightflow.customers(customer_id),
  order_status TEXT NOT NULL,
  order_purchase_timestamp TIMESTAMP,
  order_approved_at TIMESTAMP,
  order_delivered_carrier_date TIMESTAMP,
  order_delivered_customer_date TIMESTAMP,
  order_estimated_delivery_date TIMESTAMP
);

CREATE TABLE IF NOT EXISTS insightflow.products (
  product_id TEXT PRIMARY KEY,
  product_category_name TEXT,
  product_name_lenght INTEGER,
  product_description_lenght INTEGER,
  product_photos_qty INTEGER,
  product_weight_g INTEGER,
  product_length_cm INTEGER,
  product_height_cm INTEGER,
  product_width_cm INTEGER
);

CREATE TABLE IF NOT EXISTS insightflow.sellers (
  seller_id TEXT PRIMARY KEY,
  seller_zip_code_prefix TEXT,
  seller_city TEXT,
  seller_state TEXT
);

CREATE TABLE IF NOT EXISTS insightflow.order_items (
  order_id TEXT NOT NULL REFERENCES insightflow.orders(order_id),
  order_item_id INTEGER NOT NULL,
  product_id TEXT REFERENCES insightflow.products(product_id),
  seller_id TEXT REFERENCES insightflow.sellers(seller_id),
  shipping_limit_date TIMESTAMP,
  price NUMERIC(12, 2),
  freight_value NUMERIC(12, 2),
  PRIMARY KEY (order_id, order_item_id)
);

CREATE TABLE IF NOT EXISTS insightflow.order_payments (
  order_id TEXT NOT NULL REFERENCES insightflow.orders(order_id),
  payment_sequential INTEGER NOT NULL,
  payment_type TEXT,
  payment_installments INTEGER,
  payment_value NUMERIC(12, 2),
  PRIMARY KEY (order_id, payment_sequential)
);

CREATE TABLE IF NOT EXISTS insightflow.order_reviews (
  review_id TEXT,
  order_id TEXT NOT NULL REFERENCES insightflow.orders(order_id),
  review_score INTEGER,
  review_comment_title TEXT,
  review_comment_message TEXT,
  review_creation_date TIMESTAMP,
  review_answer_timestamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS insightflow.product_category_translation (
  product_category_name TEXT PRIMARY KEY,
  product_category_name_english TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS insightflow.app_users (
  id UUID PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  display_name TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS insightflow.saved_dashboards (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES insightflow.app_users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  filters JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS insightflow.dashboard_preferences (
  user_id UUID PRIMARY KEY REFERENCES insightflow.app_users(id) ON DELETE CASCADE,
  theme TEXT NOT NULL DEFAULT 'light',
  compact_view BOOLEAN NOT NULL DEFAULT FALSE,
  default_dashboard_id UUID REFERENCES insightflow.saved_dashboards(id) ON DELETE SET NULL,
  visible_sections JSONB NOT NULL DEFAULT '[]'::jsonb,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_purchase_month
  ON insightflow.orders (date_trunc('month', order_purchase_timestamp));

CREATE INDEX IF NOT EXISTS idx_orders_status
  ON insightflow.orders (order_status);

CREATE INDEX IF NOT EXISTS idx_orders_purchase_date
  ON insightflow.orders (order_purchase_timestamp);

CREATE INDEX IF NOT EXISTS idx_customers_state
  ON insightflow.customers (customer_state);

CREATE INDEX IF NOT EXISTS idx_order_items_product
  ON insightflow.order_items (product_id);

CREATE INDEX IF NOT EXISTS idx_order_payments_type
  ON insightflow.order_payments (payment_type);

CREATE INDEX IF NOT EXISTS idx_order_reviews_order
  ON insightflow.order_reviews (order_id);

CREATE INDEX IF NOT EXISTS idx_saved_dashboards_user
  ON insightflow.saved_dashboards (user_id, updated_at DESC);

-- Demo portfolio account. This is intentionally public and must not be reused
-- as a production credential. Password: portfolio-pass
INSERT INTO insightflow.app_users
  (id, email, display_name, password_hash, role)
VALUES
  (
    '11111111-1111-4111-8111-111111111111',
    'demo@example.com',
    'Demo User',
    '$2a$10$o7gRsuZRRgY.qouvzNhWSONPCd2iki5Y3jyoGqXNtvXFLTBBSFsq.',
    'USER'
  )
ON CONFLICT (email) DO NOTHING;

INSERT INTO insightflow.saved_dashboards
  (id, user_id, name, description, filters)
VALUES
  (
    '22222222-2222-4222-8222-222222222222',
    '11111111-1111-4111-8111-111111111111',
    'SP credit card performance',
    'Demo saved view for portfolio walkthroughs',
    '{"state":"SP","paymentType":"Credit Card"}'::jsonb
  )
ON CONFLICT (id) DO NOTHING;

INSERT INTO insightflow.dashboard_preferences
  (user_id, theme, compact_view, default_dashboard_id, visible_sections)
VALUES
  (
    '11111111-1111-4111-8111-111111111111',
    'light',
    false,
    '22222222-2222-4222-8222-222222222222',
    '["kpis","ai","trend","categories","states","reviews","payments"]'::jsonb
  )
ON CONFLICT (user_id) DO NOTHING;
