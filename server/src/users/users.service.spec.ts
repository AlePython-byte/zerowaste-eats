import {
  ConflictException,
  NotFoundException,
  UnprocessableEntityException,
} from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import { Prisma, UserRole, type User } from '../generated/prisma/client';
import { DEFAULT_ESTABLISHMENT_LOCATION } from '../merchants/default-establishment-location';
import { createFakeDatabase } from '../../test/fixtures/fake-database';
import {
  authenticatedUser,
  publicUserProfile,
  userRecord,
} from '../../test/fixtures/user-profile';
import { UsersService } from './users.service';

describe('UsersService.createCurrentProfile', () => {
  let db: ReturnType<typeof createFakeDatabase>;
  let service: UsersService;
  const merchantUser: User = {
    ...userRecord,
    role: UserRole.MERCHANT,
    email: 'merchant@example.test',
  };

  const useDatabase = (seed: Parameters<typeof createFakeDatabase>[0]) => {
    db = createFakeDatabase(seed);
    service = new UsersService(db.service);
  };

  beforeEach(() => useDatabase({}));

  it('creates a customer using only the authenticated UUID/email and defaults the role', async () => {
    const result = await service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
      city: 'Pasto',
    });
    expect(result.created).toBe(true);
    expect(result.profile).toMatchObject({
      id: authenticatedUser.id,
      email: authenticatedUser.email,
      displayName: 'Alejandro',
      city: 'Pasto',
      role: UserRole.CUSTOMER,
    });
    expect(db.client.user.create).toHaveBeenCalledWith({
      data: {
        id: authenticatedUser.id,
        email: authenticatedUser.email,
        displayName: 'Alejandro',
        city: 'Pasto',
        role: UserRole.CUSTOMER,
      },
    });
    expect(db.committed().merchantProfiles).toHaveLength(0);
    expect(db.committed().establishments).toHaveLength(0);
  });

  it('creates user, merchant profile and establishment in one transaction for a merchant', async () => {
    const result = await service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
      role: UserRole.MERCHANT,
      businessName: 'Panadería La Esquina',
    });
    expect(result).toMatchObject({
      created: true,
      profile: { role: UserRole.MERCHANT },
    });
    expect(db.client.$transaction).toHaveBeenCalledTimes(1);
    const { users, merchantProfiles, establishments } = db.committed();
    expect(users).toHaveLength(1);
    expect(merchantProfiles).toEqual([
      expect.objectContaining({
        userId: authenticatedUser.id,
        businessName: 'Panadería La Esquina',
      }),
    ]);
    expect(establishments).toEqual([
      expect.objectContaining({
        merchantProfileId: merchantProfiles[0].id,
        name: 'Panadería La Esquina',
        ...DEFAULT_ESTABLISHMENT_LOCATION,
      }),
    ]);
  });

  it('names the merchant business after the display name when none is given', async () => {
    await service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
      role: UserRole.MERCHANT,
    });
    expect(db.committed().merchantProfiles[0].businessName).toBe('Alejandro');
    expect(db.committed().establishments[0].name).toBe('Alejandro');
  });

  it('starts the first establishment in downtown Pasto', () => {
    expect(DEFAULT_ESTABLISHMENT_LOCATION).toEqual({
      address: 'Centro de Pasto',
      city: 'Pasto',
      latitude: '1.213611',
      longitude: '-77.281111',
    });
  });

  it.each(['merchantProfile', 'establishment'] as const)(
    'rolls back everything when creating the %s fails',
    async (failing) => {
      db.client[failing].create.mockRejectedValueOnce(
        new Error('private-database-details'),
      );
      const result = service.createCurrentProfile(authenticatedUser, {
        displayName: 'Alejandro',
        role: UserRole.MERCHANT,
      });
      await expect(result).rejects.toMatchObject({
        response: { statusCode: 500, error: 'Error interno' },
      });
      await expect(result).rejects.not.toThrow('private-database-details');
      // The user row was written inside the transaction, then discarded with it
      expect(db.client.user.create).toHaveBeenCalledTimes(1);
      expect(db.committed()).toEqual({
        users: [],
        merchantProfiles: [],
        establishments: [],
      });
    },
  );

  it('can be retried after a rollback and then succeeds', async () => {
    db.client.establishment.create.mockRejectedValueOnce(new Error('timeout'));
    const dto = { displayName: 'Alejandro', role: UserRole.MERCHANT };
    await expect(
      service.createCurrentProfile(authenticatedUser, dto),
    ).rejects.toBeDefined();
    await expect(
      service.createCurrentProfile(authenticatedUser, dto),
    ).resolves.toMatchObject({ created: true });
    expect(db.committed().users).toHaveLength(1);
    expect(db.committed().merchantProfiles).toHaveLength(1);
    expect(db.committed().establishments).toHaveLength(1);
  });

  it.each([
    ['customer', { displayName: 'Alejandro' }],
    ['merchant', { displayName: 'Alejandro', role: UserRole.MERCHANT }],
  ] as const)(
    'is idempotent for a %s: a repeated request returns the same profile without new rows',
    async (_label, dto) => {
      const first = await service.createCurrentProfile(authenticatedUser, dto);
      const second = await service.createCurrentProfile(authenticatedUser, {
        ...dto,
        displayName: 'Otro nombre',
      });
      const merchantRows = 'role' in dto ? 1 : 0;
      expect(first.created).toBe(true);
      expect(second).toEqual({ profile: first.profile, created: false });
      expect(db.client.user.create).toHaveBeenCalledTimes(1);
      expect(db.committed().users).toHaveLength(1);
      expect(db.committed().merchantProfiles).toHaveLength(merchantRows);
      expect(db.committed().establishments).toHaveLength(merchantRows);
    },
  );

  it('returns the existing merchant on retry when the role is omitted', async () => {
    await service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
      role: UserRole.MERCHANT,
    });
    await expect(
      service.createCurrentProfile(authenticatedUser, {
        displayName: 'Alejandro',
      }),
    ).resolves.toMatchObject({
      created: false,
      profile: { role: UserRole.MERCHANT },
    });
  });

  it.each([
    [UserRole.CUSTOMER, UserRole.MERCHANT],
    [UserRole.MERCHANT, UserRole.CUSTOMER],
  ])(
    'refuses to turn an existing %s into a %s',
    async (existingRole, requestedRole) => {
      useDatabase({ users: [{ ...userRecord, role: existingRole }] });
      const result = service.createCurrentProfile(authenticatedUser, {
        displayName: 'Alejandro',
        role: requestedRole,
      });
      await expect(result).rejects.toBeInstanceOf(ConflictException);
      await expect(result).rejects.toMatchObject({
        response: {
          statusCode: 409,
          error: 'Conflicto',
          message:
            'Tu perfil ya existe con otro rol y el rol no se puede cambiar.',
        },
      });
      expect(db.committed().users).toEqual([
        expect.objectContaining({ role: existingRole }),
      ]);
      expect(db.committed().merchantProfiles).toHaveLength(0);
    },
  );

  it('completes a merchant that exists without its merchant profile', async () => {
    useDatabase({ users: [merchantUser] });
    const result = await service.createCurrentProfile(
      { ...authenticatedUser, email: merchantUser.email },
      { displayName: 'Alejandro', role: UserRole.MERCHANT },
    );
    expect(result.created).toBe(false);
    expect(db.client.user.create).not.toHaveBeenCalled();
    expect(db.committed().merchantProfiles).toHaveLength(1);
    expect(db.committed().establishments).toHaveLength(1);
  });

  it('answers a request that lost a creation race with the winner profile', async () => {
    // Both requests saw no profile; the other one committed first
    await service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
      role: UserRole.MERCHANT,
    });
    db.client.user.findUnique.mockResolvedValueOnce(null);
    const result = await service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
      role: UserRole.MERCHANT,
    });
    expect(result).toMatchObject({
      created: false,
      profile: { id: authenticatedUser.id, role: UserRole.MERCHANT },
    });
    expect(db.committed().users).toHaveLength(1);
    expect(db.committed().merchantProfiles).toHaveLength(1);
    expect(db.committed().establishments).toHaveLength(1);
  });

  it('rejects a race winner that chose another role', async () => {
    await service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
    });
    db.client.user.findUnique.mockResolvedValueOnce(null);
    await expect(
      service.createCurrentProfile(authenticatedUser, {
        displayName: 'Alejandro',
        role: UserRole.MERCHANT,
      }),
    ).rejects.toBeInstanceOf(ConflictException);
    expect(db.committed().merchantProfiles).toHaveLength(0);
  });

  it('returns 409 when the email already belongs to another account', async () => {
    useDatabase({
      users: [{ ...userRecord, id: 'ce1a20d6-bf49-48ef-8bd4-782fc2e3dc01' }],
    });
    await expect(
      service.createCurrentProfile(authenticatedUser, {
        displayName: 'Alejandro',
      }),
    ).rejects.toMatchObject({
      response: {
        statusCode: 409,
        error: 'Conflicto',
        message: 'Ya existe un perfil con estos datos.',
      },
    });
    expect(db.committed().users).toHaveLength(1);
  });

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
    expect(db.client.user.create).toHaveBeenCalledWith({
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
      expect(db.client.$transaction).not.toHaveBeenCalled();
    },
  );

  it('sanitizes unexpected failures', async () => {
    db.client.$transaction.mockRejectedValueOnce(
      new Error('private-database-details'),
    );
    const result = service.createCurrentProfile(authenticatedUser, {
      displayName: 'Alejandro',
    });
    await expect(result).rejects.toMatchObject({
      response: { statusCode: 500, error: 'Error interno' },
    });
    await expect(result).rejects.not.toThrow('private-database-details');
  });
});

describe('UsersService read and update', () => {
  const user = {
    create: jest.fn<Promise<User>, [Prisma.UserCreateArgs]>(),
    findUnique: jest.fn<Promise<User | null>, [Prisma.UserFindUniqueArgs]>(),
    update: jest.fn<Promise<User>, [Prisma.UserUpdateArgs]>(),
  };
  const service = new UsersService({
    client: { user },
  } as unknown as DatabaseService);
  const prismaError = (code: string) =>
    new Prisma.PrismaClientKnownRequestError('private-database-details', {
      code,
      clientVersion: '7.10.1',
    });

  beforeEach(() => {
    user.create.mockReset().mockResolvedValue(userRecord);
    user.findUnique.mockReset().mockResolvedValue(userRecord);
    user.update.mockReset().mockResolvedValue(userRecord);
  });

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

  it.each(['findUnique', 'update'] as const)(
    'sanitizes unexpected %s failures',
    async (operation) => {
      user[operation].mockRejectedValue(new Error('private-database-details'));
      const result =
        operation === 'findUnique'
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
