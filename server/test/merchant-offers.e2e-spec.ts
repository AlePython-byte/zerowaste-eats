import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import {
  addBusiness,
  addOffer,
  createFakeOffersDb,
} from './fixtures/fake-offers-db';
import { as, createE2eApp } from './support/e2e-app';

/** Offers far enough ahead that the real clock never sees them expire during the run. */
const inHours = (hours: number) => new Date(Date.now() + hours * 3600_000);

describe('Merchant offers (e2e)', () => {
  let app: INestApplication<App>;
  let db: ReturnType<typeof createFakeOffersDb>;
  let mine: ReturnType<typeof addBusiness>;
  let theirs: ReturnType<typeof addBusiness>;
  const customerId = '7e57c0de-0000-4000-8000-00000000c057';

  beforeEach(async () => {
    db = createFakeOffersDb();
    mine = addBusiness(db.tables);
    theirs = addBusiness(db.tables);
    db.tables.users.push({ id: customerId, role: 'CUSTOMER' });
    app = await createE2eApp(db.client);
  });

  afterEach(async () => {
    await app.close();
  });

  const body = () => ({
    title: '  Bolsa de pan del día  ',
    description: 'Pan de esta mañana.',
    category: 'BAKERY',
    originalPrice: 12000,
    discountedPrice: 6000,
    quantityTotal: 4,
    pickupStart: inHours(2).toISOString(),
    pickupEnd: inHours(3).toISOString(),
  });

  it('creates, lists, edits, pauses, resumes and deletes an own offer', async () => {
    const created = await as(app, mine.userId)
      .post('/api/v1/merchant/offers')
      .send(body())
      .expect(201);
    expect(created.body).toMatchObject({
      title: 'Bolsa de pan del día',
      status: 'AVAILABLE',
      quantityAvailable: 4,
      discountPercent: 50,
    });
    const id = created.body.id;
    const path = `/api/v1/merchant/offers/${id}`;

    const listed = await as(app, mine.userId)
      .get('/api/v1/merchant/offers')
      .expect(200);
    expect(listed.body.map((o: { id: string }) => o.id)).toEqual([id]);

    await as(app, mine.userId)
      .patch(path)
      .send({ discountedPrice: 5000 })
      .expect(200)
      .expect((r) => expect(r.body.discountPercent).toBe(58));
    await as(app, mine.userId)
      .post(`${path}/pause`)
      .expect(200)
      .expect((r) => expect(r.body.status).toBe('PAUSED'));
    await as(app, mine.userId)
      .get('/api/v1/merchant/offers?status=PAUSED')
      .expect(200)
      .expect((r) => expect(r.body).toHaveLength(1));
    await as(app, mine.userId)
      .post(`${path}/resume`)
      .expect(200)
      .expect((r) => expect(r.body.status).toBe('AVAILABLE'));
    await as(app, mine.userId).delete(path).expect(204);
    await as(app, mine.userId).get(path).expect(404);
  });

  it.each([
    ['get', ''],
    ['patch', ''],
    ['post', '/pause'],
    ['post', '/resume'],
    ['delete', ''],
  ] as const)(
    'answers %s%s on another business offer with 404',
    async (method, suffix) => {
      const foreign = addOffer(db.tables, theirs.establishment.id, {
        pickupStart: inHours(1),
        pickupEnd: inHours(2),
      });
      const snapshot = { ...foreign };

      const call = as(app, mine.userId)[method](
        `/api/v1/merchant/offers/${foreign.id}${suffix}`,
      );
      await (method === 'patch' ? call.send({ title: 'Robada' }) : call)
        .expect(404)
        .expect({
          statusCode: 404,
          error: 'No encontrado',
          message: 'No encontramos esa oferta.',
        });
      expect(db.tables.offers).toEqual([snapshot]);
    },
  );

  it('answers a malformed id like an unknown offer', () =>
    as(app, mine.userId).get('/api/v1/merchant/offers/123').expect(404));

  it('is only for businesses', async () => {
    await as(app, customerId).get('/api/v1/merchant/offers').expect(403);
    await as(app, customerId)
      .post('/api/v1/merchant/offers')
      .send(body())
      .expect(403);
    expect(db.tables.offers).toHaveLength(0);
  });

  it('requires a valid session', () =>
    request(app.getHttpServer()).get('/api/v1/merchant/offers').expect(401));

  it.each([
    ['a short title', { title: 'Pa' }],
    ['an unknown category', { category: 'SNACKS' }],
    ['a fractional price', { originalPrice: 1200.5 }],
    ['a price over the limit', { originalPrice: 20_000_000 }],
    ['no units', { quantityTotal: 0 }],
    ['too many units', { quantityTotal: 100 }],
    ['a date that is not ISO', { pickupEnd: 'mañana' }],
    ['a field the client may not set', { status: 'PAUSED' }],
    ["someone else's id", { establishmentId: 'not-a-uuid' }],
    ['a long description', { description: 'x'.repeat(241) }],
  ])('refuses %s with a safe 400', async (_label, change) => {
    await as(app, mine.userId)
      .post('/api/v1/merchant/offers')
      .send({ ...body(), ...change })
      .expect(400)
      .expect({
        statusCode: 400,
        error: 'Solicitud inválida',
        message: 'Los datos enviados no son válidos.',
      });
    expect(db.tables.offers).toHaveLength(0);
  });

  it('shows a past offer as EXPIRED and refuses to resume it', async () => {
    const past = addOffer(db.tables, mine.establishment.id, {
      status: 'PAUSED',
      pickupStart: inHours(-3),
      pickupEnd: inHours(-2),
    });

    await as(app, mine.userId)
      .get(`/api/v1/merchant/offers/${past.id}`)
      .expect(200)
      .expect((r) => expect(r.body.status).toBe('EXPIRED'));
    await as(app, mine.userId)
      .post(`/api/v1/merchant/offers/${past.id}/resume`)
      .expect(409);
  });
});
