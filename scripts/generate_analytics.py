#!/usr/bin/env python3
"""Generate InsightFlow dashboard aggregates from the Olist CSV files."""

from __future__ import annotations

import csv
import json
from collections import Counter, defaultdict
from datetime import UTC, datetime
from pathlib import Path
from statistics import mean


ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "data"
OUT_PATHS = [
    ROOT / "backend" / "src" / "main" / "resources" / "analytics-summary.json",
    ROOT / "database" / "analytics-summary.json",
]
FACT_PATHS = [
    ROOT / "backend" / "src" / "main" / "resources" / "analytics-facts.json",
    ROOT / "database" / "analytics-facts.json",
]


def read_csv(name: str):
    with (DATA_DIR / name).open(newline="", encoding="utf-8-sig") as handle:
        yield from csv.DictReader(handle)


def parse_dt(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.strptime(value, "%Y-%m-%d %H:%M:%S")


def money(value: float) -> float:
    return round(value, 2)


def pct(value: float) -> float:
    return round(value * 100, 1)


def pretty(value: str) -> str:
    return value.replace("_", " ").title()


def load_dimensions():
    customers = {}
    for row in read_csv("olist_customers_dataset.csv"):
        customers[row["customer_id"]] = {
            "uniqueCustomerId": row["customer_unique_id"],
            "city": row["customer_city"],
            "state": row["customer_state"],
        }

    products = {}
    for row in read_csv("olist_products_dataset.csv"):
        products[row["product_id"]] = {
            "category": row["product_category_name"] or "unknown",
        }

    translations = {}
    for row in read_csv("product_category_name_translation.csv"):
        translations[row["product_category_name"]] = row["product_category_name_english"]

    return customers, products, translations


def build_facts():
    customers, products, translations = load_dimensions()

    orders = {}
    delivered_order_ids = set()
    for row in read_csv("olist_orders_dataset.csv"):
        purchase_at = parse_dt(row["order_purchase_timestamp"])
        delivered_at = parse_dt(row["order_delivered_customer_date"])
        estimated_at = parse_dt(row["order_estimated_delivery_date"])
        is_delivered = row["order_status"] == "delivered"
        customer = customers.get(row["customer_id"], {})

        orders[row["order_id"]] = {
            "customerId": row["customer_id"],
            "customerUniqueId": customer.get("uniqueCustomerId", row["customer_id"]),
            "customerState": customer.get("state", "unknown"),
            "purchaseDate": purchase_at.date().isoformat() if purchase_at else None,
            "month": purchase_at.strftime("%Y-%m") if purchase_at else "unknown",
            "isDelivered": is_delivered,
            "deliveryDelayed": bool(
                is_delivered and delivered_at and estimated_at and delivered_at > estimated_at
            ),
        }
        if is_delivered:
            delivered_order_ids.add(row["order_id"])

    review_by_order = {}
    review_distribution = Counter()
    for row in read_csv("olist_order_reviews_dataset.csv"):
        order_id = row["order_id"]
        if order_id in delivered_order_ids and row["review_score"]:
            score = int(row["review_score"])
            review_by_order[order_id] = score
            review_distribution[str(score)] += 1

    payments = []
    payment_types_by_order = defaultdict(set)
    for row in read_csv("olist_order_payments_dataset.csv"):
        order_id = row["order_id"]
        if order_id not in delivered_order_ids:
            continue
        payment_type = pretty(row["payment_type"])
        payment_types_by_order[order_id].add(payment_type)
        payments.append(
            {
                "orderId": order_id,
                "paymentType": payment_type,
                "paymentValue": money(float(row["payment_value"])),
            }
        )

    items = []
    for row in read_csv("olist_order_items_dataset.csv"):
        order = orders.get(row["order_id"])
        if not order or not order["isDelivered"] or not order["purchaseDate"]:
            continue

        product = products.get(row["product_id"], {"category": "unknown"})
        raw_category = product["category"]
        category = pretty(translations.get(raw_category, raw_category))

        items.append(
            {
                "orderId": row["order_id"],
                "customerUniqueId": order["customerUniqueId"],
                "purchaseDate": order["purchaseDate"],
                "month": order["month"],
                "customerState": order["customerState"],
                "productCategory": category,
                "paymentTypes": sorted(payment_types_by_order[row["order_id"]]),
                "itemRevenue": money(float(row["price"])),
                "freightValue": money(float(row["freight_value"])),
                "reviewScore": review_by_order.get(row["order_id"]),
                "deliveryDelayed": order["deliveryDelayed"],
            }
        )

    return {
        "generatedAt": datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source": "Brazilian Olist ecommerce public dataset",
        "items": items,
        "payments": payments,
    }


def build_summary():
    customers, products, translations = load_dimensions()

    orders = {}
    delivered_order_ids = set()
    delivery_days = []
    on_time = 0
    delivery_checked = 0

    for row in read_csv("olist_orders_dataset.csv"):
        purchase_at = parse_dt(row["order_purchase_timestamp"])
        delivered_at = parse_dt(row["order_delivered_customer_date"])
        estimated_at = parse_dt(row["order_estimated_delivery_date"])
        month = purchase_at.strftime("%Y-%m") if purchase_at else "unknown"
        is_delivered = row["order_status"] == "delivered"

        orders[row["order_id"]] = {
            "customerId": row["customer_id"],
            "status": row["order_status"],
            "month": month,
            "isDelivered": is_delivered,
        }

        if is_delivered:
            delivered_order_ids.add(row["order_id"])

        if is_delivered and purchase_at and delivered_at:
            delivery_days.append((delivered_at - purchase_at).total_seconds() / 86400)

        if is_delivered and delivered_at and estimated_at:
            delivery_checked += 1
            if delivered_at <= estimated_at:
                on_time += 1

    reviews_by_order = defaultdict(list)
    review_distribution = Counter()
    for row in read_csv("olist_order_reviews_dataset.csv"):
        order_id = row["order_id"]
        if order_id in delivered_order_ids and row["review_score"]:
            score = int(row["review_score"])
            reviews_by_order[order_id].append(score)
            review_distribution[str(score)] += 1

    payment_mix = defaultdict(float)
    for row in read_csv("olist_order_payments_dataset.csv"):
        if row["order_id"] in delivered_order_ids:
            payment_mix[row["payment_type"]] += float(row["payment_value"])

    monthly = defaultdict(lambda: {"revenue": 0.0, "orders": set(), "items": 0})
    categories = defaultdict(lambda: {"revenue": 0.0, "orders": set(), "items": 0, "reviews": []})
    states = defaultdict(lambda: {"revenue": 0.0, "orders": set()})
    total_revenue = 0.0
    total_items = 0
    customer_ids = set()

    for row in read_csv("olist_order_items_dataset.csv"):
        order = orders.get(row["order_id"])
        if not order or not order["isDelivered"]:
            continue

        price = float(row["price"])
        product = products.get(row["product_id"], {"category": "unknown"})
        raw_category = product["category"]
        category = translations.get(raw_category, raw_category).replace("_", " ").title()
        customer = customers.get(order["customerId"], {})
        state = customer.get("state", "unknown")

        total_revenue += price
        total_items += 1
        customer_ids.add(customer.get("uniqueCustomerId", order["customerId"]))

        monthly[order["month"]]["revenue"] += price
        monthly[order["month"]]["orders"].add(row["order_id"])
        monthly[order["month"]]["items"] += 1

        categories[category]["revenue"] += price
        categories[category]["orders"].add(row["order_id"])
        categories[category]["items"] += 1
        categories[category]["reviews"].extend(reviews_by_order.get(row["order_id"], []))

        states[state]["revenue"] += price
        states[state]["orders"].add(row["order_id"])

    delivered_orders = len(delivered_order_ids)
    avg_review_score = mean(
        score for scores in reviews_by_order.values() for score in scores
    )

    summary = {
        "generatedAt": datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source": "Brazilian Olist ecommerce public dataset",
        "kpis": {
            "deliveredOrders": delivered_orders,
            "uniqueCustomers": len(customer_ids),
            "itemsSold": total_items,
            "itemRevenue": money(total_revenue),
            "averageOrderValue": money(total_revenue / delivered_orders),
            "averageReviewScore": round(avg_review_score, 2),
            "onTimeDeliveryRate": pct(on_time / delivery_checked),
            "averageDeliveryDays": round(mean(delivery_days), 1),
        },
        "monthlySales": [
            {
                "month": month,
                "revenue": money(values["revenue"]),
                "orders": len(values["orders"]),
                "items": values["items"],
            }
            for month, values in sorted(monthly.items())
        ],
        "categoryPerformance": [
            {
                "category": category,
                "revenue": money(values["revenue"]),
                "orders": len(values["orders"]),
                "items": values["items"],
                "averageReviewScore": round(mean(values["reviews"]), 2)
                if values["reviews"]
                else None,
            }
            for category, values in sorted(
                categories.items(), key=lambda item: item[1]["revenue"], reverse=True
            )[:12]
        ],
        "statePerformance": [
            {
                "state": state,
                "revenue": money(values["revenue"]),
                "orders": len(values["orders"]),
            }
            for state, values in sorted(
                states.items(), key=lambda item: item[1]["revenue"], reverse=True
            )[:12]
        ],
        "paymentMix": [
            {"type": payment_type.replace("_", " ").title(), "value": money(value)}
            for payment_type, value in sorted(
                payment_mix.items(), key=lambda item: item[1], reverse=True
            )
        ],
        "reviewDistribution": [
            {"score": score, "count": review_distribution[str(score)]}
            for score in range(1, 6)
        ],
        "businessQuestions": [
            "Which categories create the most revenue?",
            "Where are the highest-value customers located?",
            "How strongly does delivery performance support customer satisfaction?",
            "Which payment methods account for the largest sales volume?",
        ],
    }

    return summary


def main():
    summary = build_summary()
    for path in OUT_PATHS:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    facts = build_facts()
    for path in FACT_PATHS:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(facts, separators=(",", ":")), encoding="utf-8")

    print(
        f"Generated analytics summary in {len(OUT_PATHS)} locations "
        f"and analytics facts in {len(FACT_PATHS)} locations"
    )


if __name__ == "__main__":
    main()
