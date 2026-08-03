# InsightFlow Benchmark Results

These results were collected in a local development environment. They are useful for portfolio context, but they are not production performance guarantees.

## Test Environment

- Environment: local development machine.
- Dataset: Brazilian Olist ecommerce dataset.
- Data scope: full Olist dataset used by `scripts/generate_analytics.py`.
- Application scope: 100K+ real-world ecommerce transactions.
- API endpoint: `GET /api/analytics/summary?state=SP`.
- Notes: exact hardware, JVM warm-up state, and local system load were not recorded.

## Raw Benchmark Summary

| Benchmark | Result |
| --- | --- |
| Filtered analytics API | 0.171351 seconds average, approximately 171 ms, across 20 local requests |
| Full analytics generation | 7.775 seconds total, approximately 7.8 seconds |
| Backend tests | 20 tests, 0 failures, 0 errors; Maven build completed successfully |
| Registration verification | Succeeded and returned a JWT; approximately 1.20 seconds |
| Login verification | Succeeded and returned a JWT; approximately 136 ms |

## Exact Commands

API benchmark:

```bash
for i in {1..20}; do
  curl -s -o /dev/null \
    -w "%{time_total}\n" \
    "http://localhost:8080/api/analytics/summary?state=SP"
done > api-times.txt

awk '{sum += $1} END {print "Average:", sum/NR, "seconds"}' api-times.txt
```

Analytics generation:

```bash
time python3 scripts/generate_analytics.py
```

Backend tests:

```bash
cd backend
./mvnw test
```

## Methodology

The API benchmark used 20 identical local `curl` requests against the filtered analytics endpoint and averaged `time_total` with `awk`. Analytics generation was measured with the Unix `time` command while processing the full Olist dataset. Backend test results come from the Maven test suite.

Authentication was verified separately by registering and logging in successfully, with each flow returning a JWT.

## Limitations

- Results are from local testing and are not production guarantees.
- API latency may vary by hardware, JVM warm-up, analytics mode, database state, and local system load.
- The API benchmark uses one repeated filtered request and does not measure concurrent traffic.
- Analytics generation timing depends on CSV file location, disk speed, Python runtime, and dataset state.
- No model training or fine-tuning is performed by the hybrid analytics assistant.
