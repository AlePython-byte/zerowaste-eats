import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from '../../test/fixtures/fake-offers-db';
import { OffersService } from './offers.service';

const NOW = new Date('2026-11-07T20:00:00Z');
const PASTO = { lat: 1.213611, lng: -77.281111 };
/** Degrees of latitude for [km] kilometres north. */
const north = (km: number) => 1.213611 + km / 111.195;

describe('OffersService', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let service: OffersService;
  let downtown: ReturnType<typeof addBusiness>;

  beforeEach(() => {
    db = createFakeOffersDb();
    downtown = addBusiness(db.tables, { businessName: 'Panadería La Esquina' });
    service = new OffersService(db.service);
    service.clock = () => NOW;
  });

  const titles = async (query: Record<string, unknown> = {}) =>
    (await service.nearby({ ...PASTO, ...query } as never)).map((o) => o.title);

  // --- Radius ---

  describe('radius', () => {
    beforeEach(() => {
      addOffer(db.tables, downtown.establishment.id, { title: 'Aquí' });
      const near = addBusiness(db.tables, { latitude: north(2) });
      addOffer(db.tables, near.establishment.id, { title: 'A 2 km' });
      const edge = addBusiness(db.tables, { latitude: north(4.9) });
      addOffer(db.tables, edge.establishment.id, { title: 'A 4.9 km' });
      const outside = addBusiness(db.tables, { latitude: north(5.2) });
      addOffer(db.tables, outside.establishment.id, { title: 'A 5.2 km' });
      const far = addBusiness(db.tables, { latitude: north(30) });
      addOffer(db.tables, far.establishment.id, { title: 'A 30 km' });
    });

    it('lists the offers within 5 km by default, nearest first, with their distance', async () => {
      const offers = await service.nearby({ ...PASTO } as never);

      expect(offers.map((o) => [o.title, o.distanceKm])).toEqual([
        ['Aquí', 0],
        ['A 2 km', 2],
        ['A 4.9 km', 4.9],
      ]);
    });

    it('follows the requested radius', async () => {
      expect(await titles({ radiusKm: 1 })).toEqual(['Aquí']);
      expect(await titles({ radiusKm: 50 })).toEqual([
        'Aquí',
        'A 2 km',
        'A 4.9 km',
        'A 5.2 km',
        'A 30 km',
      ]);
    });

    it('lets the database discard far establishments before measuring', async () => {
      await service.nearby({ ...PASTO, radiusKm: 5 } as never);

      const [{ where }] = db.client.offer.findMany.mock.calls[0] as [
        { where: any },
      ];
      expect(where.establishment.latitude.gte).toBeCloseTo(north(-5), 4);
      expect(where.establishment.latitude.lte).toBeCloseTo(north(5), 4);
      expect(where.establishment.longitude.gte).toBeLessThan(PASTO.lng);
      expect(where.establishment.longitude.lte).toBeGreaterThan(PASTO.lng);
    });
  });

  // --- What customers may see ---

  it('only shows offers on sale, with units, still in time', async () => {
    const id = downtown.establishment.id;
    addOffer(db.tables, id, { title: 'Disponible' });
    addOffer(db.tables, id, { title: 'Pausada', status: 'PAUSED' });
    addOffer(db.tables, id, {
      title: 'Agotada',
      status: 'SOLD_OUT',
      quantityAvailable: 0,
    });
    addOffer(db.tables, id, { title: 'Sin unidades', quantityAvailable: 0 });
    addOffer(db.tables, id, { title: 'Cancelada', status: 'CANCELLED' });
    addOffer(db.tables, id, {
      title: 'Vencida',
      pickupStart: new Date('2026-11-07T17:00:00Z'),
      pickupEnd: new Date('2026-11-07T19:00:00Z'),
    });
    addOffer(db.tables, id, { title: 'Vence justo ahora', pickupEnd: NOW });

    expect(await titles()).toEqual(['Disponible']);
  });

  it('drops an offer from the list the moment its pickup ends', async () => {
    addOffer(db.tables, downtown.establishment.id, { title: 'Pan' });

    service.clock = () => new Date('2026-11-07T22:59:59Z');
    expect(await titles()).toEqual(['Pan']);
    service.clock = () => new Date('2026-11-07T23:00:00Z');
    expect(await titles()).toEqual([]);
  });

  // --- Search, category and order ---

  describe('filters and order', () => {
    beforeEach(() => {
      const id = downtown.establishment.id;
      addOffer(db.tables, id, {
        title: 'Croissants del día',
        category: 'BAKERY',
        originalPrice: 15000,
        discountedPrice: 9000,
        pickupEnd: new Date('2026-11-07T23:30:00Z'),
      });
      addOffer(db.tables, id, {
        title: 'Caja de postres',
        description: 'Tortas y brownies',
        category: 'DESSERTS',
        originalPrice: 20000,
        discountedPrice: 8000,
        pickupEnd: new Date('2026-11-07T21:00:00Z'),
      });
      const veggies = addBusiness(db.tables, {
        businessName: 'Verduras Doña Rosa',
        latitude: north(1),
      });
      addOffer(db.tables, veggies.establishment.id, {
        title: 'Canasta de verduras',
        category: 'PRODUCE',
        originalPrice: 10000,
        discountedPrice: 7000,
        pickupEnd: new Date('2026-11-07T22:00:00Z'),
      });
    });

    it('searches the title, the description and the business name, ignoring case', async () => {
      expect(await titles({ q: 'CROISS' })).toEqual(['Croissants del día']);
      expect(await titles({ q: 'brownies' })).toEqual(['Caja de postres']);
      expect(await titles({ q: 'doña rosa' })).toEqual(['Canasta de verduras']);
      expect(await titles({ q: 'pizza' })).toEqual([]);
    });

    it('filters by category', async () => {
      expect(await titles({ category: 'DESSERTS' })).toEqual([
        'Caja de postres',
      ]);
    });

    it('sorts by price, discount or pickup end when asked', async () => {
      expect(await titles({ sort: 'price' })).toEqual([
        'Canasta de verduras',
        'Caja de postres',
        'Croissants del día',
      ]);
      expect(await titles({ sort: 'discount' })).toEqual([
        'Caja de postres',
        'Croissants del día',
        'Canasta de verduras',
      ]);
      expect(await titles({ sort: 'pickup' })).toEqual([
        'Caja de postres',
        'Canasta de verduras',
        'Croissants del día',
      ]);
    });

    it('limits the number of results', async () => {
      expect(await titles({ limit: 2 })).toHaveLength(2);
    });

    it('shows the business behind each offer', async () => {
      const [offer] = await service.nearby({
        ...PASTO,
        q: 'verduras',
      } as never);

      expect(offer).toMatchObject({
        discountPercent: 30,
        distanceKm: 1,
        isFavorite: false,
        business: {
          name: 'Verduras Doña Rosa',
          address: 'Centro de Pasto',
          city: 'Pasto',
        },
      });
    });
  });

  // --- Detail ---

  it('shows the detail of an offer on sale, with the distance when the location is known', async () => {
    const near = addBusiness(db.tables, { latitude: north(3) });
    const offer = addOffer(db.tables, near.establishment.id, {
      description: 'Pan de hoy',
    });

    await expect(
      service.detail(offer.id, { latitude: PASTO.lat, longitude: PASTO.lng }),
    ).resolves.toMatchObject({
      id: offer.id,
      description: 'Pan de hoy',
      distanceKm: 3,
    });
    await expect(service.detail(offer.id, null)).resolves.toMatchObject({
      distanceKm: null,
    });
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
  ])('hides the detail of a %s offer', async (_label, overrides) => {
    const offer = addOffer(db.tables, downtown.establishment.id, overrides);

    await expect(service.detail(offer.id, null)).rejects.toMatchObject({
      response: {
        statusCode: 404,
        message: 'Esta oferta ya no está disponible.',
      },
    });
  });

  it('sanitizes database failures', async () => {
    db.client.offer.findMany.mockRejectedValueOnce(
      new Error('private-database-details'),
    );

    const result = service.nearby({ ...PASTO } as never);

    await expect(result).rejects.toMatchObject({
      response: { statusCode: 500 },
    });
    await expect(result).rejects.not.toThrow('private-database-details');
  });
});
