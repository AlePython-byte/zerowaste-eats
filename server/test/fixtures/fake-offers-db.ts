import { randomUUID } from 'node:crypto';
import type { DatabaseService } from '../../src/database/database.service';

/**
 * A tiny in-memory database for the offer services. It evaluates the `where` shapes those
 * services use (equality, `in`, `gt`, `lt`, `gte`, `lte`, `contains` with insensitive mode,
 * `OR`, and the nested ownership filter through establishment and merchant profile), so tests
 * check real behaviour, such as one business not reaching another's offers, not just that a
 * filter was passed along.
 */
export interface FakeUser {
  id: string;
  role: 'CUSTOMER' | 'MERCHANT';
}
export interface FakeMerchantProfile {
  id: string;
  userId: string;
  businessName: string;
  description: string | null;
  isVerified: boolean;
}
export interface FakeEstablishment {
  id: string;
  merchantProfileId: string;
  name: string;
  address: string;
  city: string;
  latitude: number;
  longitude: number;
  createdAt: Date;
}
export type FakeOffer = Record<string, any> & {
  id: string;
  establishmentId: string;
};

export interface FakeTables {
  users: FakeUser[];
  merchantProfiles: FakeMerchantProfile[];
  establishments: FakeEstablishment[];
  offers: FakeOffer[];
  reservations: { offerId: string }[];
  favorites: { userId: string; offerId: string; createdAt: Date }[];
}

type Where = Record<string, any>;

export function createFakeOffersDb(seed: Partial<FakeTables> = {}) {
  const t: FakeTables = {
    users: [],
    merchantProfiles: [],
    establishments: [],
    offers: [],
    reservations: [],
    favorites: [],
    ...seed,
  };

  const establishmentOf = (offer: FakeOffer) =>
    t.establishments.find((e) => e.id === offer.establishmentId);
  const profileOf = (establishment: FakeEstablishment | undefined) =>
    t.merchantProfiles.find((m) => m.id === establishment?.merchantProfileId);

  function matchValue(value: any, condition: any): boolean {
    if (
      condition !== null &&
      typeof condition === 'object' &&
      !(condition instanceof Date)
    ) {
      if ('in' in condition && !condition.in.includes(value)) return false;
      if ('gt' in condition && !(value > condition.gt)) return false;
      if ('lt' in condition && !(value < condition.lt)) return false;
      if ('gte' in condition && !(value >= condition.gte)) return false;
      if ('lte' in condition && !(value <= condition.lte)) return false;
      if ('contains' in condition) {
        const haystack = String(value ?? '');
        const needle = String(condition.contains);
        return condition.mode === 'insensitive'
          ? haystack.toLowerCase().includes(needle.toLowerCase())
          : haystack.includes(needle);
      }
      return true;
    }
    if (value instanceof Date && condition instanceof Date) {
      return value.getTime() === condition.getTime();
    }
    return value === condition;
  }

  function matchRecord(
    record: Record<string, any>,
    where: Where = {},
  ): boolean {
    return Object.entries(where).every(([key, condition]) => {
      if (condition === undefined) return true;
      if (key === 'OR')
        return (condition as Where[]).some((w) => matchRecord(record, w));
      if (key === 'AND')
        return (condition as Where[]).every((w) => matchRecord(record, w));
      return matchValue(record[key], condition);
    });
  }

  function matchEstablishment(
    establishment: FakeEstablishment | undefined,
    where: Where,
  ): boolean {
    if (!establishment) return false;
    const { merchantProfile, ...own } = where;
    if (!matchRecord(establishment, own)) return false;
    if (merchantProfile) {
      const profile = profileOf(establishment);
      if (!profile || !matchRecord(profile, merchantProfile)) return false;
    }
    return true;
  }

  function matchOffer(offer: FakeOffer, where: Where = {}): boolean {
    return Object.entries(where).every(([key, condition]) => {
      if (condition === undefined) return true;
      if (key === 'establishment')
        return matchEstablishment(establishmentOf(offer), condition);
      if (key === 'OR')
        return (condition as Where[]).some((w) => matchOffer(offer, w));
      if (key === 'AND')
        return (condition as Where[]).every((w) => matchOffer(offer, w));
      return matchValue(offer[key], condition);
    });
  }

  /** Includes the establishment (and its profile) when asked, like Prisma's `include`. */
  function withIncludes(offer: FakeOffer, include?: Where) {
    if (!include?.establishment) return { ...offer };
    const establishment = establishmentOf(offer)!;
    const merchantProfile = profileOf(establishment)!;
    return {
      ...offer,
      establishment: {
        ...establishment,
        merchantProfile: { ...merchantProfile },
      },
    };
  }

  function sortBy<T extends Record<string, any>>(
    rows: T[],
    orderBy?: Where,
  ): T[] {
    if (!orderBy) return rows;
    const [[key, direction]] = Object.entries(orderBy);
    return [...rows].sort((a, b) => {
      const left = a[key] instanceof Date ? a[key].getTime() : a[key];
      const right = b[key] instanceof Date ? b[key].getTime() : b[key];
      const order = left < right ? -1 : left > right ? 1 : 0;
      return direction === 'desc' ? -order : order;
    });
  }

  const offer = {
    findFirst: jest.fn(
      async ({ where, include }: { where?: Where; include?: Where } = {}) => {
        const found = t.offers.find((o) => matchOffer(o, where));
        return found ? withIncludes(found, include) : null;
      },
    ),
    findMany: jest.fn(
      async ({
        where,
        include,
        orderBy,
        take,
      }: {
        where?: Where;
        include?: Where;
        orderBy?: Where;
        take?: number;
      } = {}) =>
        sortBy(
          t.offers.filter((o) => matchOffer(o, where)),
          orderBy,
        )
          .slice(0, take ?? undefined)
          .map((o) => withIncludes(o, include)),
    ),
    create: jest.fn(async ({ data }: { data: Record<string, any> }) => {
      const now = new Date();
      const row: FakeOffer = {
        id: randomUUID(),
        createdAt: now,
        updatedAt: now,
        ...data,
      } as unknown as FakeOffer;
      t.offers.push(row);
      return { ...row };
    }),
    update: jest.fn(
      async ({
        where,
        data,
      }: {
        where: { id: string };
        data: Record<string, any>;
      }) => {
        const row = t.offers.find((o) => o.id === where.id);
        if (!row) throw new Error('Record to update not found');
        Object.assign(row, data, { updatedAt: new Date() });
        return { ...row };
      },
    ),
    updateMany: jest.fn(
      async ({ where, data }: { where: Where; data: Record<string, any> }) => {
        const rows = t.offers.filter((o) => matchOffer(o, where));
        rows.forEach((row) =>
          Object.assign(row, data, { updatedAt: new Date() }),
        );
        return { count: rows.length };
      },
    ),
    delete: jest.fn(async ({ where }: { where: { id: string } }) => {
      const index = t.offers.findIndex((o) => o.id === where.id);
      if (index < 0) throw new Error('Record to delete does not exist');
      const [row] = t.offers.splice(index, 1);
      t.favorites = t.favorites.filter((f) => f.offerId !== row.id);
      return row;
    }),
  };

  const establishment = {
    findFirst: jest.fn(
      async ({ where, orderBy }: { where?: Where; orderBy?: Where } = {}) => {
        const rows = sortBy(
          t.establishments.filter((e) => matchEstablishment(e, where ?? {})),
          orderBy,
        );
        return rows[0] ? { ...rows[0] } : null;
      },
    ),
    update: jest.fn(
      async ({
        where,
        data,
      }: {
        where: { id: string };
        data: Record<string, any>;
      }) => {
        const row = t.establishments.find((e) => e.id === where.id);
        if (!row) throw new Error('Record to update not found');
        Object.assign(row, data);
        return { ...row };
      },
    ),
    create: jest.fn(async ({ data }: { data: Record<string, any> }) => {
      const row = {
        id: randomUUID(),
        createdAt: new Date(),
        ...data,
      } as unknown as FakeEstablishment;
      t.establishments.push(row);
      return { ...row };
    }),
  };

  const merchantProfile = {
    findUnique: jest.fn(async ({ where }: { where: { userId: string } }) => {
      const found = t.merchantProfiles.find((m) => m.userId === where.userId);
      return found ? { ...found } : null;
    }),
    update: jest.fn(
      async ({
        where,
        data,
      }: {
        where: { id: string };
        data: Record<string, any>;
      }) => {
        const row = t.merchantProfiles.find((m) => m.id === where.id);
        if (!row) throw new Error('Record to update not found');
        Object.assign(row, data);
        return { ...row };
      },
    ),
  };

  /**
   * Runs the callback against the same tables and, if it throws, puts every table back as
   * it was, like a rolled-back transaction.
   */
  const $transaction = jest.fn(
    async (callback: (tx: unknown) => Promise<unknown>): Promise<unknown> => {
      const snapshot = (Object.keys(t) as (keyof FakeTables)[]).map(
        (key) => [key, t[key].map((row) => ({ ...row }))] as const,
      );
      try {
        return await callback(client);
      } catch (error) {
        snapshot.forEach(([key, rows]) => {
          (t as unknown as Record<string, unknown>)[key] = rows;
        });
        throw error;
      }
    },
  );

  const reservation = {
    count: jest.fn(
      async ({ where }: { where?: Where } = {}) =>
        t.reservations.filter((r) => matchRecord(r, where)).length,
    ),
  };

  /** What RolesGuard reads: the stored role of a user. */
  const user = {
    findUnique: jest.fn(async ({ where }: { where: { id: string } }) => {
      const found = t.users.find((u) => u.id === where.id);
      return found ? { role: found.role } : null;
    }),
  };

  const client = {
    offer,
    establishment,
    reservation,
    user,
    merchantProfile,
    $transaction,
  };
  return {
    tables: t,
    client,
    service: { client } as unknown as DatabaseService,
  };
}

/** A business with one establishment, in downtown Pasto unless told otherwise. */
export function addBusiness(
  tables: FakeTables,
  overrides: Partial<FakeEstablishment> & {
    userId?: string;
    businessName?: string;
  } = {},
) {
  const userId = overrides.userId ?? randomUUID();
  const profile: FakeMerchantProfile = {
    id: randomUUID(),
    userId,
    businessName: overrides.businessName ?? 'Panadería La Esquina',
    description: null,
    isVerified: false,
  };
  const establishment: FakeEstablishment = {
    id: randomUUID(),
    merchantProfileId: profile.id,
    name: overrides.name ?? profile.businessName,
    address: overrides.address ?? 'Centro de Pasto',
    city: overrides.city ?? 'Pasto',
    latitude: overrides.latitude ?? 1.213611,
    longitude: overrides.longitude ?? -77.281111,
    createdAt: overrides.createdAt ?? new Date('2026-11-01T00:00:00Z'),
  };
  tables.users.push({ id: userId, role: 'MERCHANT' });
  tables.merchantProfiles.push(profile);
  tables.establishments.push(establishment);
  return { userId, profile, establishment };
}

/** An offer stored as the database would hold it (prices as plain numbers). */
export function addOffer(
  tables: FakeTables,
  establishmentId: string,
  overrides: Record<string, any> = {},
): FakeOffer {
  const offer: FakeOffer = {
    id: randomUUID(),
    establishmentId,
    title: 'Bolsa de pan del día',
    description: null,
    category: 'BAKERY',
    imageUrl: null,
    originalPrice: 12000,
    discountedPrice: 6000,
    quantityTotal: 5,
    quantityAvailable: 5,
    pickupStart: new Date('2026-11-07T22:00:00Z'),
    pickupEnd: new Date('2026-11-07T23:00:00Z'),
    expiresAt: new Date('2026-11-07T23:00:00Z'),
    status: 'AVAILABLE',
    createdAt: new Date('2026-11-07T12:00:00Z'),
    updatedAt: new Date('2026-11-07T12:00:00Z'),
    ...overrides,
  };
  tables.offers.push(offer);
  return offer;
}
