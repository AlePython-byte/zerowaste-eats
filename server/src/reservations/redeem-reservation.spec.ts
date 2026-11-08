import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from '../../test/fixtures/fake-offers-db';
import { MerchantReservationsService } from './merchant-reservations.service';
import { ReservationsService } from './reservations.service';

const START = new Date('2026-11-07T20:00:00Z');
const ANA = '7e57c0de-0000-4000-8000-0000000000a1';
const minutes = (n: number) => new Date(START.getTime() + n * 60_000);

describe('MerchantReservationsService.redeem', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let businesses: MerchantReservationsService;
  let bakery: ReturnType<typeof addBusiness>;
  let reservationId: string;
  let code: string;
  let now: Date;

  beforeEach(async () => {
    db = createFakeOffersDb();
    db.tables.users.push({ id: ANA, role: 'CUSTOMER', displayName: 'Ana' });
    bakery = addBusiness(db.tables);
    const offer = addOffer(db.tables, bakery.establishment.id);
    const customers = new ReservationsService(db.service);
    customers.clock = () => START;
    ({ id: reservationId, code } = await customers.reserve(ANA, {
      offerId: offer.id,
      quantity: 1,
    }));
    now = START;
    businesses = new MerchantReservationsService(db.service);
    businesses.clock = () => now;
  });

  const stored = () =>
    db.tables.reservations.find((r) => r.id === reservationId)!;
  const redeem = (typed: string, userId = bakery.userId) =>
    businesses.redeem(userId, reservationId, typed);
  const aWrongCode = () => (code === 'ZW-2222' ? 'ZW-3333' : 'ZW-2222');

  it('completes a pending reservation with its code', async () => {
    const view = await redeem(code);

    expect(view).toMatchObject({ id: reservationId, status: 'COMPLETED' });
    expect(view.completedAt).toBe(START.toISOString());
    expect(stored()).toMatchObject({ status: 'COMPLETED', completedAt: START });
  });

  it('accepts the code however it is typed', async () => {
    await expect(
      redeem(` ${code.toLowerCase().replace('-', '')} `),
    ).resolves.toMatchObject({
      status: 'COMPLETED',
    });
  });

  it('redeems once: the second time is "Ya canjeado" (409)', async () => {
    await redeem(code);

    await expect(redeem(code)).rejects.toMatchObject({
      response: {
        statusCode: 409,
        message: 'Ya canjeado: esta reserva ya se entregó.',
      },
    });
  });

  it('does not redeem a cancelled reservation', async () => {
    stored().status = 'CANCELLED';

    await expect(redeem(code)).rejects.toMatchObject({
      response: { statusCode: 409, message: 'Esta reserva fue cancelada.' },
    });
  });

  it("answers another business's reservation with 404, without counting an attempt", async () => {
    const other = addBusiness(db.tables, { businessName: 'Otra' });

    await expect(redeem(code, other.userId)).rejects.toMatchObject({
      response: { statusCode: 404, message: 'No encontramos esa reserva.' },
    });
    expect(stored().status).toBe('PENDING');
    expect(db.tables.redemptionAttempts).toEqual([]);
  });

  it('counts a wrong code and says how many tries are left', async () => {
    await expect(redeem(aWrongCode())).rejects.toMatchObject({
      response: {
        statusCode: 400,
        message:
          'El código no corresponde a esta reserva. Te quedan 4 intentos.',
      },
    });
    expect(stored().status).toBe('PENDING');
    expect(db.tables.redemptionAttempts).toHaveLength(1);
  });

  it('locks after 5 wrong codes in 10 minutes, even for the right code', async () => {
    const answers: number[] = [];
    for (let i = 0; i < 5; i++) {
      now = minutes(i);
      answers.push(
        await redeem(aWrongCode()).then(
          () => 200,
          (e) => e.response.statusCode as number,
        ),
      );
    }
    expect(answers).toEqual([400, 400, 400, 400, 429]);

    now = minutes(5);
    await expect(redeem(code)).rejects.toMatchObject({
      response: {
        statusCode: 429,
        // The first failure (minute 0) ages out at minute 10
        message:
          'Demasiados intentos con un código incorrecto. Intenta de nuevo en 5 minutos.',
      },
    });
    expect(stored().status).toBe('PENDING');
  });

  it('lets the business try again once the oldest failure leaves the window', async () => {
    for (let i = 0; i < 5; i++) {
      now = minutes(i);
      await redeem(aWrongCode()).catch(() => undefined);
    }

    now = minutes(10);
    await expect(redeem(code)).resolves.toMatchObject({ status: 'COMPLETED' });
    // Done with this reservation: its attempts are gone
    expect(db.tables.redemptionAttempts).toEqual([]);
  });

  it('forgets failures older than 10 minutes', async () => {
    for (let i = 0; i < 4; i++) {
      now = minutes(i);
      await redeem(aWrongCode()).catch(() => undefined);
    }
    now = minutes(30);

    await expect(redeem(aWrongCode())).rejects.toMatchObject({
      response: { message: expect.stringContaining('Te quedan 4 intentos') },
    });
    expect(db.tables.redemptionAttempts).toHaveLength(1);
  });

  it('keeps each reservation to its own count', async () => {
    for (let i = 0; i < 5; i++)
      await redeem(aWrongCode()).catch(() => undefined);
    const otherOffer = addOffer(db.tables, bakery.establishment.id);
    const customers = new ReservationsService(db.service);
    customers.clock = () => START;
    const other = await customers.reserve(ANA, {
      offerId: otherOffer.id,
      quantity: 1,
    });

    await expect(
      businesses.redeem(bakery.userId, other.id, other.code),
    ).resolves.toMatchObject({ status: 'COMPLETED' });
  });

  it('locks the reservation row before deciding', async () => {
    await redeem(code);

    expect(db.client.$executeRaw).toHaveBeenCalledTimes(1);
    expect(db.client.$transaction).toHaveBeenCalled();
  });

  it('hides database failures behind a safe 500', async () => {
    db.client.$transaction.mockRejectedValueOnce(new Error('x'));

    await expect(redeem(code)).rejects.toMatchObject({
      response: { statusCode: 500 },
    });
  });

  // Keeps the helper above honest: the code under test is never the wrong one
  it('uses a wrong code that really is wrong', () => {
    expect(aWrongCode()).not.toBe(code);
  });
});
