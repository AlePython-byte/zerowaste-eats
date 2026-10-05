import {
  ConflictException,
  NotFoundException,
  UnprocessableEntityException,
} from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import { Prisma, UserRole, type User } from '../generated/prisma/client';
import {
  authenticatedUser,
  publicUserProfile,
  userRecord,
} from '../../test/fixtures/user-profile';
import { UsersService } from './users.service';

describe('UsersService', () => {
  const user = {
    create: jest.fn<Promise<User>, [Prisma.UserCreateArgs]>(),
    findUnique: jest.fn<Promise<User | null>, [Prisma.UserFindUniqueArgs]>(),
    update: jest.fn<Promise<User>, [Prisma.UserUpdateArgs]>(),
  };
  const service = new UsersService({
    client: { user },
  } as unknown as DatabaseService);
  const prismaError = (code: string, target?: string) =>
    new Prisma.PrismaClientKnownRequestError('private-database-details', {
      code,
      clientVersion: '7.10.0',
      meta: { target: [target] },
    });

  beforeEach(() => {
    user.create.mockReset().mockResolvedValue(userRecord);
    user.findUnique.mockReset().mockResolvedValue(userRecord);
    user.update.mockReset().mockResolvedValue(userRecord);
  });

  it('creates a customer using only the authenticated UUID/email and defaults the role', async () => {
    await expect(
      service.createCurrentProfile(authenticatedUser, {
        displayName: 'Alejandro',
        city: 'Pasto',
      }),
    ).resolves.toEqual(publicUserProfile);
    expect(user.create).toHaveBeenCalledWith({
      data: {
        id: authenticatedUser.id,
        email: authenticatedUser.email,
        displayName: 'Alejandro',
        city: 'Pasto',
        role: UserRole.CUSTOMER,
      },
    });
  });

  it.each([UserRole.CUSTOMER, UserRole.MERCHANT])(
    'accepts the selected %s role only at creation',
    async (role) => {
      user.create.mockResolvedValue({ ...userRecord, role, city: null });
      await expect(
        service.createCurrentProfile(authenticatedUser, {
          displayName: 'Alejandro',
          role,
        }),
      ).resolves.toEqual({ ...publicUserProfile, role, city: null });
      expect(user.create.mock.calls[0][0].data).toEqual({
        id: authenticatedUser.id,
        email: authenticatedUser.email,
        displayName: 'Alejandro',
        city: null,
        role,
      });
    },
  );

  it('never forwards injected identity or relation fields, even if DTO validation is bypassed', async () => {
    const dto = {
      displayName: 'Alejandro',
      id: 'another-user',
      email: 'other@example.test',
      avatarUrl: 'private-value',
      merchantProfile: { create: {} },
      createdAt: new Date(),
    };
    await service.createCurrentProfile(authenticatedUser, dto);
    expect(user.create).toHaveBeenCalledWith({
      data: {
        id: authenticatedUser.id,
        email: authenticatedUser.email,
        displayName: 'Alejandro',
        city: null,
        role: UserRole.CUSTOMER,
      },
    });
  });

  it.each([null, '', '   ', 'invalid-email'])(
    'rejects unusable verified email (%s) before accessing Prisma',
    async (email) => {
      await expect(
        service.createCurrentProfile(
          { ...authenticatedUser, email },
          { displayName: 'Alejandro' },
        ),
      ).rejects.toBeInstanceOf(UnprocessableEntityException);
      expect(user.create).not.toHaveBeenCalled();
    },
  );

  it.each(['id', 'email'])(
    'maps %s uniqueness conflicts to a safe 409',
    async (target) => {
      user.create.mockRejectedValue(prismaError('P2002', target));
      const result = service.createCurrentProfile(authenticatedUser, {
        displayName: 'Alejandro',
      });
      await expect(result).rejects.toBeInstanceOf(ConflictException);
      await expect(result).rejects.toMatchObject({
        response: {
          statusCode: 409,
          error: 'Conflicto',
          message: 'Ya existe un perfil con estos datos.',
        },
      });
    },
  );

  it('returns only the current public profile with ISO timestamps, excluding relations', async () => {
    const recordWithRelations = {
      ...userRecord,
      merchantProfile: { private: true },
      reservations: [],
      favorites: [],
    };
    user.findUnique.mockResolvedValue(recordWithRelations);
    await expect(service.getCurrentProfile(authenticatedUser)).resolves.toEqual(
      publicUserProfile,
    );
    expect(user.findUnique).toHaveBeenCalledWith({
      where: { id: authenticatedUser.id },
    });
    expect(user.create).not.toHaveBeenCalled();
  });

  it('returns 404 without implicitly creating a profile', async () => {
    user.findUnique.mockResolvedValue(null);
    await expect(
      service.getCurrentProfile(authenticatedUser),
    ).rejects.toBeInstanceOf(NotFoundException);
    expect(user.create).not.toHaveBeenCalled();
  });

  it('cannot read a different user profile', async () => {
    user.findUnique.mockImplementation(async ({ where }) =>
      where.id === userRecord.id ? userRecord : null,
    );
    const anotherIdentity = {
      ...authenticatedUser,
      id: 'ce1a20d6-bf49-48ef-8bd4-782fc2e3dc01',
    };
    await expect(
      service.getCurrentProfile(anotherIdentity),
    ).rejects.toBeInstanceOf(NotFoundException);
    expect(user.findUnique).toHaveBeenCalledWith({
      where: { id: anotherIdentity.id },
    });
  });

  it.each([{ displayName: 'Nuevo nombre' }, { city: 'Bogotá' }])(
    'updates only supplied allowed fields (%j)',
    async (dto) => {
      user.update.mockResolvedValue({ ...userRecord, ...dto });
      await expect(
        service.updateCurrentProfile(authenticatedUser, dto),
      ).resolves.toEqual({ ...publicUserProfile, ...dto });
      expect(user.update).toHaveBeenCalledWith({
        where: { id: authenticatedUser.id },
        data: dto,
      });
    },
  );

  it('never updates identity, role, timestamps, or relations from an untrusted DTO', async () => {
    const dto = {
      city: 'Pasto',
      id: 'another-user',
      email: 'other@example.test',
      role: UserRole.MERCHANT,
      avatarUrl: 'private-value',
      updatedAt: new Date(),
      merchantProfile: { create: {} },
    };
    await service.updateCurrentProfile(authenticatedUser, dto);
    expect(user.update).toHaveBeenCalledWith({
      where: { id: authenticatedUser.id },
      data: { city: 'Pasto' },
    });
  });

  it('returns 404 when an update targets a missing profile', async () => {
    user.update.mockRejectedValue(prismaError('P2025'));
    await expect(
      service.updateCurrentProfile(authenticatedUser, { city: 'Pasto' }),
    ).rejects.toMatchObject({
      response: {
        statusCode: 404,
        error: 'No encontrado',
        message: 'No se encontró tu perfil.',
      },
    });
  });

  it.each(['create', 'findUnique', 'update'] as const)(
    'sanitizes unexpected %s failures',
    async (operation) => {
      user[operation].mockRejectedValue(new Error('private-database-details'));
      const result =
        operation === 'create'
          ? service.createCurrentProfile(authenticatedUser, {
              displayName: 'Alejandro',
            })
          : operation === 'findUnique'
            ? service.getCurrentProfile(authenticatedUser)
            : service.updateCurrentProfile(authenticatedUser, {
                city: 'Pasto',
              });
      await expect(result).rejects.toMatchObject({
        response: { statusCode: 500, error: 'Error interno' },
      });
      await expect(result).rejects.not.toThrow('private-database-details');
    },
  );
});
