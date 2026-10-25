export interface EnvironmentVariables {
  DATABASE_URL: string;
  SUPABASE_URL: string;
  SUPABASE_PUBLISHABLE_KEY: string;
  NODE_ENV: 'development' | 'test' | 'production';
  PORT: number;
  /** Absent means offer suggestions run in simulated mode. */
  ANTHROPIC_API_KEY?: string;
  AI_MODEL: string;
  /** Offer suggestions each business may request per day. */
  AI_DAILY_LIMIT: number;
  /** Browser origins allowed by CORS. Empty disables CORS: the mobile app does not need it. */
  CORS_ORIGINS: string[];
  /** Requests per minute allowed from one client before it is throttled. */
  RATE_LIMIT_PER_MINUTE: number;
  /** Telemetry credentials; both or neither. Without them telemetry stays off. */
  OBSERVE_APP_KEY?: string;
  OBSERVE_APP_SECRET?: string;
}

const DEFAULT_AI_MODEL = 'claude-haiku-5-5';
const DEFAULT_AI_DAILY_LIMIT = 20;
const DEFAULT_RATE_LIMIT_PER_MINUTE = 60;

/** Parses a positive whole number within bounds; errors never include the value. */
function parseBoundedInteger(
  name: string,
  value: unknown,
  fallback: number,
  max: number,
): number {
  if (value === undefined) {
    return fallback;
  }
  const number = Number(value);
  if (
    !['string', 'number'].includes(typeof value) ||
    !/^\d+$/.test(String(value)) ||
    !Number.isInteger(number) ||
    number < 1 ||
    number > max
  ) {
    throw new Error(`${name} must be an integer between 1 and ${max}.`);
  }
  return number;
}

function parseCorsOrigins(value: unknown): string[] {
  if (
    value === undefined ||
    (typeof value === 'string' && value.trim() === '')
  ) {
    return [];
  }
  if (typeof value !== 'string') {
    throw new Error('CORS_ORIGINS must be a comma-separated list of origins.');
  }
  return value.split(',').map((entry) => {
    const origin = entry.trim();
    try {
      const url = new URL(origin);
      // An origin is scheme, host and port only: no path, query or credentials
      if (
        !['http:', 'https:'].includes(url.protocol) ||
        url.origin !== origin ||
        url.username ||
        url.password
      ) {
        throw new Error();
      }
    } catch {
      throw new Error(
        'CORS_ORIGINS must be a comma-separated list of origins.',
      );
    }
    return origin;
  });
}

export function validateEnvironment(
  config: Record<string, unknown>,
): EnvironmentVariables {
  const databaseUrl = config.DATABASE_URL;
  if (typeof databaseUrl !== 'string' || databaseUrl.trim() === '') {
    throw new Error(
      'DATABASE_URL is required. Configure it before starting the application.',
    );
  }

  try {
    const url = new URL(databaseUrl);
    if (!['postgresql:', 'postgres:'].includes(url.protocol) || !url.hostname) {
      throw new Error();
    }
  } catch {
    throw new Error('DATABASE_URL must be a valid PostgreSQL connection URL.');
  }

  const supabaseUrl = config.SUPABASE_URL;
  if (typeof supabaseUrl !== 'string' || supabaseUrl.trim() === '') {
    throw new Error('SUPABASE_URL is required.');
  }
  try {
    const url = new URL(supabaseUrl);
    if (
      url.protocol !== 'https:' ||
      !url.hostname ||
      url.username ||
      url.password
    ) {
      throw new Error();
    }
  } catch {
    throw new Error(
      'SUPABASE_URL must be a valid HTTPS URL without credentials.',
    );
  }

  const publishableKey = config.SUPABASE_PUBLISHABLE_KEY;
  if (typeof publishableKey !== 'string' || publishableKey.trim() === '') {
    throw new Error('SUPABASE_PUBLISHABLE_KEY is required.');
  }

  const nodeEnv = config.NODE_ENV ?? 'development';
  if (
    nodeEnv !== 'development' &&
    nodeEnv !== 'test' &&
    nodeEnv !== 'production'
  ) {
    throw new Error('NODE_ENV must be development, test, or production.');
  }

  const portValue = config.PORT ?? 3000;
  const port = Number(portValue);
  if (
    !['string', 'number'].includes(typeof portValue) ||
    !/^\d+$/.test(String(portValue)) ||
    !Number.isInteger(port) ||
    port < 1 ||
    port > 65535
  ) {
    throw new Error('PORT must be an integer between 1 and 65535.');
  }

  const apiKey = config.ANTHROPIC_API_KEY;
  if (apiKey !== undefined && typeof apiKey !== 'string') {
    throw new Error('ANTHROPIC_API_KEY must be text.');
  }

  const aiModel = config.AI_MODEL ?? DEFAULT_AI_MODEL;
  if (typeof aiModel !== 'string' || !/^[a-z0-9][a-z0-9.-]*$/.test(aiModel)) {
    throw new Error('AI_MODEL must be a model identifier.');
  }

  const observeKey = config.OBSERVE_APP_KEY;
  const observeSecret = config.OBSERVE_APP_SECRET;
  const hasText = (value: unknown): value is string =>
    typeof value === 'string' && value.trim() !== '';
  const isBlank = (value: unknown) =>
    value === undefined || (typeof value === 'string' && value.trim() === '');
  if (
    !(hasText(observeKey) && hasText(observeSecret)) &&
    !(isBlank(observeKey) && isBlank(observeSecret))
  ) {
    throw new Error(
      'OBSERVE_APP_KEY and OBSERVE_APP_SECRET must be set together.',
    );
  }

  return {
    DATABASE_URL: databaseUrl,
    SUPABASE_URL: supabaseUrl,
    SUPABASE_PUBLISHABLE_KEY: publishableKey,
    NODE_ENV: nodeEnv,
    PORT: port,
    // A blank key counts as no key
    ...(typeof apiKey === 'string' && apiKey.trim() !== ''
      ? { ANTHROPIC_API_KEY: apiKey.trim() }
      : {}),
    AI_MODEL: aiModel,
    AI_DAILY_LIMIT: parseBoundedInteger(
      'AI_DAILY_LIMIT',
      config.AI_DAILY_LIMIT,
      DEFAULT_AI_DAILY_LIMIT,
      1000,
    ),
    CORS_ORIGINS: parseCorsOrigins(config.CORS_ORIGINS),
    RATE_LIMIT_PER_MINUTE: parseBoundedInteger(
      'RATE_LIMIT_PER_MINUTE',
      config.RATE_LIMIT_PER_MINUTE,
      DEFAULT_RATE_LIMIT_PER_MINUTE,
      10000,
    ),
    ...(hasText(observeKey) && hasText(observeSecret)
      ? {
          OBSERVE_APP_KEY: observeKey.trim(),
          OBSERVE_APP_SECRET: observeSecret.trim(),
        }
      : {}),
  };
}
