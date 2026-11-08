import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from '../../test/fixtures/fake-offers-db';
import { ReservationsService } from './reservations.service';

const NOW = new Date('2026-11-07T20:00:00Z');
const ANA = '7e57c0de-0000-4000-8000-0000000000a1';
const LUIS = '7e57c0de-0000-4000-8000-0000000000b2';

describe('ReservationsService', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let reservations: ReservationsService;
  let business: ReturnType<typeof addBusiness>;

  beforeEach(() => {
    db = createFakeOffersDb();
    db.tables.users.push(
      { id: ANA, role: 'CUSTOMER', displayName: 'Ana' },
      { id: LUIS, role: 'CUSTOMER', displayName: 'Luis' },
    );
    business = addBusiness(db.tables);
    reservations = new ReservationsService(db.service);
    reservations.clock = () => NOW;
  });

  const offerWith = (overrides: Record<string, unknown> = {}) =>
    addOffer(db.tables, business.establishment.id, overrides);
  /** The stored row now: a rolled-back transaction replaces the row objects. */
  const stored = (offerId: string) =>
    db.tables.offers.find((o) => o.id === offerId)!;

  describe('reserve', () => {
    it('takes the units, keeps the price paid and returns a pickup code', async () => {
      const offer = offerWith({ quantityAvailable: 5 });

      const reservation = await reservations.reserve(ANA, {
        offerId: offer.id,
        quantity: 2,
      });

      expect(reservation).toMatchObject({
        code: expect.stringMatching(/^ZW-[2-9A-HJ-NP-Z]{4}$/),
        status: 'PENDING',
        quantity: 2,
        unitPrice: 6000,
        totalPrice: 12000,
        offer: { id: offer.id, title: 'Bolsa de pan del día' },
        business: { name: 'Panadería La Esquina' },
      });
      expect(offer).toMatchObject({
        quantityAvailable: 3,
        status: 'AVAILABLE',
      });
      expect(db.tables.reservations).toEqual([
        expect.objectContaining({
          customerId: ANA,
          offerId: offer.id,
          reservationCode: reservation.code,
        }),
      ]);
    });

    it('marks the offer sold out when the last unit goes', async () => {
      const offer = offerWith({ quantityAvailable: 2 });

      await reservations.reserve(ANA, { offerId: offer.id, quantity: 2 });

      expect(offer).toMatchObject({ quantityAvailable: 0, status: 'SOLD_OUT' });
    });

    it('lets only one of two simultaneous reservations take the last unit', async () => {
      const offer = offerWith({ quantityAvailable: 1 });

      const results = await Promise.allSettled([
        reservations.reserve(ANA, { offerId: offer.id, quantity: 1 }),
        reservations.reserve(LUIS, { offerId: offer.id, quantity: 1 }),
      ]);

      expect(results.filter((r) => r.status === 'fulfilled')).toHaveLength(1);
      const [lost] = results.filter((r) => r.status === 'rejected');
      expect((lost as PromiseRejectedResult).reason).toMatchObject({
        response: {
          statusCode: 409,
          message: 'Ya no quedan unidades de esta oferta.',
        },
      });
      expect(db.tables.reservations).toHaveLength(1);
      expect(offer).toMatchObject({ quantityAvailable: 0, status: 'SOLD_OUT' });
    });

    it('never sells more units than there are, however many ask at once', async () => {
      const offer = offerWith({ quantityAvailable: 3 });
      const customers = Array.from(
        { length: 8 },
        (_, i) => `7e57c0de-0000-4000-8000-00000000010${i}`,
      );

      const results = await Promise.allSettled(
        customers.map((id) =>
          reservations.reserve(id, { offerId: offer.id, quantity: 1 }),
        ),
      );

      expect(results.filter((r) => r.status === 'fulfilled')).toHaveLength(3);
      expect(offer.quantityAvailable).toBe(0);
    });

    it('says how many are left when asking for more', async () => {
      const offer = offerWith({ quantityAvailable: 2 });

      await expect(
        reservations.reserve(ANA, { offerId: offer.id, quantity: 3 }),
      ).rejects.toMatchObject({
        response: {
          statusCode: 409,
          message: 'Solo quedan 2 unidades; no alcanzan para 3.',
        },
      });
      expect(offer.quantityAvailable).toBe(2);
      expect(db.tables.reservations).toEqual([]);
    });

    it.each([
      ['sold out', { quantityAvailable: 0, status: 'SOLD_OUT' }],
      ['paused', { status: 'PAUSED' }],
      ['cancelled', { status: 'CANCELLED' }],
      [
        'over',
        {
          pickupStart: new Date('2026-11-07T17:00:00Z'),
          pickupEnd: new Date('2026-11-07T19:00:00Z'),
          expiresAt: new Date('2026-11-07T19:00:00Z'),
        },
      ],
    ])('refuses a %s offer with 409', async (_label, overrides) => {
      const offer = offerWith(overrides);
      const before = offer.quantityAvailable;

      await expect(
        reservations.reserve(ANA, { offerId: offer.id, quantity: 1 }),
      ).rejects.toMatchObject({
        response: {
          statusCode: 409,
          message: 'Ya no quedan unidades de esta oferta.',
        },
      });
      expect(offer.quantityAvailable).toBe(before);
    });

    it('answers an offer that does not exist with 404', async () => {
      await expect(
        reservations.reserve(ANA, {
          offerId: '00000000-0000-4000-8000-000000000000',
          quantity: 1,
        }),
      ).rejects.toMatchObject({ response: { statusCode: 404 } });
    });

    it('draws a new code when the first one is taken, without taking units twice', async () => {
      const first = offerWith({ quantityAvailable: 5 });
      const second = offerWith({ quantityAvailable: 5 });
      const codes = ['ZW-AAAA', 'ZW-AAAA', 'ZW-BBBB'];
      reservations.nextCode = () => codes.shift()!;

      await reservations.reserve(ANA, { offerId: first.id, quantity: 1 });
      const again = await reservations.reserve(LUIS, {
        offerId: second.id,
        quantity: 2,
      });

      expect(again.code).toBe('ZW-BBBB');
      expect(stored(second.id).quantityAvailable).toBe(3);
      expect(db.tables.reservations.map((r) => r.reservationCode)).toEqual([
        'ZW-AAAA',
        'ZW-BBBB',
      ]);
    });

    it('gives up with a safe 500 if every code drawn is taken', async () => {
      const offer = offerWith({ quantityAvailable: 5 });
      reservations.nextCode = () => 'ZW-AAAA';
      await reservations.reserve(ANA, { offerId: offer.id, quantity: 1 });

      await expect(
        reservations.reserve(LUIS, { offerId: offer.id, quantity: 1 }),
      ).rejects.toMatchObject({ response: { statusCode: 500 } });
      expect(stored(offer.id).quantityAvailable).toBe(4);
    });

    it('hides database failures behind a safe 500', async () => {
      db.client.$transaction.mockRejectedValueOnce(
        new Error('connection details'),
      );

      await expect(
        reservations.reserve(ANA, { offerId: offerWith().id, quantity: 1 }),
      ).rejects.toMatchObject({
        response: {
          statusCode: 500,
          message: 'No fue posible completar la operación con tus reservas.',
        },
      });
    });
  });
});
