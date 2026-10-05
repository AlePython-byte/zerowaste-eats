import { BadRequestException, ValidationPipe } from '@nestjs/common';
import { UserRole } from '../../generated/prisma/enums';
import { CreateUserProfileDto } from './create-user-profile.dto';
import { UpdateUserProfileDto } from './update-user-profile.dto';

describe('User profile DTO validation', () => {
  const pipe = new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
  });
  const create = (body: unknown) =>
    pipe.transform(body, { type: 'body', metatype: CreateUserProfileDto });
  const update = (body: unknown) =>
    pipe.transform(body, { type: 'body', metatype: UpdateUserProfileDto });

  it('trims valid create text and accepts the Prisma role', async () => {
    await expect(
      create({
        displayName: '  Alejandro  ',
        city: '  Pasto  ',
        role: UserRole.MERCHANT,
      }),
    ).resolves.toMatchObject({
      displayName: 'Alejandro',
      city: 'Pasto',
      role: UserRole.MERCHANT,
    });
  });

  it('accepts omitted city and role', async () => {
    await expect(create({ displayName: 'Alejandro' })).resolves.toBeInstanceOf(
      CreateUserProfileDto,
    );
  });

  it.each([2, 80])(
    'accepts text at the %i-character boundary',
    async (length) => {
      const text = 'a'.repeat(length);
      await expect(
        create({ displayName: text, city: text }),
      ).resolves.toMatchObject({ displayName: text, city: text });
    },
  );

  it.each([
    undefined,
    null,
    '',
    '   ',
    'A',
    'a'.repeat(81),
    123,
    ['Alejandro'],
  ])('rejects invalid displayName (%j)', async (value) => {
    await expect(create({ displayName: value })).rejects.toBeInstanceOf(
      BadRequestException,
    );
  });

  it.each([null, '', '   ', 'A', 'a'.repeat(81), 123])(
    'rejects invalid optional city (%j)',
    async (city) => {
      await expect(
        create({ displayName: 'Alejandro', city }),
      ).rejects.toBeInstanceOf(BadRequestException);
    },
  );

  it.each(['ADMIN', 'customer', null])(
    'rejects invalid role (%j)',
    async (role) => {
      await expect(
        create({ displayName: 'Alejandro', role }),
      ).rejects.toBeInstanceOf(BadRequestException);
    },
  );

  it.each([
    'id',
    'email',
    'avatarUrl',
    'createdAt',
    'updatedAt',
    'merchantProfile',
    'reservations',
    'favorites',
  ])('rejects forbidden create property %s', async (field) => {
    await expect(
      create({ displayName: 'Alejandro', [field]: 'untrusted-value' }),
    ).rejects.toBeInstanceOf(BadRequestException);
  });

  it.each([
    {},
    null,
    undefined,
    { displayName: null },
    { city: null },
    { city: '   ' },
    { displayName: '' },
    { city: 'a'.repeat(81) },
  ])('rejects empty or invalid PATCH body (%j)', async (body) => {
    await expect(update(body)).rejects.toBeInstanceOf(BadRequestException);
  });

  it.each([
    { displayName: ' Alejandro ' },
    { city: ' Pasto ' },
    { displayName: ' Alejandro ', city: ' Pasto ' },
  ])('accepts and trims allowed PATCH fields (%j)', async (body) => {
    const dto = (await update(body)) as UpdateUserProfileDto;
    if (body.displayName) expect(dto.displayName).toBe('Alejandro');
    if (body.city) expect(dto.city).toBe('Pasto');
  });

  it.each([
    'id',
    'email',
    'role',
    'avatarUrl',
    'createdAt',
    'updatedAt',
    'merchantProfile',
    'reservations',
    'favorites',
  ])('rejects forbidden PATCH property %s', async (field) => {
    await expect(
      update({ city: 'Pasto', [field]: 'untrusted-value' }),
    ).rejects.toBeInstanceOf(BadRequestException);
  });
});
