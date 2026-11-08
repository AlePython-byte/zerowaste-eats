import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from '../../test/fixtures/fake-offers-db';
import { CustomerStatsService } from './customer-stats.service';

const ANA = '7e57c0de-0000-4000-8000-0000000000a1';
const LUIS = '7e57c0de-0000-4000-8000-0000000000b2';

describe('CustomerStatsService', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let stats: CustomerStatsService;
  let establishmentId: string;

  beforeEach(() => {
    db = createFakeOffersDb();
    establishmentId = addBusiness(db.tables).establishment.id;
    stats = new CustomerStatsService(db.service);
  });

  /** A reservation as stored: unitPrice is what was paid per unit. */
  function reservation(
    customerId: string,
    offer: { originalPrice: number },
    quantity: number,
    unitPrice: number,
    status = 'COMPLETED',
  ) {
    const { id: offerId } = addOffer(db.tables, establishmentId, offer);
    db.tables.reservations.push({
      id: `r-${db.tables.reservations.length}`,
      customerId,
      offerId,
      quantity,
      unitPrice,
      totalPrice: unitPrice * quantity,
      status,
    });
  }

  it('adds up completed pickups: orders, savings and estimated kilos', async () => {
    reservation(ANA, { originalPrice: 12000 }, 2, 6000); // saves 12 000
    reservation(ANA, { originalPrice: 9000 }, 1, 4500); // saves 4 500
    reservation(ANA, { originalPrice: 20000 }, 3, 10000, 'PENDING');
    reservation(ANA, { originalPrice: 20000 }, 1, 10000, 'CANCELLED');
    reservation(LUIS, { originalPrice: 20000 }, 5, 1000);

    expect(await stats.forCustomer(ANA)).toEqual({
      completedOrders: 2,
      unitsRescued: 3,
      moneySaved: 16500,
      foodRescuedKg: 1.2,
      foodRescuedKgIsEstimate: true,
      kgPerUnitEstimate: 0.4,
    });
  });

  it('is all zeros for someone with no pickups', async () => {
    reservation(LUIS, { originalPrice: 20000 }, 5, 1000);

    expect(await stats.forCustomer(ANA)).toMatchObject({
      completedOrders: 0,
      unitsRescued: 0,
      moneySaved: 0,
      foodRescuedKg: 0,
      foodRescuedKgIsEstimate: true,
    });
  });

  it('adds decimal prices exactly', async () => {
    for (let i = 0; i < 10; i++) {
      reservation(ANA, { originalPrice: 0.3 }, 1, 0.2);
    }

    expect((await stats.forCustomer(ANA)).moneySaved).toBe(1);
  });

  it('never counts a negative saving if the original price was lowered later', async () => {
    reservation(ANA, { originalPrice: 5000 }, 2, 6000);

    expect((await stats.forCustomer(ANA)).moneySaved).toBe(0);
  });

  it('rounds the estimated kilos to one decimal', async () => {
    reservation(ANA, { originalPrice: 12000 }, 7, 6000);

    expect((await stats.forCustomer(ANA)).foodRescuedKg).toBe(2.8);
  });

  it('hides database failures behind a safe 500', async () => {
    db.client.reservation.findMany.mockRejectedValueOnce(new Error('x'));

    await expect(stats.forCustomer(ANA)).rejects.toMatchObject({
      response: {
        statusCode: 500,
        message: 'No fue posible calcular tus estadísticas.',
      },
    });
  });
});
