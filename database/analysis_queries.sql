-- Monthly delivered revenue and order volume.
SELECT
  date_trunc('month', o.order_purchase_timestamp)::date AS month,
  COUNT(DISTINCT o.order_id) AS delivered_orders,
  SUM(oi.price) AS item_revenue,
  SUM(oi.freight_value) AS freight_revenue
FROM insightflow.orders o
JOIN insightflow.order_items oi ON oi.order_id = o.order_id
WHERE o.order_status = 'delivered'
GROUP BY 1
ORDER BY 1;

-- Top categories by revenue.
WITH review_rollup AS (
  SELECT order_id, MAX(review_score) AS review_score
  FROM insightflow.order_reviews
  GROUP BY order_id
)
SELECT
  COALESCE(t.product_category_name_english, p.product_category_name, 'unknown') AS category,
  COUNT(DISTINCT oi.order_id) AS orders,
  COUNT(*) AS items,
  SUM(oi.price) AS item_revenue,
  AVG(r.review_score) AS average_review_score
FROM insightflow.order_items oi
JOIN insightflow.orders o ON o.order_id = oi.order_id
LEFT JOIN insightflow.products p ON p.product_id = oi.product_id
LEFT JOIN insightflow.product_category_translation t
  ON t.product_category_name = p.product_category_name
LEFT JOIN review_rollup r ON r.order_id = oi.order_id
WHERE o.order_status = 'delivered'
GROUP BY 1
ORDER BY item_revenue DESC
LIMIT 12;

-- State performance.
SELECT
  c.customer_state,
  COUNT(DISTINCT o.order_id) AS delivered_orders,
  COUNT(DISTINCT c.customer_unique_id) AS unique_customers,
  SUM(oi.price) AS item_revenue
FROM insightflow.orders o
JOIN insightflow.customers c ON c.customer_id = o.customer_id
JOIN insightflow.order_items oi ON oi.order_id = o.order_id
WHERE o.order_status = 'delivered'
GROUP BY 1
ORDER BY item_revenue DESC;

-- Delivery performance and review score.
WITH review_rollup AS (
  SELECT order_id, MAX(review_score) AS review_score
  FROM insightflow.order_reviews
  GROUP BY order_id
)
SELECT
  CASE
    WHEN o.order_delivered_customer_date <= o.order_estimated_delivery_date THEN 'on_time'
    ELSE 'late'
  END AS delivery_status,
  COUNT(DISTINCT o.order_id) AS orders,
  AVG(r.review_score) AS average_review_score,
  AVG(EXTRACT(EPOCH FROM (o.order_delivered_customer_date - o.order_purchase_timestamp)) / 86400) AS average_delivery_days
FROM insightflow.orders o
LEFT JOIN review_rollup r ON r.order_id = o.order_id
WHERE o.order_status = 'delivered'
  AND o.order_delivered_customer_date IS NOT NULL
  AND o.order_estimated_delivery_date IS NOT NULL
GROUP BY 1;

-- Portfolio dashboard KPI rollup.
WITH review_rollup AS (
  SELECT order_id, MAX(review_score) AS review_score
  FROM insightflow.order_reviews
  GROUP BY order_id
),
delivered_order_items AS (
  SELECT
    o.order_id,
    c.customer_unique_id,
    oi.price,
    r.review_score,
    CASE
      WHEN o.order_delivered_customer_date > o.order_estimated_delivery_date THEN 1
      ELSE 0
    END AS delayed
  FROM insightflow.orders o
  JOIN insightflow.customers c ON c.customer_id = o.customer_id
  JOIN insightflow.order_items oi ON oi.order_id = o.order_id
  LEFT JOIN review_rollup r ON r.order_id = o.order_id
  WHERE o.order_status = 'delivered'
),
order_rollup AS (
  SELECT
    order_id,
    customer_unique_id,
    SUM(price) AS order_revenue,
    MAX(review_score) AS review_score,
    MAX(delayed) AS delayed
  FROM delivered_order_items
  GROUP BY 1, 2
),
customer_rollup AS (
  SELECT
    customer_unique_id,
    COUNT(*) AS order_count
  FROM order_rollup
  GROUP BY 1
)
SELECT
  SUM(order_revenue) AS total_revenue,
  COUNT(*) AS total_orders,
  SUM(order_revenue) / NULLIF(COUNT(*), 0) AS average_order_value,
  AVG(review_score) AS average_review_score,
  AVG(delayed) * 100 AS delivery_delay_rate,
  (
    COUNT(DISTINCT customer_unique_id) FILTER (WHERE customer_unique_id IN (
      SELECT customer_unique_id FROM customer_rollup WHERE order_count > 1
    ))::numeric
    / NULLIF(COUNT(DISTINCT customer_unique_id), 0)
  ) * 100 AS repeat_customer_rate
FROM order_rollup;

-- Payment method breakdown for delivered orders.
SELECT
  op.payment_type,
  SUM(op.payment_value) AS payment_value,
  SUM(op.payment_value) / SUM(SUM(op.payment_value)) OVER () * 100 AS share
FROM insightflow.order_payments op
JOIN insightflow.orders o ON o.order_id = op.order_id
WHERE o.order_status = 'delivered'
GROUP BY 1
ORDER BY payment_value DESC;
