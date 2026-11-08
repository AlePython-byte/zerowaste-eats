import { ConflictException, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from '../../../test/fixtures/fake-offers-db';
import { OfferStatus } from '../../generated/prisma/enums';
import { MerchantOffersService } from './merchant-offers.service';

const SUPABASE_URL = 'https://auth.example.invalid';
const NOW = new Date('2026-11-07T20:00:00Z');
const at = (iso: string) => new Date(iso).toISOString();

describe('MerchantOffersService', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let service: MerchantOffersService;
  let mine: ReturnType<typeof addBusiness>;
  let theirs: ReturnType<typeof addBusiness>;

  const photoOf = (userId: string, file = 'pan.jpg') =>
    `${SUPABASE_URL}/storage/v1/object/public/offer-photos/${userId}/${file}`;

  const newOffer = (overrides: Record<string, unknown> = {}) => ({
    title: 'Bolsa de pan del día',
    category: 'BAKERY' as const,
    originalPrice: 12000,
    discountedPrice: 6000,
    quantityTotal: 5,
    pickupStart: '2026-11-07T22:00:00Z',
    pickupEnd: '2026-11-07T23:00:00Z',
    ...overrides,
  });

  beforeEach(() => {
    db = createFakeOffersDb();
    mine = addBusiness(db.tables, { businessName: 'Mi panadería' });
    theirs = addBusiness(db.tables, { businessName: 'La de enfrente' });
    service = new MerchantOffersService(
      db.service,
      new ConfigService({ SUPABASE_URL }),
    );
    service.clock = () => NOW;
  });

  // --- Ownership ---

  describe('ownership', () => {
    let foreign: ReturnType<typeof addOffer>;

    beforeEach(() => {
      addOffer(db.tables, mine.establishment.id, { title: 'Mía' });
      foreign = addOffer(db.tables, theirs.establishment.id, {
        title: 'Ajena',
      });
    });

    it('lists only the caller business offers', async () => {
      const offers = await service.list(mine.userId);

      expect(offers.map((o) => o.title)).toEqual(['Mía']);
    });

    it.each([
      [
        'read',
        (s: MerchantOffersService, u: string, id: string) => s.get(u, id),
      ],
      [
        'edit',
        (s: MerchantOffersService, u: string, id: string) =>
          s.update(u, id, { title: 'Robada' }),
      ],
      [
        'pause',
        (s: MerchantOffersService, u: string, id: string) => s.pause(u, id),
      ],
      [
        'resume',
        (s: MerchantOffersService, u: string, id: string) => s.resume(u, id),
      ],
      [
        'delete',
        (s: MerchantOffersService, u: string, id: string) => s.remove(u, id),
      ],
    ])(
      'cannot %s another business offer: 404, and nothing changes',
      async (_label, act) => {
        const before = { ...foreign };

        await expect(
          act(service, mine.userId, foreign.id),
        ).rejects.toBeInstanceOf(NotFoundException);
        expect(db.tables.offers.find((o) => o.id === foreign.id)).toEqual(
          before,
        );
      },
    );

    it('answers a foreign offer exactly like one that does not exist', async () => {
      const foreignError = await service
        .get(mine.userId, foreign.id)
        .catch((e) => e);
      const missingError = await service
        .get(mine.userId, '00000000-0000-4000-8000-000000000000')
        .catch((e) => e);

      expect(foreignError.getResponse()).toEqual(missingError.getResponse());
    });

    it('cannot publish on another business establishment', async () => {
      await expect(
        service.create(
          mine.userId,
          newOffer({ establishmentId: theirs.establishment.id }),
        ),
      ).rejects.toMatchObject({ response: { statusCode: 404 } });
      expect(db.tables.offers).toHaveLength(2);
    });
  });

  // --- Create ---

  it('creates an available offer on the business establishment, with all units free', async () => {
    const offer = await service.create(
      mine.userId,
      newOffer({
        description: '  Pan de esta mañana.  ',
        imageUrl: photoOf(mine.userId),
      }),
    );

    expect(offer).toMatchObject({
      establishmentId: mine.establishment.id,
      title: 'Bolsa de pan del día',
      description: 'Pan de esta mañana.',
      imageUrl: photoOf(mine.userId),
      originalPrice: 12000,
      discountedPrice: 6000,
      discountPercent: 50,
      quantityTotal: 5,
      quantityAvailable: 5,
      status: OfferStatus.AVAILABLE,
      pickupEnd: at('2026-11-07T23:00:00Z'),
    });
    expect(db.tables.offers[0].expiresAt).toEqual(
      new Date('2026-11-07T23:00:00Z'),
    );
  });

  it('asks a merchant without an establishment to complete the business profile', async () => {
    const stranger = '6c1c3c4e-5b8e-4a55-9b52-2d7d3a8f0a01';

    await expect(service.create(stranger, newOffer())).rejects.toMatchObject({
      response: {
        statusCode: 404,
        message: 'Completa el perfil de tu negocio antes de publicar ofertas.',
      },
    });
  });

  it.each([
    [
      { discountedPrice: 12000 },
      'El precio con descuento debe ser menor que el precio original.',
    ],
    [
      {
        pickupEnd: '2026-11-07T21:00:00Z',
        pickupStart: '2026-11-07T22:00:00Z',
      },
      'La recogida debe terminar después de empezar.',
    ],
    [
      {
        pickupStart: '2026-11-07T18:00:00Z',
        pickupEnd: '2026-11-07T19:00:00Z',
      },
      'La recogida debe terminar en el futuro.',
    ],
    [
      {
        pickupStart: '2026-11-20T18:00:00Z',
        pickupEnd: '2026-11-20T19:00:00Z',
      },
      'La recogida debe terminar dentro de los próximos 7 días.',
    ],
  ])('rejects %j with a clear Spanish 400', async (overrides, message) => {
    await expect(
      service.create(mine.userId, newOffer(overrides)),
    ).rejects.toMatchObject({
      response: { statusCode: 400, message },
    });
    expect(db.tables.offers).toHaveLength(0);
  });

  it.each([
    ['another business folder', (u: string, other: string) => photoOf(other)],
    ['another site', () => 'https://evil.example/pan.jpg'],
    ['a subfolder', (u: string) => photoOf(u, 'otra/pan.jpg')],
    ['a path trick', (u: string) => photoOf(u, '..%2Fpan.jpg')],
    ['a query string', (u: string) => `${photoOf(u)}?x=1`],
    ['another file type', (u: string) => photoOf(u, 'pan.svg')],
  ])(
    'only accepts photos in its own bucket folder, not %s',
    async (_label, url) => {
      await expect(
        service.create(
          mine.userId,
          newOffer({ imageUrl: url(mine.userId, theirs.userId) }),
        ),
      ).rejects.toMatchObject({
        response: {
          statusCode: 400,
          message: 'La foto debe estar en tu carpeta de fotos de ofertas.',
        },
      });
    },
  );

  // --- Expiry, worked out when reading ---

  it('reads an offer whose pickup has ended as EXPIRED, without writing anything', async () => {
    const offer = addOffer(db.tables, mine.establishment.id, {
      pickupStart: new Date('2026-11-07T17:00:00Z'),
      pickupEnd: new Date('2026-11-07T19:00:00Z'),
    });

    await expect(service.get(mine.userId, offer.id)).resolves.toMatchObject({
      status: OfferStatus.EXPIRED,
    });
    expect(db.tables.offers[0].status).toBe(OfferStatus.AVAILABLE);
    expect(db.client.offer.update).not.toHaveBeenCalled();
  });

  it('becomes EXPIRED exactly when the pickup ends', async () => {
    const offer = addOffer(db.tables, mine.establishment.id);

    service.clock = () => new Date('2026-11-07T22:59:59Z');
    expect((await service.get(mine.userId, offer.id)).status).toBe(
      OfferStatus.AVAILABLE,
    );
    service.clock = () => new Date('2026-11-07T23:00:00Z');
    expect((await service.get(mine.userId, offer.id)).status).toBe(
      OfferStatus.EXPIRED,
    );
  });

  it('filters the list by the status people see', async () => {
    addOffer(db.tables, mine.establishment.id, { title: 'Vigente' });
    addOffer(db.tables, mine.establishment.id, {
      title: 'Vencida',
      pickupStart: new Date('2026-11-07T17:00:00Z'),
      pickupEnd: new Date('2026-11-07T19:00:00Z'),
    });

    expect(
      (await service.list(mine.userId, OfferStatus.EXPIRED)).map(
        (o) => o.title,
      ),
    ).toEqual(['Vencida']);
    expect(
      (await service.list(mine.userId, OfferStatus.AVAILABLE)).map(
        (o) => o.title,
      ),
    ).toEqual(['Vigente']);
  });

  // --- Pause and resume ---

  describe('pause and resume', () => {
    it.each([OfferStatus.AVAILABLE, OfferStatus.SOLD_OUT])(
      'pauses a %s offer',
      async (status) => {
        const offer = addOffer(db.tables, mine.establishment.id, { status });

        await expect(
          service.pause(mine.userId, offer.id),
        ).resolves.toMatchObject({
          status: OfferStatus.PAUSED,
        });
      },
    );

    it('resumes to AVAILABLE when units are left, SOLD_OUT when none are', async () => {
      const withStock = addOffer(db.tables, mine.establishment.id, {
        status: 'PAUSED',
      });
      const empty = addOffer(db.tables, mine.establishment.id, {
        status: 'PAUSED',
        quantityAvailable: 0,
      });

      expect((await service.resume(mine.userId, withStock.id)).status).toBe(
        OfferStatus.AVAILABLE,
      );
      expect((await service.resume(mine.userId, empty.id)).status).toBe(
        OfferStatus.SOLD_OUT,
      );
    });

    it('is idempotent: pausing a paused offer and resuming an active one change nothing', async () => {
      const paused = addOffer(db.tables, mine.establishment.id, {
        status: 'PAUSED',
      });
      const active = addOffer(db.tables, mine.establishment.id);

      expect((await service.pause(mine.userId, paused.id)).status).toBe(
        OfferStatus.PAUSED,
      );
      expect((await service.resume(mine.userId, active.id)).status).toBe(
        OfferStatus.AVAILABLE,
      );
    });

    it.each([
      ['pause', 'AVAILABLE'],
      ['resume', 'PAUSED'],
    ] as const)(
      'cannot %s an offer whose pickup has ended',
      async (action, status) => {
        const offer = addOffer(db.tables, mine.establishment.id, {
          status,
          pickupStart: new Date('2026-11-07T17:00:00Z'),
          pickupEnd: new Date('2026-11-07T19:00:00Z'),
        });

        await expect(
          service[action](mine.userId, offer.id),
        ).rejects.toMatchObject({
          response: {
            statusCode: 409,
            message:
              'La oferta ya venció. Cambia el horario de recogida para volver a publicarla.',
          },
        });
        expect(db.tables.offers[0].status).toBe(status);
      },
    );

    it.each(['pause', 'resume'] as const)(
      'cannot %s a cancelled offer',
      async (action) => {
        const offer = addOffer(db.tables, mine.establishment.id, {
          status: 'CANCELLED',
        });

        await expect(
          service[action](mine.userId, offer.id),
        ).rejects.toBeInstanceOf(ConflictException);
      },
    );
  });

  // --- Update ---

  it('edits only what was sent and keeps reserved units reserved', async () => {
    const offer = addOffer(db.tables, mine.establishment.id, {
      quantityTotal: 5,
      quantityAvailable: 3,
    });

    const edited = await service.update(mine.userId, offer.id, {
      title: 'Pan y mogollas',
      quantityTotal: 8,
    });

    expect(edited).toMatchObject({
      title: 'Pan y mogollas',
      quantityTotal: 8,
      quantityAvailable: 6,
      originalPrice: 12000,
      category: 'BAKERY',
    });
  });

  it('refuses a total below the units already reserved', async () => {
    const offer = addOffer(db.tables, mine.establishment.id, {
      quantityTotal: 5,
      quantityAvailable: 2,
    });

    await expect(
      service.update(mine.userId, offer.id, { quantityTotal: 2 }),
    ).rejects.toMatchObject({
      response: {
        statusCode: 409,
        message:
          'Ya hay 3 unidades reservadas; la cantidad no puede ser menor.',
      },
    });
  });

  it('marks an offer sold out when its free units reach zero, and back on sale above it', async () => {
    const offer = addOffer(db.tables, mine.establishment.id, {
      quantityTotal: 5,
      quantityAvailable: 2,
    });

    expect(
      (await service.update(mine.userId, offer.id, { quantityTotal: 3 }))
        .status,
    ).toBe(OfferStatus.SOLD_OUT);
    expect(
      (await service.update(mine.userId, offer.id, { quantityTotal: 6 }))
        .status,
    ).toBe(OfferStatus.AVAILABLE);
  });

  it('checks the new discounted price against the stored original price', async () => {
    const offer = addOffer(db.tables, mine.establishment.id);

    await expect(
      service.update(mine.userId, offer.id, { discountedPrice: 13000 }),
    ).rejects.toMatchObject({ response: { statusCode: 400 } });
  });

  it('lets the text of a past offer be fixed, but a new window must be in the future', async () => {
    const offer = addOffer(db.tables, mine.establishment.id, {
      pickupStart: new Date('2026-11-07T17:00:00Z'),
      pickupEnd: new Date('2026-11-07T19:00:00Z'),
    });

    await expect(
      service.update(mine.userId, offer.id, { title: 'Corregido' }),
    ).resolves.toMatchObject({
      title: 'Corregido',
      status: OfferStatus.EXPIRED,
    });
    await expect(
      service.update(mine.userId, offer.id, {
        pickupEnd: '2026-11-07T19:30:00Z',
      }),
    ).rejects.toMatchObject({ response: { statusCode: 400 } });

    // Moving the window forward publishes it again
    await expect(
      service.update(mine.userId, offer.id, {
        pickupStart: '2026-11-08T17:00:00Z',
        pickupEnd: '2026-11-08T19:00:00Z',
      }),
    ).resolves.toMatchObject({ status: OfferStatus.AVAILABLE });
  });

  it('clears the description or photo with null', async () => {
    const offer = addOffer(db.tables, mine.establishment.id, {
      description: 'Algo',
      imageUrl: photoOf(mine.userId),
    });

    await expect(
      service.update(mine.userId, offer.id, {
        description: null,
        imageUrl: null,
      }),
    ).resolves.toMatchObject({ description: null, imageUrl: null });
  });

  it('refuses an empty change and edits on a cancelled offer', async () => {
    const offer = addOffer(db.tables, mine.establishment.id);
    const cancelled = addOffer(db.tables, mine.establishment.id, {
      status: 'CANCELLED',
    });

    await expect(
      service.update(mine.userId, offer.id, {}),
    ).rejects.toMatchObject({
      response: { statusCode: 400 },
    });
    await expect(
      service.update(mine.userId, cancelled.id, { title: 'Otra' }),
    ).rejects.toBeInstanceOf(ConflictException);
  });

  // --- Delete ---

  it('deletes an offer nobody reserved', async () => {
    const offer = addOffer(db.tables, mine.establishment.id);

    await service.remove(mine.userId, offer.id);

    expect(db.tables.offers).toHaveLength(0);
  });

  it('keeps an offer with reservations and suggests pausing it', async () => {
    const offer = addOffer(db.tables, mine.establishment.id);
    db.tables.reservations.push({ offerId: offer.id });

    await expect(service.remove(mine.userId, offer.id)).rejects.toMatchObject({
      response: {
        statusCode: 409,
        message:
          'Esta oferta tiene reservas y no se puede borrar. Puedes pausarla.',
      },
    });
    expect(db.tables.offers).toHaveLength(1);
  });

  it('sanitizes database failures', async () => {
    db.client.offer.findMany.mockRejectedValueOnce(
      new Error('private-database-details'),
    );

    const result = service.list(mine.userId);

    await expect(result).rejects.toMatchObject({
      response: { statusCode: 500, error: 'Error interno' },
    });
    await expect(result).rejects.not.toThrow('private-database-details');
  });
});
