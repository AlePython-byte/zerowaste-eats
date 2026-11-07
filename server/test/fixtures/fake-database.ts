import { randomUUID } from 'node:crypto';
import type { DatabaseService } from '../../src/database/database.service';
import { Prisma } from '../../src/generated/prisma/client';
import type {
  Establishment,
  MerchantProfile,
  User,
} from '../../src/generated/prisma/client';

interface Tables {
  users: User[];
  merchantProfiles: MerchantProfile[];
  establishments: Establishment[];
}

const uniqueViolation = (target: string) =>
  new Prisma.PrismaClientKnownRequestError('private-database-details', {
    code: 'P2002',
    clientVersion: '7.10.1',
    meta: { target: [target] },
  });

/**
 * In-memory stand-in for the Prisma client that honors transactions: writes made inside
 * `$transaction` become visible to others only if the callback resolves, and are discarded
 * if it throws. Unique constraints mirror the schema (user id/email, one profile per user).
 */
export function createFakeDatabase(seed: Partial<Tables> = {}) {
  let committed: Tables = {
    users: [...(seed.users ?? [])],
    merchantProfiles: [...(seed.merchantProfiles ?? [])],
    establishments: [...(seed.establishments ?? [])],
  };
  // The tables the next operation reads and writes: a private copy while in a transaction
  let active = committed;
  const now = () => new Date('2026-11-07T12:00:00.000Z');

  const user = {
    findUnique: jest.fn(
      async (args: {
        where: { id: string };
        include?: { merchantProfile?: unknown };
      }) => {
        const found = active.users.find((row) => row.id === args.where.id);
        if (!found) return null;
        if (!args.include?.merchantProfile) return { ...found };
        const profile = active.merchantProfiles.find(
          (row) => row.userId === found.id,
        );
        return {
          ...found,
          merchantProfile: profile ? { id: profile.id } : null,
        };
      },
    ),
    create: jest.fn(async (args: { data: Prisma.UserUncheckedCreateInput }) => {
      if (active.users.some((row) => row.id === args.data.id))
        throw uniqueViolation('id');
      if (active.users.some((row) => row.email === args.data.email))
        throw uniqueViolation('email');
      const row: User = {
        id: args.data.id,
        email: args.data.email,
        displayName: args.data.displayName,
        role: args.data.role ?? 'CUSTOMER',
        city: args.data.city ?? null,
        avatarUrl: args.data.avatarUrl ?? null,
        createdAt: now(),
        updatedAt: now(),
      };
      active.users.push(row);
      return { ...row };
    }),
  };

  const merchantProfile = {
    create: jest.fn(
      async (args: { data: Prisma.MerchantProfileUncheckedCreateInput }) => {
        if (
          active.merchantProfiles.some((row) => row.userId === args.data.userId)
        )
          throw uniqueViolation('userId');
        const row: MerchantProfile = {
          id: randomUUID(),
          userId: args.data.userId,
          businessName: args.data.businessName,
          description: args.data.description ?? null,
          isVerified: false,
          createdAt: now(),
          updatedAt: now(),
        };
        active.merchantProfiles.push(row);
        return { ...row };
      },
    ),
  };

  const establishment = {
    create: jest.fn(
      async (args: { data: Prisma.EstablishmentUncheckedCreateInput }) => {
        const row = {
          id: randomUUID(),
          ...args.data,
          createdAt: now(),
          updatedAt: now(),
        } as unknown as Establishment;
        active.establishments.push(row);
        return { ...row };
      },
    ),
  };

  const $transaction = jest.fn(
    async <T>(callback: (tx: unknown) => Promise<T>): Promise<T> => {
      const draft: Tables = {
        users: [...committed.users],
        merchantProfiles: [...committed.merchantProfiles],
        establishments: [...committed.establishments],
      };
      active = draft;
      try {
        const result = await callback({ user, merchantProfile, establishment });
        committed = draft;
        return result;
      } finally {
        active = committed;
      }
    },
  );

  const client = { $transaction, user, merchantProfile, establishment };
  return {
    client,
    service: { client } as unknown as DatabaseService,
    /** What another connection would see right now. */
    committed: () => committed,
  };
}
