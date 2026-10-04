import { validateEnvironment } from './environment.validation';

describe('validateEnvironment', () => {
  const databaseUrl = 'postgresql://test:test@127.0.0.1:5432/test';

  it('requires DATABASE_URL without including any supplied values in errors', () => {
    for (const value of [undefined, null, '', '  ', 'private-invalid-value']) {
      expect(() => validateEnvironment({ DATABASE_URL: value })).toThrow(
        /^DATABASE_URL (is required|must be a valid PostgreSQL connection URL)/,
      );
    }
  });

  it('uses development defaults and preserves the supplied database URL', () => {
    expect(validateEnvironment({ DATABASE_URL: databaseUrl })).toEqual({
      DATABASE_URL: databaseUrl,
      NODE_ENV: 'development',
      PORT: 3000,
    });
  });

  it('converts PORT to a number and accepts a configured environment', () => {
    expect(
      validateEnvironment({
        DATABASE_URL: databaseUrl,
        NODE_ENV: 'production',
        PORT: '8080',
      }),
    ).toMatchObject({ NODE_ENV: 'production', PORT: 8080 });
  });

  it.each(['', 'abc', '3.5', '1e3', 0, 65536, true])(
    'rejects invalid PORT %s',
    (port) => {
      expect(() =>
        validateEnvironment({ DATABASE_URL: databaseUrl, PORT: port }),
      ).toThrow('PORT must be an integer between 1 and 65535.');
    },
  );

  it('rejects invalid NODE_ENV without reflecting its value', () => {
    expect(() =>
      validateEnvironment({
        DATABASE_URL: databaseUrl,
        NODE_ENV: 'private-value',
      }),
    ).toThrow('NODE_ENV must be development, test, or production.');
  });
});
