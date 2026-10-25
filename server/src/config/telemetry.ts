export interface ObserveCredentials {
  appKey: string;
  appSecret: string;
}

/**
 * The telemetry credentials, or null when either is missing. Telemetry leaves the server
 * only with real credentials: without them nothing is instrumented or sent anywhere.
 */
export function readObserveCredentials(
  env: Record<string, string | undefined>,
): ObserveCredentials | null {
  const appKey = env.OBSERVE_APP_KEY?.trim();
  const appSecret = env.OBSERVE_APP_SECRET?.trim();
  return appKey && appSecret ? { appKey, appSecret } : null;
}
