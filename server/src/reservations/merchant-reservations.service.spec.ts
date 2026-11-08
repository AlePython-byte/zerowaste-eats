import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from '../../test/fixtures/fake-offers-db';
import { MerchantReservationsService } from './merchant-reservations.service';
import { ReservationsService } from './reservations.service';

const NOW = new Date('2026-11-07T20:00:00Z');
const ANA = '7e57c0de-0000-4000-8000-0000000000a1';
const LUIS = '7e57c0de-0000-4000-8000-0000000000b2';

describe('Reservation listings', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let customers: ReservationsService;
  let businesses: MerchantReservationsService;
  let bakery: ReturnType<typeof addBusiness>;
  let deli: ReturnType<typeof addBusiness>;
  let bread: string;
  let soup: string;

  beforeEach(() => {
    db = createFakeOffersDb();
    db.tables.users.push(
      { id: ANA, role: 'CUSTOMER', displayName: 'Ana' },
      { id: LUIS, role: 'CUSTOMER', displayName: 'Luis' },
    );
    bakery = addBusiness(db.tables);
    deli = addBusiness(db.tables, { businessName: 'Comidas Doña Rosa' });
    bread = addOffer(db.tables, bakery.establishment.id, {
      quantityAvailable: 9,
    }).id;
    soup = addOffer(db.tables, deli.establishment.id, {
      title: 'Sopa del día',
      quantityAvailable: 9,
    }).id;
    customers = new ReservationsService(db.service);
    customers.clock = () => NOW;
    businesses = new MerchantReservationsService(db.service);
    businesses.clock = () => NOW;
  });

  /** Reserves, then dates the reservation [minutesAgo] so the order is deterministic. */
  async function reserve(
    customerId: string,
    offerId: string,
    minutesAgo: number,
    status = 'PENDING',
  ) {
    const view = await customers.reserve(customerId, { offerId, quantity: 1 });
    const row = db.tables.reservations.find((r) => r.id === view.id)!;
    row.createdAt = new Date(NOW.getTime() - minutesAgo * 60_000);
    row.status = status;
    return view.id;
  }

  describe('for the customer', () => {
    it('lists only their own reservations, newest first, with offer and business', async () => {
      const older = await reserve(ANA, bread, 30);
      const newer = await reserve(ANA, soup, 5);
      await reserve(LUIS, bread, 1);

      const list = await customers.listForCustomer(ANA);

      expect(list.map((r) => r.id)).toEqual([newer, older]);
      expect(list[0]).toMatchObject({
        code: expect.stringMatching(/^ZW-/),
        offer: { id: soup, title: 'Sopa del día', status: 'AVAILABLE' },
        business: { name: 'Comidas Doña Rosa', address: 'Centro de Pasto' },
      });
      expect(await customers.listForCustomer(LUIS)).toHaveLength(1);
    });

    it('shows an empty list to someone who never reserved', async () => {
      await reserve(ANA, bread, 1);
      expect(
        await customers.listForCustomer('7e57c0de-0000-4000-8000-0000000000ff'),
      ).toEqual([]);
    });
  });

  describe('for the business', () => {
    it('lists pending reservations first (oldest first), then the rest (newest first)', async () => {
      const done = await reserve(ANA, bread, 60, 'COMPLETED');
      const waitingLong = await reserve(ANA, bread, 50);
      const cancelled = await reserve(LUIS, bread, 10, 'CANCELLED');
      const waitingShort = await reserve(LUIS, bread, 5);

      const list = await businesses.list(bakery.userId);

      expect(list.map((r) => r.id)).toEqual([
        waitingLong,
        waitingShort,
        cancelled,
        done,
      ]);
    });

    it("only sees reservations of its own offers, and never the customer's code", async () => {
      await reserve(ANA, bread, 10);
      const atDeli = await reserve(LUIS, soup, 5);

      const list = await businesses.list(deli.userId);

      expect(list).toHaveLength(1);
      expect(list[0]).toMatchObject({
        id: atDeli,
        status: 'PENDING',
        customer: { displayName: 'Luis' },
        offer: { id: soup, title: 'Sopa del día' },
      });
      expect(list[0]).not.toHaveProperty('code');
      expect(JSON.stringify(list)).not.toMatch(/ZW-/);
    });

    it('hides database failures behind a safe 500', async () => {
      db.client.reservation.findMany.mockRejectedValueOnce(new Error('x'));

      await expect(businesses.list(bakery.userId)).rejects.toMatchObject({
        response: { statusCode: 500 },
      });
    });
  });
});
