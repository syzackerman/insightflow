# InsightFlow Deployment Guide

This guide uses Render because it can host the Spring Boot web service and a managed PostgreSQL database from one `render.yaml` Blueprint.

## Deployment Shape

- One Docker web service serves both the frontend and backend API.
- Frontend URL and backend API URL share the same host.
- PostgreSQL stores user accounts, saved dashboards, and preferences.
- Analytics defaults to JSON mode for live demos so the app works immediately without importing the full Olist dataset into hosted Postgres.

## Render Blueprint Steps

1. Push this repository to GitHub.
2. In Render, choose **New +** -> **Blueprint**.
3. Connect the GitHub repository.
4. Render reads `render.yaml` and creates:
   - `insightflow` web service
   - `insightflow-db` PostgreSQL database
5. Leave `OPENAI_API_KEY` blank for local AI analyst mode, or provide a key for OpenAI-backed reports.
6. Deploy.

After deploy:

- Frontend: `https://<render-service>.onrender.com`
- Backend API: `https://<render-service>.onrender.com/api/health`
- Analytics API: `https://<render-service>.onrender.com/api/analytics/summary`

## Why JSON Analytics Mode In Production Blueprint?

The live portfolio demo should work immediately after deploy. `INSIGHTFLOW_ANALYTICS_MODE=json` uses the generated analytics artifact bundled with the app. PostgreSQL is still used for authentication, saved dashboards, and preferences.

To run hosted PostgreSQL analytics later:

1. Import the Olist CSV files into the Render database from a trusted machine.
2. Change `INSIGHTFLOW_ANALYTICS_MODE` to `postgres`.
3. Redeploy the service.

## Health Check

Render uses:

```text
/api/health
```

Expected response:

```json
{
  "status": "ok",
  "service": "insightflow",
  "timestamp": "..."
}
```

## Live Links Checklist

After deployment, update the README:

```text
Live Demo: https://<render-service>.onrender.com
Backend API: https://<render-service>.onrender.com/api/health
GitHub: https://github.com/<username>/<repo>
```
