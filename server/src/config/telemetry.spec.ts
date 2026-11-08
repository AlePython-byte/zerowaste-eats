import { readObserveCredentials } from './telemetry';

describe('readObserveCredentials', () => {
  it('returns both credentials, trimmed, when both are set', () => {
    expect(
      readObserveCredentials({
        OBSERVE_APP_KEY: ' key-placeholder ',
        OBSERVE_APP_SECRET: ' secret-placeholder ',
      }),
    ).toEqual({ appKey: 'key-placeholder', appSecret: 'secret-placeholder' });
  });

  it.each([
    {},
    { OBSERVE_APP_KEY: 'key-placeholder' },
    { OBSERVE_APP_SECRET: 'secret-placeholder' },
    { OBSERVE_APP_KEY: '  ', OBSERVE_APP_SECRET: 'secret-placeholder' },
  ])('keeps telemetry off unless both are set (%j)', (env) => {
    expect(readObserveCredentials(env)).toBeNull();
  });
});
