import { ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { AuthService } from '../auth.service';
import type { AuthenticatedRequest } from '../interfaces/authenticated-request.interface';
import type { AuthenticatedUser } from '../interfaces/authenticated-user.interface';
import { SupabaseAuthGuard } from './supabase-auth.guard';

describe('SupabaseAuthGuard', () => {
  const user: AuthenticatedUser = {
    id: 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13',
    email: null,
  };
  const authService = {
    verifyAccessToken: jest.fn<Promise<AuthenticatedUser>, [string]>(),
  };
  const guard = new SupabaseAuthGuard(authService as unknown as AuthService);

  function createContext(authorization?: string) {
    const request = { headers: { authorization } } as AuthenticatedRequest;
    const context = {
      switchToHttp: () => ({ getRequest: () => request }),
    } as unknown as ExecutionContext;
    return { request, context };
  }

  beforeEach(() => {
    authService.verifyAccessToken.mockReset().mockResolvedValue(user);
  });

  it.each([
    undefined,
    '',
    'Bearer',
    'Bearer ',
    'Bearer   ',
    'Basic credentials',
    'Bearer first second',
    'Bearer first,second',
  ])('rejects absent or malformed authorization (%s)', async (header) => {
    const { context, request } = createContext(header);
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
    expect(authService.verifyAccessToken).not.toHaveBeenCalled();
    expect(request.user).toBeUndefined();
  });

  it('sanitizes verification failures and discards any stale user', async () => {
    const { context, request } = createContext('Bearer opaque-test-token');
    request.user = user;
    authService.verifyAccessToken.mockRejectedValue(
      new Error('private-auth-details'),
    );
    await expect(guard.canActivate(context)).rejects.toMatchObject({
      response: {
        statusCode: 401,
        error: 'No autorizado',
        message: 'Se requiere un token de acceso válido.',
      },
    });
    expect(request.user).toBeUndefined();
  });

  it.each(['Bearer', 'bearer'])(
    'attaches only a verified user using %s',
    async (scheme) => {
      const { context, request } = createContext(`${scheme} opaque-test-token`);
      await expect(guard.canActivate(context)).resolves.toBe(true);
      expect(authService.verifyAccessToken).toHaveBeenCalledWith(
        'opaque-test-token',
      );
      expect(request.user).toEqual(user);
    },
  );
});
