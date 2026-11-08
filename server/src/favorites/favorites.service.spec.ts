import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from '../../test/fixtures/fake-offers-db';
import { OffersService } from '../offers/offers.service';
import { FavoritesService } from './favorites.service';

const NOW = new Date('2026-11-07T20:00:00Z');
const ANA = '7e57c0de-0000-4000-8000-0000000000a1';
const LUIS = '7e57c0de-0000-4000-8000-0000000000b2';

describe('FavoritesService', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let favorites: FavoritesService;
  let offerId: string;
  let business: ReturnType<typeof addBusiness>;

  beforeEach(() => {
    db = createFakeOffersDb();
    db.tables.users.push(
      { id: ANA, role: 'CUSTOMER' },
      { id: LUIS, role: 'CUSTOMER' },
    );
    business = addBusiness(db.tables);
    offerId = addOffer(db.tables, business.establishment.id).id;
    favorites = new FavoritesService(db.service);
    favorites.clock = () => NOW;
  });

  it('saves an offer once, however many times it is added', async () => {
    await favorites.add(ANA, offerId);
    await favorites.add(ANA, offerId);

    expect(db.tables.favorites).toEqual([
      expect.objectContaining({ userId: ANA, offerId }),
    ]);
  });

  it('removes a saved offer, and removing again changes nothing', async () => {
    await favorites.add(ANA, offerId);

    await favorites.remove(ANA, offerId);
    await favorites.remove(ANA, offerId);

    expect(db.tables.favorites).toEqual([]);
  });

  it("never touches another person's favorites", async () => {
    await favorites.add(ANA, offerId);
    await favorites.add(LUIS, offerId);

    await favorites.remove(LUIS, offerId);

    expect(db.tables.favorites).toEqual([
      expect.objectContaining({ userId: ANA }),
    ]);
    expect(await favorites.list(LUIS)).toEqual([]);
    expect((await favorites.list(ANA)).map((f) => f.id)).toEqual([offerId]);
  });

  it.each([
    ['paused', { status: 'PAUSED' }],
    ['sold out', { quantityAvailable: 0 }],
    [
      'expired',
      {
        pickupStart: new Date('2026-11-07T17:00:00Z'),
        pickupEnd: new Date('2026-11-07T19:00:00Z'),
      },
    ],
  ])('cannot save a %s offer', async (_label, overrides) => {
    const hidden = addOffer(db.tables, business.establishment.id, overrides).id;

    await expect(favorites.add(ANA, hidden)).rejects.toMatchObject({
      response: {
        statusCode: 404,
        message: 'Esta oferta ya no está disponible.',
      },
    });
    expect(db.tables.favorites).toEqual([]);
  });

  it('asks someone without a profile to complete it first', async () => {
    await expect(
      favorites.add('7e57c0de-0000-4000-8000-0000000000ff', offerId),
    ).rejects.toMatchObject({
      response: {
        statusCode: 403,
        message: 'Completa tu perfil para guardar favoritos.',
      },
    });
  });

  it('keeps a saved offer that went off sale, marked as not available', async () => {
    await favorites.add(ANA, offerId);
    db.tables.offers[0].status = 'PAUSED';

    const [saved] = await favorites.list(ANA);

    expect(saved).toMatchObject({
      id: offerId,
      isFavorite: true,
      status: 'PAUSED',
      isAvailable: false,
    });
  });

  it('marks as not available a saved offer whose pickup ended', async () => {
    await favorites.add(ANA, offerId);
    favorites.clock = () => new Date('2026-11-07T23:30:00Z');

    expect((await favorites.list(ANA))[0]).toMatchObject({
      status: 'EXPIRED',
      isAvailable: false,
    });
  });

  it('lists the newest favorites first', async () => {
    const second = addOffer(db.tables, business.establishment.id, {
      title: 'Segunda',
    }).id;
    await favorites.add(ANA, offerId);
    db.tables.favorites[0].createdAt = new Date('2026-11-07T10:00:00Z');
    await favorites.add(ANA, second);

    expect((await favorites.list(ANA)).map((f) => f.id)).toEqual([
      second,
      offerId,
    ]);
  });

  it('marks the saved offers in the nearby list, for that person only', async () => {
    const other = addOffer(db.tables, business.establishment.id, {
      title: 'Otra',
    }).id;
    await favorites.add(ANA, offerId);
    const offers = new OffersService(db.service);
    offers.clock = () => NOW;
    const near = { lat: 1.213611, lng: -77.281111 } as never;

    const forAna = await offers.nearby(near, (ids) =>
      favorites.savedAmong(ANA, ids),
    );
    const forLuis = await offers.nearby(near, (ids) =>
      favorites.savedAmong(LUIS, ids),
    );

    expect(Object.fromEntries(forAna.map((o) => [o.id, o.isFavorite]))).toEqual(
      {
        [offerId]: true,
        [other]: false,
      },
    );
    expect(forLuis.every((o) => !o.isFavorite)).toBe(true);
  });

  it('asks the database for the marks in one query', async () => {
    await favorites.savedAmong(ANA, ['a', 'b', 'c']);
    await favorites.savedAmong(ANA, []);

    expect(db.client.favorite.findMany).toHaveBeenCalledTimes(1);
  });
});
