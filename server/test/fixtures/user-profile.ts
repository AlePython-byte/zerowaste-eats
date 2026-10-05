import { UserRole } from '../../src/generated/prisma/enums';
import type { User } from '../../src/generated/prisma/client';
import type { AuthenticatedUser } from '../../src/auth/interfaces/authenticated-user.interface';

export const authenticatedUser: AuthenticatedUser = {
  id: 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13',
  email: 'customer@example.test',
};

export const userRecord: User = {
  id: authenticatedUser.id,
  email: 'customer@example.test',
  displayName: 'Alejandro',
  role: UserRole.CUSTOMER,
  city: 'Pasto',
  avatarUrl: null,
  createdAt: new Date('2026-10-04T12:00:00.000Z'),
  updatedAt: new Date('2026-10-04T12:00:00.000Z'),
};

export const publicUserProfile = {
  id: authenticatedUser.id,
  email: 'customer@example.test',
  displayName: 'Alejandro',
  role: UserRole.CUSTOMER,
  city: 'Pasto',
  avatarUrl: null,
  createdAt: '2026-10-04T12:00:00.000Z',
  updatedAt: '2026-10-04T12:00:00.000Z',
};
