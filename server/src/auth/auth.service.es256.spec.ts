import { UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AuthService } from './auth.service';

/**
 * Exercises the real Supabase SDK verification path for asymmetric (ES256) access tokens,
 * the kind a project issues once its JWT signing keys are migrated. Tokens are signed here
 * with a throwaway key; only the JWKS download is simulated, nothing leaves the process.
 */
describe('AuthService with ES256 signing keys', () => {
  // The SDK caches signing keys process-wide per project URL, so each test gets its own
  let project = 0;
  let supabaseUrl: string;
  let issuer: string;
  const userId = 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13';
  const kid = 'test-signing-key';
  const algorithm = { name: 'ECDSA', namedCurve: 'P-256' } as const;

  let signingKey: CryptoKeyPair;
  let otherKey: CryptoKeyPair;
  let publicJwk: JsonWebKey;
  let fetchMock: jest.SpyInstance;
  let service: AuthService;

  const base64url = (bytes: Uint8Array | string) =>
    Buffer.from(bytes).toString('base64url');

  async function sign(
    claims: Record<string, unknown>,
    options: { key?: CryptoKey; kid?: string } = {},
  ): Promise<string> {
    const header = base64url(
      JSON.stringify({ alg: 'ES256', typ: 'JWT', kid: options.kid ?? kid }),
    );
    const payload = base64url(JSON.stringify(claims));
    const signature = await crypto.subtle.sign(
      { name: 'ECDSA', hash: 'SHA-256' },
      options.key ?? signingKey.privateKey,
      new TextEncoder().encode(`${header}.${payload}`),
    );
    return `${header}.${payload}.${base64url(new Uint8Array(signature))}`;
  }

  const validClaims = () => {
    const now = Math.floor(Date.now() / 1000);
    return {
      sub: userId,
      email: 'customer@example.test',
      iss: issuer,
      aud: 'authenticated',
      role: 'authenticated',
      iat: now,
      exp: now + 3600,
    };
  };

  beforeAll(async () => {
    signingKey = await crypto.subtle.generateKey(algorithm, true, [
      'sign',
      'verify',
    ]);
    otherKey = await crypto.subtle.generateKey(algorithm, true, [
      'sign',
      'verify',
    ]);
    publicJwk = {
      ...(await crypto.subtle.exportKey('jwk', signingKey.publicKey)),
      kid,
      alg: 'ES256',
      use: 'sig',
      key_ops: ['verify'],
    } as JsonWebKey;
  });

  beforeEach(() => {
    project += 1;
    supabaseUrl = `https://project-${project}.example.invalid`;
    issuer = `${supabaseUrl}/auth/v1`;
    fetchMock = jest
      .spyOn(globalThis, 'fetch')
      .mockImplementation(async (input) => {
        const url = typeof input === 'string' ? input : input.toString();
        if (url === `${issuer}/.well-known/jwks.json`) {
          return new Response(JSON.stringify({ keys: [publicJwk] }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          });
        }
        // Any other call (e.g. the getUser fallback) is refused like a revoked session
        return new Response(JSON.stringify({ message: 'invalid JWT' }), {
          status: 401,
          headers: { 'Content-Type': 'application/json' },
        });
      });
    service = new AuthService(
      new ConfigService({
        SUPABASE_URL: supabaseUrl,
        SUPABASE_PUBLISHABLE_KEY: 'test-publishable-placeholder',
      }),
    );
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const calledUrls = () => fetchMock.mock.calls.map(([input]) => String(input));

  it('accepts a valid token by checking its signature locally against the JWKS', async () => {
    await expect(
      service.verifyAccessToken(await sign(validClaims())),
    ).resolves.toEqual({ id: userId, email: 'customer@example.test' });
    // Only the public keys are fetched: no per-request round trip to the auth server
    expect(calledUrls()).toEqual([`${issuer}/.well-known/jwks.json`]);
  });

  it('caches the signing keys across requests', async () => {
    await service.verifyAccessToken(await sign(validClaims()));
    await service.verifyAccessToken(await sign(validClaims()));
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('picks up a rotated signing key without a restart', async () => {
    await service.verifyAccessToken(await sign(validClaims()));
    const original = publicJwk;
    publicJwk = {
      ...(await crypto.subtle.exportKey('jwk', otherKey.publicKey)),
      kid: 'rotated-key',
      alg: 'ES256',
      use: 'sig',
      key_ops: ['verify'],
    } as JsonWebKey;
    try {
      await expect(
        service.verifyAccessToken(
          await sign(validClaims(), {
            key: otherKey.privateKey,
            kid: 'rotated-key',
          }),
        ),
      ).resolves.toMatchObject({ id: userId });
      expect(fetchMock).toHaveBeenCalledTimes(2);
    } finally {
      publicJwk = original;
    }
  });

  it('rejects a token signed by another key under the same key id', async () => {
    await expect(
      service.verifyAccessToken(
        await sign(validClaims(), { key: otherKey.privateKey }),
      ),
    ).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('rejects a token whose claims were altered after signing', async () => {
    const [header, , signature] = (await sign(validClaims())).split('.');
    const forged = base64url(
      JSON.stringify({
        ...validClaims(),
        sub: 'ce1a20d6-bf49-48ef-8bd4-782fc2e3dc01',
      }),
    );
    await expect(
      service.verifyAccessToken(`${header}.${forged}.${signature}`),
    ).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('rejects an expired token', async () => {
    const now = Math.floor(Date.now() / 1000);
    await expect(
      service.verifyAccessToken(
        await sign({ ...validClaims(), iat: now - 7200, exp: now - 3600 }),
      ),
    ).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('rejects a correctly signed token from another issuer', async () => {
    await expect(
      service.verifyAccessToken(
        await sign({
          ...validClaims(),
          iss: 'https://other.example.invalid/auth/v1',
        }),
      ),
    ).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('rejects an unknown key id when the auth server does not vouch for the token', async () => {
    await expect(
      service.verifyAccessToken(
        await sign(validClaims(), { key: otherKey.privateKey, kid: 'unknown' }),
      ),
    ).rejects.toBeInstanceOf(UnauthorizedException);
  });
});
