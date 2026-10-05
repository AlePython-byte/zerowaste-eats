export interface EnvironmentVariables {
  DATABASE_URL: string;
  SUPABASE_URL: string;
  SUPABASE_PUBLISHABLE_KEY: string;
  NODE_ENV: 'development' | 'test' | 'production';
  PORT: number;
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

  return {
    DATABASE_URL: databaseUrl,
    SUPABASE_URL: supabaseUrl,
    SUPABASE_PUBLISHABLE_KEY: publishableKey,
    NODE_ENV: nodeEnv,
    PORT: port,
  };
}
