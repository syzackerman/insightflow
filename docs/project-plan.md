# InsightFlow Project Plan

## Goal

InsightFlow is a business analysis project that turns the Olist ecommerce dataset into a dashboard and API for answering practical sales, customer, logistics, and product questions.

## Core Business Questions

- Which product categories create the most delivered revenue?
- Which customer states have the highest sales concentration?
- How are revenue and order volume changing month over month?
- How do delivery speed and on-time performance relate to customer reviews?
- Which payment methods represent the largest share of payment value?

## First Demo Scope

- Load the CSV data with `scripts/generate_analytics.py`.
- Generate reusable summary JSON for the frontend, backend, and database folder.
- Present KPIs and charts in `frontend/index.html`.
- Serve the same summary from Spring Boot at `/api/analytics/summary` once Maven is available.

## Recommended Next Iterations

1. Add a Postgres import script using the tables in `database/schema.sql`.
2. Replace the JSON-backed Spring service with repository-backed query services.
3. Add filters for date range, category, and customer state.
4. Add a business insights page with written recommendations from the dashboard results.
5. Add unit tests for the analytics calculations and API response shape.
