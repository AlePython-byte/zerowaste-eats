# Configuration

Application configuration and environment validation.

`validateEnvironment` requires a PostgreSQL `DATABASE_URL`, defaults `NODE_ENV`
to `development`, and defaults `PORT` to `3000`. Invalid values stop startup
with messages that never include supplied values. Configuration is available
through the global Nest `ConfigService`.

Tests use placeholder environment variables and mocked database access. The real
`.env` file is not loaded when `NODE_ENV` is `test`.
