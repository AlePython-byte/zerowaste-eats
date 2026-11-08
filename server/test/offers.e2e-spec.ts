import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from './fixtures/fake-offers-db';
import { as, createE2eApp } from './support/e2e-app';

const inHours = (hours: number) => new Date(Date.now() + hours * 3600_000);
const north = (km: number) => 1.213611 + km / 111.195;
const HERE = 'lat=1.213611&lng=-77.281111';

describe('Customer offers (e2e)', () => {
  let app: INestApplication<App>;
  let db: ReturnType<typeof createFakeOffersDb>;
  const customerId = '7e57c0de-0000-4000-8000-00000000c057';

  beforeEach(async () => {
    db = createFakeOffersDb();
    db.tables.users.push({ id: customerId, role: 'CUSTOMER' });
    const downtown = addBusiness(db.tables);
    addOffer(db.tables, downtown.establishment.id, {
      title: 'Pan del día',
      pickupStart: inHours(1),
      pickupEnd: inHours(2),
    });
    addOffer(db.tables, downtown.establishment.id, {
      title: 'Pausada',
      status: 'PAUSED',
      pickupStart: inHours(1),
      pickupEnd: inHours(2),
    });
    const far = addBusiness(db.tables, {
      businessName: 'Verduras Lejanas',
      latitude: north(12),
    });
    addOffer(db.tables, far.establishment.id, {
      title: 'Lejos',
      pickupStart: inHours(1),
      pickupEnd: inHours(2),
    });
    app = await createE2eApp(db.client);
  });

  afterEach(async () => {
    await app.close();
  });

  it('lists offers on sale near the customer', async () => {
    const response = await as(app, customerId)
      .get(`/api/v1/offers?${HERE}`)
      .expect(200);

    expect(response.body.map((o: { title: string }) => o.title)).toEqual([
      'Pan del día',
    ]);
    expect(response.body[0]).toMatchObject({
      distanceKm: 0,
      isFavorite: false,
    });
  });

  it('reads the query numbers and filters from text', async () => {
    const response = await as(app, customerId)
      .get(`/api/v1/offers?${HERE}&radiusKm=20&sort=distance&limit=10&q=pan`)
      .expect(200);

    expect(response.body.map((o: { title: string }) => o.title)).toEqual([
      'Pan del día',
    ]);
  });

  it('is open to businesses browsing too', async () => {
    const business = addBusiness(db.tables);

    await as(app, business.userId).get(`/api/v1/offers?${HERE}`).expect(200);
  });

  it('requires a session', () =>
    request(app.getHttpServer()).get(`/api/v1/offers?${HERE}`).expect(401));

  it.each([
    ['no location', ''],
    ['a latitude out of range', 'lat=95&lng=-77'],
    ['text as a coordinate', 'lat=norte&lng=-77'],
    ['a radius over 50 km', `${HERE}&radiusKm=80`],
    ['an unknown sort', `${HERE}&sort=random`],
    ['an unknown category', `${HERE}&category=SNACKS`],
    ['a one-letter search', `${HERE}&q=p`],
    ['too many results', `${HERE}&limit=500`],
    ['an unknown parameter', `${HERE}&userId=otro`],
  ])('refuses %s with a safe 400', (_label, query) =>
    as(app, customerId).get(`/api/v1/offers?${query}`).expect(400).expect({
      statusCode: 400,
      error: 'Solicitud inválida',
      message: 'Los datos enviados no son válidos.',
    }),
  );

  it('shows the detail of an offer on sale and hides a paused one', async () => {
    const [onSale, paused] = db.tables.offers;

    await as(app, customerId)
      .get(`/api/v1/offers/${onSale.id}?${HERE}`)
      .expect(200)
      .expect((r) =>
        expect(r.body).toMatchObject({ title: 'Pan del día', distanceKm: 0 }),
      );
    await as(app, customerId).get(`/api/v1/offers/${paused.id}`).expect(404);
    await as(app, customerId).get('/api/v1/offers/no-es-un-id').expect(404);
  });
});
