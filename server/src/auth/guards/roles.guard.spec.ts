import {
  ForbiddenException,
  UnauthorizedException,
  type ExecutionContext,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import type { DatabaseService } from '../../database/database.service';
import { UserRole } from '../../generated/prisma/enums';
import { authenticatedUser } from '../../../test/fixtures/user-profile';
import { Roles } from '../decorators/roles.decorator';
import type { AuthenticatedUser } from '../interfaces/authenticated-user.interface';
import { RolesGuard } from './roles.guard';

@Roles(UserRole.MERCHANT)
class MerchantController {
  manageOffers() {}

  @Roles(UserRole.CUSTOMER, UserRole.MERCHANT)
  anyProfile() {}
}

class OpenController {
  open() {}

  @Roles()
  misconfigured() {}
}

describe('RolesGuard', () => {
  const findUnique = jest.fn();
  const guard = new RolesGuard(new Reflector(), {
    client: { user: { findUnique } },
  } as unknown as DatabaseService);

  const contextFor = (
    controller: new () => object,
    handler: string,
    // Extra properties model whatever else might ride on the request identity
    user: AuthenticatedUser | undefined,
  ) =>
    ({
      getClass: () => controller,
      getHandler: () =>
        (controller.prototype as Record<string, unknown>)[handler],
      switchToHttp: () => ({ getRequest: () => ({ user }) }),
    }) as unknown as ExecutionContext;

  beforeEach(() => {
    findUnique.mockReset().mockResolvedValue({ role: UserRole.MERCHANT });
  });

  it('lets routes without @Roles through without touching the database', async () => {
    await expect(
      guard.canActivate(contextFor(OpenController, 'open', undefined)),
    ).resolves.toBe(true);
    expect(findUnique).not.toHaveBeenCalled();
  });

  it('allows a caller whose stored role is required, reading only that role', async () => {
    await expect(
      guard.canActivate(
        contextFor(MerchantController, 'manageOffers', authenticatedUser),
      ),
    ).resolves.toBe(true);
    expect(findUnique).toHaveBeenCalledWith({
      where: { id: authenticatedUser.id },
      select: { role: true },
    });
  });

  it('rejects a caller whose stored role is not allowed with a 403', async () => {
    findUnique.mockResolvedValue({ role: UserRole.CUSTOMER });
    await expect(
      guard.canActivate(
        contextFor(MerchantController, 'manageOffers', authenticatedUser),
      ),
    ).rejects.toMatchObject({
      response: {
        statusCode: 403,
        error: 'Prohibido',
        message: 'No tienes permiso para realizar esta acción.',
      },
    });
  });

  it('trusts the database, not a role carried on the request identity', async () => {
    findUnique.mockResolvedValue({ role: UserRole.CUSTOMER });
    const forged = {
      ...authenticatedUser,
      role: UserRole.MERCHANT,
      user_metadata: { role: UserRole.MERCHANT },
    };
    await expect(
      guard.canActivate(contextFor(MerchantController, 'manageOffers', forged)),
    ).rejects.toBeInstanceOf(ForbiddenException);
  });

  it('lets the method-level @Roles override the controller-level one', async () => {
    findUnique.mockResolvedValue({ role: UserRole.CUSTOMER });
    await expect(
      guard.canActivate(
        contextFor(MerchantController, 'anyProfile', authenticatedUser),
      ),
    ).resolves.toBe(true);
  });

  it('asks a caller without a profile to complete it', async () => {
    findUnique.mockResolvedValue(null);
    await expect(
      guard.canActivate(
        contextFor(MerchantController, 'manageOffers', authenticatedUser),
      ),
    ).rejects.toMatchObject({
      response: {
        statusCode: 403,
        error: 'Prohibido',
        message: 'Completa tu perfil para continuar.',
      },
    });
  });

  it('fails closed on an empty @Roles()', async () => {
    await expect(
      guard.canActivate(
        contextFor(OpenController, 'misconfigured', authenticatedUser),
      ),
    ).rejects.toBeInstanceOf(ForbiddenException);
  });

  it('requires the authentication guard to have run first', async () => {
    await expect(
      guard.canActivate(
        contextFor(MerchantController, 'manageOffers', undefined),
      ),
    ).rejects.toBeInstanceOf(UnauthorizedException);
    expect(findUnique).not.toHaveBeenCalled();
  });

  it('sanitizes database failures', async () => {
    findUnique.mockRejectedValue(new Error('private-database-details'));
    const result = guard.canActivate(
      contextFor(MerchantController, 'manageOffers', authenticatedUser),
    );
    await expect(result).rejects.toMatchObject({
      response: { statusCode: 500, error: 'Error interno' },
    });
    await expect(result).rejects.not.toThrow('private-database-details');
  });
});
