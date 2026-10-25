# Configuration

Application configuration and environment validation.

`validateEnvironment` requires a PostgreSQL `DATABASE_URL`, an HTTPS
`SUPABASE_URL`, and a non-empty `SUPABASE_PUBLISHABLE_KEY`. It defaults `NODE_ENV`
to `development` and `PORT` to `3000`.

Optional settings: `ANTHROPIC_API_KEY` (absent or blank means offer suggestions run
in simulated mode), `AI_MODEL` (default `claude-haiku-5-5`), `AI_DAILY_LIMIT`
(suggestions per business per day, default 20), `CORS_ORIGINS` (comma-separated
browser origins; empty keeps CORS off, which the mobile app does not need) and
`RATE_LIMIT_PER_MINUTE` (requests per client per minute, default 60).

Telemetry runs only when both `OBSERVE_APP_KEY` and `OBSERVE_APP_SECRET` are set;
otherwise the module is not loaded and the app is not instrumented, so nothing is
sent outside the server. Invalid values stop startup
with messages that never include supplied values. Configuration is available
through the global Nest `ConfigService`.

Tests use placeholder environment variables and mocked database/auth access. The real
`.env` file is not loaded when `NODE_ENV` is `test`.
