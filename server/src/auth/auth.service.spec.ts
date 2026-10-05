import { UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  AuthError,
  type JwtPayload,
  type SupabaseClient,
} from '@supabase/supabase-js';
import { AuthService } from './auth.service';

type ClaimsResponse = Awaited<ReturnType<SupabaseClient['auth']['getClaims']>>;

describe('AuthService', () => {
  const userId = 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13';
  const token = 'opaque-test-token';
  const claims: JwtPayload = {
    sub: userId,
    email: 'customer@example.test',
    iss: 'https://auth.example.invalid/auth/v1',
    aud: 'authenticated',
    exp: 4102444800,
    iat: 1704067200,
    role: 'authenticated',
    aal: 'aal1',
    session_id: 'f98b2203-a7de-4d31-9afc-f9dd522c8b75',
  };
  let service: AuthService;
  let getClaims: jest.SpyInstance<Promise<ClaimsResponse>>;
  let fetchMock: jest.SpyInstance;

  function verifiedResponse(
    overrides: Partial<JwtPayload> = {},
  ): ClaimsResponse {
    return {
      data: {
        claims: { ...claims, ...overrides },
        header: { alg: 'ES256', kid: 'test-key-id', typ: 'JWT' },
        signature: new Uint8Array(),
      },
      error: null,
    };
  }

  beforeEach(() => {
    fetchMock = jest
      .spyOn(globalThis, 'fetch')
      .mockRejectedValue(new Error('Network disabled in tests'));
    service = new AuthService(
      new ConfigService({
        SUPABASE_URL: 'https://auth.example.invalid',
        SUPABASE_PUBLISHABLE_KEY: 'test-publishable-placeholder',
      }),
    );
    getClaims = jest.spyOn(service['supabase'].auth, 'getClaims');
    getClaims.mockResolvedValue(verifiedResponse());
  });

  afterEach(() => {
    expect(fetchMock).not.toHaveBeenCalled();
    jest.restoreAllMocks();
  });

  it('returns only id and email from verified claims', async () => {
    await expect(service.verifyAccessToken(token)).resolves.toEqual({
      id: userId,
      email: claims.email,
    });
    expect(getClaims).toHaveBeenCalledWith(token, { allowExpired: false });
  });

  it('returns null when a verified email is absent', async () => {
    getClaims.mockResolvedValue(verifiedResponse({ email: undefined }));
    await expect(service.verifyAccessToken(token)).resolves.toEqual({
      id: userId,
      email: null,
    });
  });

  it.each(['invalid', 'expired'])(
    'sanitizes %s token verification errors',
    async () => {
      getClaims.mockResolvedValue({
        data: null,
        error: new AuthError('private-auth-details'),
      });
      const result = service.verifyAccessToken(token);
      await expect(result).rejects.toBeInstanceOf(UnauthorizedException);
      await expect(result).rejects.toMatchObject({
        response: {
          statusCode: 401,
          error: 'No autorizado',
          message: 'Se requiere un token de acceso válido.',
        },
      });
    },
  );

  it('sanitizes thrown SDK errors', async () => {
    getClaims.mockRejectedValue(new Error('private-auth-details'));
    await expect(service.verifyAccessToken(token)).rejects.toThrow(
      'Se requiere un token de acceso válido.',
    );
  });

  it('rejects missing claims', async () => {
    getClaims.mockResolvedValue({ data: null, error: null });
    await expect(service.verifyAccessToken(token)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it.each([undefined, '', 'not-a-uuid'])(
    'rejects missing or invalid sub (%s)',
    async (sub) => {
      getClaims.mockResolvedValue(verifiedResponse({ sub }));
      await expect(service.verifyAccessToken(token)).rejects.toBeInstanceOf(
        UnauthorizedException,
      );
    },
  );

  it('rejects claims from an unexpected issuer', async () => {
    getClaims.mockResolvedValue(
      verifiedResponse({ iss: 'https://other.example.invalid/auth/v1' }),
    );
    await expect(service.verifyAccessToken(token)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it('rejects an empty token without invoking the SDK', async () => {
    await expect(service.verifyAccessToken('')).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
    expect(getClaims).not.toHaveBeenCalled();
  });
});
