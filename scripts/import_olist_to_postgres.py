#!/usr/bin/env python3
"""Import the Olist CSV dataset into PostgreSQL for InsightFlow analytics."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "data"
SCHEMA_PATH = ROOT / "database" / "schema.sql"


@dataclass(frozen=True)
class TableImport:
    table: str
    csv_file: str
    columns: tuple[str, ...]


IMPORTS = (
    TableImport(
        "customers",
        "olist_customers_dataset.csv",
        (
            "customer_id",
            "customer_unique_id",
            "customer_zip_code_prefix",
            "customer_city",
            "customer_state",
        ),
    ),
    TableImport(
        "orders",
        "olist_orders_dataset.csv",
        (
            "order_id",
            "customer_id",
            "order_status",
            "order_purchase_timestamp",
            "order_approved_at",
            "order_delivered_carrier_date",
            "order_delivered_customer_date",
            "order_estimated_delivery_date",
        ),
    ),
    TableImport(
        "products",
        "olist_products_dataset.csv",
        (
            "product_id",
            "product_category_name",
            "product_name_lenght",
            "product_description_lenght",
            "product_photos_qty",
            "product_weight_g",
            "product_length_cm",
            "product_height_cm",
            "product_width_cm",
        ),
    ),
    TableImport(
        "sellers",
        "olist_sellers_dataset.csv",
        ("seller_id", "seller_zip_code_prefix", "seller_city", "seller_state"),
    ),
    TableImport(
        "order_items",
        "olist_order_items_dataset.csv",
        (
            "order_id",
            "order_item_id",
            "product_id",
            "seller_id",
            "shipping_limit_date",
            "price",
            "freight_value",
        ),
    ),
    TableImport(
        "order_payments",
        "olist_order_payments_dataset.csv",
        (
            "order_id",
            "payment_sequential",
            "payment_type",
            "payment_installments",
            "payment_value",
        ),
    ),
    TableImport(
        "order_reviews",
        "olist_order_reviews_dataset.csv",
        (
            "review_id",
            "order_id",
            "review_score",
            "review_comment_title",
            "review_comment_message",
            "review_creation_date",
            "review_answer_timestamp",
        ),
    ),
    TableImport(
        "product_category_translation",
        "product_category_name_translation.csv",
        ("product_category_name", "product_category_name_english"),
    ),
)


TRUNCATE_SQL = """
TRUNCATE TABLE
  insightflow.order_reviews,
  insightflow.order_payments,
  insightflow.order_items,
  insightflow.orders,
  insightflow.products,
  insightflow.sellers,
  insightflow.customers,
  insightflow.product_category_translation
RESTART IDENTITY CASCADE;
"""


def parse_args() -> argparse.Namespace:
    load_env_file(ROOT / ".env")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default=os.getenv("POSTGRES_HOST", "localhost"))
    parser.add_argument("--port", default=os.getenv("POSTGRES_PORT", "5432"))
    parser.add_argument("--database", default=os.getenv("POSTGRES_DB", "insightflow"))
    parser.add_argument("--user", default=os.getenv("POSTGRES_USER", "insightflow_user"))
    parser.add_argument("--password", default=os.getenv("POSTGRES_PASSWORD", "insightflow_pass"))
    parser.add_argument("--schema-only", action="store_true", help="Create schema without loading CSVs.")
    parser.add_argument("--use-psql", action="store_true", help="Force psql-based import.")
    return parser.parse_args()


def load_env_file(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key and key not in os.environ:
            os.environ[key] = value


def validate_files() -> None:
    missing = [str(DATA_DIR / spec.csv_file) for spec in IMPORTS if not (DATA_DIR / spec.csv_file).exists()]
    if missing:
        raise SystemExit("Missing CSV files:\n" + "\n".join(missing))
    if not SCHEMA_PATH.exists():
        raise SystemExit(f"Missing schema file: {SCHEMA_PATH}")


def run_psql(args: argparse.Namespace, sql: str | None = None, file: Path | None = None) -> None:
    command = [
        "psql",
        "--host",
        args.host,
        "--port",
        str(args.port),
        "--username",
        args.user,
        "--dbname",
        args.database,
        "--set",
        "ON_ERROR_STOP=1",
    ]
    if file:
        command.extend(["--file", str(file)])
    if sql:
        command.extend(["--command", sql])

    env = os.environ.copy()
    env["PGPASSWORD"] = args.password
    subprocess.run(command, check=True, env=env)


def import_with_psql(args: argparse.Namespace) -> None:
    run_psql(args, file=SCHEMA_PATH)
    if args.schema_only:
        return
    run_psql(args, sql=TRUNCATE_SQL)
    for spec in IMPORTS:
        columns = ", ".join(spec.columns)
        csv_path = (DATA_DIR / spec.csv_file).resolve()
        copy_sql = (
            f"\\copy insightflow.{spec.table} ({columns}) "
            f"FROM '{csv_path}' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8')"
        )
        print(f"Loading {spec.csv_file} -> insightflow.{spec.table}")
        run_psql(args, sql=copy_sql)


def import_with_psycopg(args: argparse.Namespace) -> None:
    try:
        import psycopg
    except ImportError:
        psycopg = None

    try:
        import psycopg2
    except ImportError:
        psycopg2 = None

    if psycopg:
        import_with_psycopg3(args, psycopg)
        return
    if psycopg2:
        import_with_psycopg2(args, psycopg2)
        return

    raise SystemExit(
        "No PostgreSQL importer found. Install either the psql client or a Python driver:\n"
        "  python3 -m pip install 'psycopg[binary]'\n"
        "Then rerun scripts/import_olist_to_postgres.py."
    )


def import_with_psycopg3(args: argparse.Namespace, psycopg) -> None:
    conninfo = connection_string(args)
    with psycopg.connect(conninfo) as connection:
        with connection.cursor() as cursor:
            execute_sql_script(cursor, SCHEMA_PATH.read_text(encoding="utf-8"))
            if args.schema_only:
                return
            cursor.execute(TRUNCATE_SQL)
            for spec in IMPORTS:
                copy_sql = copy_statement(spec)
                print(f"Loading {spec.csv_file} -> insightflow.{spec.table}")
                with cursor.copy(copy_sql) as copy:
                    with (DATA_DIR / spec.csv_file).open("r", encoding="utf-8-sig", newline="") as handle:
                        while chunk := handle.read(1024 * 1024):
                            copy.write(chunk)


def import_with_psycopg2(args: argparse.Namespace, psycopg2) -> None:
    conninfo = connection_string(args)
    with psycopg2.connect(conninfo) as connection:
        with connection.cursor() as cursor:
            execute_sql_script(cursor, SCHEMA_PATH.read_text(encoding="utf-8"))
            if args.schema_only:
                return
            cursor.execute(TRUNCATE_SQL)
            for spec in IMPORTS:
                copy_sql = copy_statement(spec)
                print(f"Loading {spec.csv_file} -> insightflow.{spec.table}")
                with (DATA_DIR / spec.csv_file).open("r", encoding="utf-8-sig", newline="") as handle:
                    cursor.copy_expert(copy_sql, handle)


def copy_statement(spec: TableImport) -> str:
    columns = ", ".join(spec.columns)
    return f"COPY insightflow.{spec.table} ({columns}) FROM STDIN WITH (FORMAT csv, HEADER true)"


def connection_string(args: argparse.Namespace) -> str:
    return (
        f"host={args.host} port={args.port} dbname={args.database} "
        f"user={args.user} password={args.password}"
    )


def execute_sql_script(cursor, script: str) -> None:
    for statement in script.split(";"):
        statement = statement.strip()
        if statement:
            cursor.execute(statement)


def main() -> None:
    args = parse_args()
    validate_files()

    if args.use_psql or shutil.which("psql"):
        import_with_psql(args)
    else:
        import_with_psycopg(args)

    print("Olist import complete.")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
    except Exception as error:
        if error.__class__.__name__ == "OperationalError":
            sys.exit(
                "Could not connect to PostgreSQL. Check that the server is running and that "
                "POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USER, and POSTGRES_PASSWORD are correct.\n"
                f"Original error: {error}"
            )
        raise
