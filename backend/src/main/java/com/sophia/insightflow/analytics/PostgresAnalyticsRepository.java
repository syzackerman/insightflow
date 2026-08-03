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
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresAnalyticsRepository {

  private static final String CATEGORY_EXPR =
      "initcap(replace(coalesce(t.product_category_name_english, p.product_category_name, 'unknown'), '_', ' '))";
  private static final String CATEGORY_FILTER_EXPR =
      "lower(replace(coalesce(t.product_category_name_english, p.product_category_name, 'unknown'), '_', ' '))";
  private static final String PAYMENT_EXPR = "initcap(replace(op.payment_type, '_', ' '))";
  private static final String FILTERED_ITEMS_TABLE = "filtered_analytics_items";
  private static final String FILTERED_ORDERS_TABLE = "filtered_analytics_orders";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PostgresAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public AnalyticsSummaryResponse summarize(AnalyticsFilter filter) {
    MapSqlParameterSource params = params(filter);
    createFilteredAnalyticsTables(filter, params);
    KpiSummary kpis = jdbcTemplate.queryForObject(kpiSql(), params, this::mapKpis);
    List<MonthlyRevenueMetric> monthlyRevenue =
        jdbcTemplate.query(monthlySql(), params, this::mapMonthlyRevenue);
    List<CategoryMetric> topCategories = jdbcTemplate.query(categorySql(), params, this::mapCategory);
    List<StateMetric> revenueByState = jdbcTemplate.query(stateSql(), params, this::mapState);
    List<PaymentMetric> paymentBreakdown =
        jdbcTemplate.query(paymentSql(filter), params, this::mapPayment);
    List<ReviewScoreMetric> reviewDistribution =
        jdbcTemplate.query(reviewDistributionSql(), params, this::mapReviewDistribution);

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
            SELECT min(order_purchase_timestamp)::date
            FROM insightflow.orders
            WHERE order_status = 'delivered'
              AND order_purchase_timestamp IS NOT NULL
            """,
            Map.of(),
            (rs, rowNum) -> toLocalDate(rs.getDate(1)));
    LocalDate maxDate =
        jdbcTemplate.queryForObject(
            """
            SELECT max(order_purchase_timestamp)::date
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

  private void createFilteredAnalyticsTables(AnalyticsFilter filter, MapSqlParameterSource params) {
    jdbcTemplate.getJdbcOperations().execute("SET LOCAL work_mem = '64MB'");
    jdbcTemplate.getJdbcOperations().execute("DROP TABLE IF EXISTS pg_temp." + FILTERED_ORDERS_TABLE);
    jdbcTemplate.getJdbcOperations().execute("DROP TABLE IF EXISTS pg_temp." + FILTERED_ITEMS_TABLE);
    jdbcTemplate.update(createFilteredItemsSql(filter), params);
    jdbcTemplate.getJdbcOperations().execute("ANALYZE " + FILTERED_ITEMS_TABLE);
    jdbcTemplate.getJdbcOperations().execute(createFilteredOrdersSql());
    jdbcTemplate.getJdbcOperations().execute("ANALYZE " + FILTERED_ORDERS_TABLE);
  }

  private String kpiSql() {
    return """
        WITH customer_rollup AS (
          SELECT customer_unique_id, count(*) AS order_count
          FROM filtered_analytics_orders
          GROUP BY customer_unique_id
        )
        SELECT
          coalesce(sum(order_revenue), 0) AS total_revenue,
          count(*) AS total_orders,
          coalesce(sum(order_revenue) / nullif(count(*), 0), 0) AS average_order_value,
          coalesce(
            count(DISTINCT filtered_analytics_orders.customer_unique_id) FILTER (
              WHERE filtered_analytics_orders.customer_unique_id IN (
                SELECT customer_unique_id FROM customer_rollup WHERE order_count > 1
              )
            )::numeric / nullif(count(DISTINCT filtered_analytics_orders.customer_unique_id), 0) * 100,
            0
          ) AS repeat_customer_rate,
          avg(review_score) AS average_review_score,
          coalesce(avg(CASE WHEN delivery_delayed THEN 1.0 ELSE 0.0 END) * 100, 0) AS delivery_delay_rate
        FROM filtered_analytics_orders
        """;
  }

  private String monthlySql() {
    return """
        SELECT
          month,
          coalesce(sum(order_revenue), 0) AS revenue,
          count(*) AS orders
        FROM filtered_analytics_orders
        GROUP BY month
        ORDER BY month
        """;
  }

  private String categorySql() {
    return """
        WITH category_rollup AS (
          SELECT
            product_category AS category,
            order_id,
            sum(item_revenue) AS revenue,
            count(*) AS items,
            max(review_score) AS review_score
          FROM filtered_analytics_items
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

  private String stateSql() {
    return """
        SELECT
          customer_state AS state,
          coalesce(sum(order_revenue), 0) AS revenue,
          count(*) AS orders
        FROM filtered_analytics_orders
        GROUP BY customer_state
        ORDER BY revenue DESC, state
        """;
  }

  private String paymentSql(AnalyticsFilter filter) {
    StringBuilder sql =
        new StringBuilder(
            """
        WITH filtered_orders AS (
          SELECT order_id FROM filtered_analytics_orders
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
        """);
    if (filter.paymentType() != null) {
      sql.append(
          """
          WHERE op.payment_type = :paymentTypeKey
        """);
    }
    sql.append(
        """
          GROUP BY 1
        )
        SELECT
          payment_type,
          value,
          coalesce(value / nullif(sum(value) OVER (), 0) * 100, 0) AS share
        FROM payment_totals
        ORDER BY value DESC, payment_type
        """);
    return sql.toString();
  }

  private String reviewDistributionSql() {
    return """
        WITH order_reviews AS (
          SELECT review_score
          FROM filtered_analytics_orders
          WHERE review_score IS NOT NULL
        )
        SELECT score, coalesce(count(order_reviews.review_score), 0) AS count
        FROM generate_series(1, 5) AS score
        LEFT JOIN order_reviews ON order_reviews.review_score = score
        GROUP BY score
        ORDER BY score
        """;
  }

  private String createFilteredItemsSql(AnalyticsFilter filter) {
    StringBuilder sql =
        new StringBuilder(
            """
        CREATE TEMP TABLE filtered_analytics_items ON COMMIT DROP AS
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
        """);
    appendFilterClauses(sql, filter);
    return sql.toString();
  }

  private String createFilteredOrdersSql() {
    return """
        CREATE TEMP TABLE filtered_analytics_orders ON COMMIT DROP AS
        SELECT
          order_id,
          customer_unique_id,
          month,
          customer_state,
          sum(item_revenue) AS order_revenue,
          max(review_score) AS review_score,
          bool_or(delivery_delayed) AS delivery_delayed
        FROM filtered_analytics_items
        GROUP BY order_id, customer_unique_id, month, customer_state
        """;
  }

  private void appendFilterClauses(StringBuilder sql, AnalyticsFilter filter) {
    if (filter.startDate() != null) {
      sql.append("            AND o.order_purchase_timestamp >= :startDate\n");
    }
    if (filter.endDate() != null) {
      sql.append("            AND o.order_purchase_timestamp < :endExclusiveDate\n");
    }
    if (filter.state() != null) {
      sql.append("            AND c.customer_state = :stateCode\n");
    }
    if (filter.category() != null) {
      sql.append("            AND " + CATEGORY_FILTER_EXPR + " = :categoryKey\n");
    }
    if (filter.paymentType() != null) {
      sql.append(
          """
            AND EXISTS (
              SELECT 1
              FROM insightflow.order_payments op
              WHERE op.order_id = o.order_id
                AND op.payment_type = :paymentTypeKey
            )
        """);
    }
  }

  private MapSqlParameterSource params(AnalyticsFilter filter) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (filter.startDate() != null) {
      params.addValue("startDate", filter.startDate());
    }
    if (filter.endDate() != null) {
      params.addValue("endExclusiveDate", filter.endDate().plusDays(1));
    }
    if (filter.state() != null) {
      params.addValue("state", filter.state());
      params.addValue("stateCode", filter.state().toUpperCase(Locale.ROOT));
    }
    if (filter.category() != null) {
      params.addValue("category", filter.category());
      params.addValue("categoryKey", normalizeDisplayValue(filter.category()));
    }
    if (filter.paymentType() != null) {
      params.addValue("paymentType", filter.paymentType());
      params.addValue("paymentTypeKey", normalizeStorageKey(filter.paymentType()));
    }
    return params;
  }

  private String normalizeDisplayValue(String value) {
    return value.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
  }

  private String normalizeStorageKey(String value) {
    return value.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
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
