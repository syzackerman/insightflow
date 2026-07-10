package com.sophia.insightflow.analytics;

import com.sophia.insightflow.analytics.dto.AnalyticsFilter;
import com.sophia.insightflow.analytics.dto.AnalyticsSummaryResponse;
import com.sophia.insightflow.analytics.dto.CategoryMetric;
import com.sophia.insightflow.analytics.dto.FilterOptions;
import com.sophia.insightflow.analytics.dto.KpiSummary;
import com.sophia.insightflow.analytics.dto.MonthlyRevenueMetric;
import com.sophia.insightflow.analytics.dto.PaymentMetric;
import com.sophia.insightflow.analytics.dto.ReviewScoreMetric;
import com.sophia.insightflow.analytics.dto.StateMetric;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresAnalyticsRepository {

  private static final String CATEGORY_EXPR =
      "initcap(replace(coalesce(t.product_category_name_english, p.product_category_name, 'unknown'), '_', ' '))";
  private static final String PAYMENT_EXPR = "initcap(replace(op.payment_type, '_', ' '))";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PostgresAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public AnalyticsSummaryResponse summarize(AnalyticsFilter filter) {
    MapSqlParameterSource params = params(filter);
    KpiSummary kpis = jdbcTemplate.queryForObject(kpiSql(filter), params, this::mapKpis);
    List<MonthlyRevenueMetric> monthlyRevenue =
        jdbcTemplate.query(monthlySql(filter), params, this::mapMonthlyRevenue);
    List<CategoryMetric> topCategories =
        jdbcTemplate.query(categorySql(filter), params, this::mapCategory);
    List<StateMetric> revenueByState =
        jdbcTemplate.query(stateSql(filter), params, this::mapState);
    List<PaymentMetric> paymentBreakdown =
        jdbcTemplate.query(paymentSql(filter), params, this::mapPayment);
    List<ReviewScoreMetric> reviewDistribution =
        jdbcTemplate.query(reviewDistributionSql(filter), params, this::mapReviewDistribution);

    return new AnalyticsSummaryResponse(
        Instant.now().toString(),
        "PostgreSQL-backed Brazilian Olist ecommerce dataset",
        filter,
        filterOptions(),
        kpis,
        monthlyRevenue,
        topCategories,
        revenueByState,
        paymentBreakdown,
        reviewDistribution,
        kpis.totalOrders() == 0);
  }

  public FilterOptions filterOptions() {
    LocalDate minDate =
        jdbcTemplate.queryForObject(
            """
            SELECT min(order_purchase_timestamp::date)
            FROM insightflow.orders
            WHERE order_status = 'delivered'
              AND order_purchase_timestamp IS NOT NULL
            """,
            Map.of(),
            (rs, rowNum) -> toLocalDate(rs.getDate(1)));
    LocalDate maxDate =
        jdbcTemplate.queryForObject(
            """
            SELECT max(order_purchase_timestamp::date)
            FROM insightflow.orders
            WHERE order_status = 'delivered'
              AND order_purchase_timestamp IS NOT NULL
            """,
            Map.of(),
            (rs, rowNum) -> toLocalDate(rs.getDate(1)));
    List<String> states =
        jdbcTemplate.queryForList(
            """
            SELECT DISTINCT customer_state
            FROM insightflow.customers
            WHERE customer_state IS NOT NULL
            ORDER BY customer_state
            """,
            Map.of(),
            String.class);
    List<String> categories =
        jdbcTemplate.queryForList(
            """
            SELECT DISTINCT initcap(replace(coalesce(t.product_category_name_english, p.product_category_name, 'unknown'), '_', ' '))
            FROM insightflow.products p
            LEFT JOIN insightflow.product_category_translation t
              ON t.product_category_name = p.product_category_name
            ORDER BY 1
            """,
            Map.of(),
            String.class);
    List<String> paymentTypes =
        jdbcTemplate.queryForList(
            """
            SELECT DISTINCT initcap(replace(payment_type, '_', ' '))
            FROM insightflow.order_payments
            WHERE payment_type IS NOT NULL
            ORDER BY 1
            """,
            Map.of(),
            String.class);
    return new FilterOptions(minDate, maxDate, states, categories, paymentTypes);
  }

  private String kpiSql(AnalyticsFilter filter) {
    return filteredItemsCte(filter)
        + """
        , order_rollup AS (
          SELECT
            order_id,
            customer_unique_id,
            sum(item_revenue) AS order_revenue,
            max(review_score) AS review_score,
            bool_or(delivery_delayed) AS delivery_delayed
          FROM filtered_items
          GROUP BY order_id, customer_unique_id
        ),
        customer_rollup AS (
          SELECT customer_unique_id, count(*) AS order_count
          FROM order_rollup
          GROUP BY customer_unique_id
        )
        SELECT
          coalesce(sum(order_revenue), 0) AS total_revenue,
          count(*) AS total_orders,
          coalesce(sum(order_revenue) / nullif(count(*), 0), 0) AS average_order_value,
          coalesce(
            count(DISTINCT order_rollup.customer_unique_id) FILTER (
              WHERE order_rollup.customer_unique_id IN (
                SELECT customer_unique_id FROM customer_rollup WHERE order_count > 1
              )
            )::numeric / nullif(count(DISTINCT order_rollup.customer_unique_id), 0) * 100,
            0
          ) AS repeat_customer_rate,
          avg(review_score) AS average_review_score,
          coalesce(avg(CASE WHEN delivery_delayed THEN 1.0 ELSE 0.0 END) * 100, 0) AS delivery_delay_rate
        FROM order_rollup
        """;
  }

  private String monthlySql(AnalyticsFilter filter) {
    return filteredItemsCte(filter)
        + """
        SELECT
          month,
          coalesce(sum(item_revenue), 0) AS revenue,
          count(DISTINCT order_id) AS orders
        FROM filtered_items
        GROUP BY month
        ORDER BY month
        """;
  }

  private String categorySql(AnalyticsFilter filter) {
    return filteredItemsCte(filter)
        + """
        , category_rollup AS (
          SELECT
            product_category AS category,
            order_id,
            sum(item_revenue) AS revenue,
            count(*) AS items,
            max(review_score) AS review_score
          FROM filtered_items
          GROUP BY product_category, order_id
        )
        SELECT
          category,
          coalesce(sum(revenue), 0) AS revenue,
          count(DISTINCT order_id) AS orders,
          sum(items) AS items,
          avg(review_score) AS average_review_score
        FROM category_rollup
        GROUP BY category
        ORDER BY revenue DESC, category
        LIMIT 10
        """;
  }

  private String stateSql(AnalyticsFilter filter) {
    return filteredItemsCte(filter)
        + """
        SELECT
          customer_state AS state,
          coalesce(sum(item_revenue), 0) AS revenue,
          count(DISTINCT order_id) AS orders
        FROM filtered_items
        GROUP BY customer_state
        ORDER BY revenue DESC, state
        """;
  }

  private String paymentSql(AnalyticsFilter filter) {
    return filteredItemsCte(filter)
        + """
        , filtered_orders AS (
          SELECT DISTINCT order_id FROM filtered_items
        ),
        payment_totals AS (
          SELECT
            """
        + PAYMENT_EXPR
        + """
             AS payment_type,
            sum(op.payment_value) AS value
          FROM insightflow.order_payments op
          JOIN filtered_orders fo ON fo.order_id = op.order_id
          WHERE (:paymentType IS NULL OR lower("""
        + PAYMENT_EXPR
        + """
            ) = lower(:paymentType))
          GROUP BY 1
        )
        SELECT
          payment_type,
          value,
          coalesce(value / nullif(sum(value) OVER (), 0) * 100, 0) AS share
        FROM payment_totals
        ORDER BY value DESC, payment_type
        """;
  }

  private String reviewDistributionSql(AnalyticsFilter filter) {
    return filteredItemsCte(filter)
        + """
        , order_reviews AS (
          SELECT order_id, max(review_score) AS review_score
          FROM filtered_items
          WHERE review_score IS NOT NULL
          GROUP BY order_id
        )
        SELECT score, coalesce(count(order_reviews.review_score), 0) AS count
        FROM generate_series(1, 5) AS score
        LEFT JOIN order_reviews ON order_reviews.review_score = score
        GROUP BY score
        ORDER BY score
        """;
  }

  private String filteredItemsCte(AnalyticsFilter filter) {
    return """
        WITH filtered_items AS (
          SELECT
            o.order_id,
            c.customer_unique_id,
            o.order_purchase_timestamp::date AS purchase_date,
            to_char(o.order_purchase_timestamp, 'YYYY-MM') AS month,
            c.customer_state,
            """
        + CATEGORY_EXPR
        + """
             AS product_category,
            oi.price AS item_revenue,
            r.review_score,
            (
              o.order_delivered_customer_date IS NOT NULL
              AND o.order_estimated_delivery_date IS NOT NULL
              AND o.order_delivered_customer_date > o.order_estimated_delivery_date
            ) AS delivery_delayed
          FROM insightflow.orders o
          JOIN insightflow.customers c ON c.customer_id = o.customer_id
          JOIN insightflow.order_items oi ON oi.order_id = o.order_id
          LEFT JOIN insightflow.products p ON p.product_id = oi.product_id
          LEFT JOIN insightflow.product_category_translation t
            ON t.product_category_name = p.product_category_name
          LEFT JOIN (
            SELECT order_id, max(review_score) AS review_score
            FROM insightflow.order_reviews
            GROUP BY order_id
          ) r ON r.order_id = o.order_id
          WHERE o.order_status = 'delivered'
            AND o.order_purchase_timestamp IS NOT NULL
            AND (:startDate IS NULL OR o.order_purchase_timestamp::date >= :startDate)
            AND (:endDate IS NULL OR o.order_purchase_timestamp::date <= :endDate)
            AND (:state IS NULL OR lower(c.customer_state) = lower(:state))
            AND (:category IS NULL OR lower("""
        + CATEGORY_EXPR
        + """
            ) = lower(:category))
            AND (:paymentType IS NULL OR EXISTS (
              SELECT 1
              FROM insightflow.order_payments op
              WHERE op.order_id = o.order_id
                AND lower("""
        + PAYMENT_EXPR
        + """
                ) = lower(:paymentType)
            ))
        )
        """;
  }

  private MapSqlParameterSource params(AnalyticsFilter filter) {
    return new MapSqlParameterSource()
        .addValue("startDate", filter.startDate())
        .addValue("endDate", filter.endDate())
        .addValue("state", filter.state())
        .addValue("category", filter.category())
        .addValue("paymentType", filter.paymentType());
  }

  private KpiSummary mapKpis(ResultSet rs, int rowNum) throws SQLException {
    return new KpiSummary(
        money(rs.getBigDecimal("total_revenue")),
        rs.getLong("total_orders"),
        money(rs.getBigDecimal("average_order_value")),
        percent(rs.getBigDecimal("repeat_customer_rate")),
        nullableDouble(rs, "average_review_score", 2),
        percent(rs.getBigDecimal("delivery_delay_rate")));
  }

  private MonthlyRevenueMetric mapMonthlyRevenue(ResultSet rs, int rowNum) throws SQLException {
    return new MonthlyRevenueMetric(
        rs.getString("month"), money(rs.getBigDecimal("revenue")), rs.getLong("orders"));
  }

  private CategoryMetric mapCategory(ResultSet rs, int rowNum) throws SQLException {
    return new CategoryMetric(
        rs.getString("category"),
        money(rs.getBigDecimal("revenue")),
        rs.getLong("orders"),
        rs.getLong("items"),
        nullableDouble(rs, "average_review_score", 2));
  }

  private StateMetric mapState(ResultSet rs, int rowNum) throws SQLException {
    return new StateMetric(
        rs.getString("state"), money(rs.getBigDecimal("revenue")), rs.getLong("orders"));
  }

  private PaymentMetric mapPayment(ResultSet rs, int rowNum) throws SQLException {
    return new PaymentMetric(
        rs.getString("payment_type"),
        money(rs.getBigDecimal("value")),
        percent(rs.getBigDecimal("share")));
  }

  private ReviewScoreMetric mapReviewDistribution(ResultSet rs, int rowNum) throws SQLException {
    return new ReviewScoreMetric(rs.getInt("score"), rs.getLong("count"));
  }

  private BigDecimal money(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
  }

  private double percent(BigDecimal value) {
    return round(value == null ? BigDecimal.ZERO : value, 1);
  }

  private Double nullableDouble(ResultSet rs, String column, int scale) throws SQLException {
    BigDecimal value = rs.getBigDecimal(column);
    if (value == null) {
      return null;
    }
    return round(value, scale);
  }

  private double round(BigDecimal value, int scale) {
    return value.setScale(scale, RoundingMode.HALF_UP).doubleValue();
  }

  private LocalDate toLocalDate(Date date) {
    return date == null ? null : date.toLocalDate();
  }
}
