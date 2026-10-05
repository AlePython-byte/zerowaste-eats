# Configuration

Application configuration and environment validation.

`validateEnvironment` requires a PostgreSQL `DATABASE_URL`, an HTTPS
`SUPABASE_URL`, and a non-empty `SUPABASE_PUBLISHABLE_KEY`. It defaults `NODE_ENV`
to `development` and `PORT` to `3000`. Invalid values stop startup
with messages that never include supplied values. Configuration is available
through the global Nest `ConfigService`.

Tests use placeholder environment variables and mocked database/auth access. The real
`.env` file is not loaded when `NODE_ENV` is `test`.
