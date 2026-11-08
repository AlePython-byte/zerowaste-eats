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

describe('Reservations (e2e)', () => {
  let app: INestApplication<App>;
  let db: ReturnType<typeof createFakeOffersDb>;
  let business: ReturnType<typeof addBusiness>;
  let offerId: string;
  const ana = '7e57c0de-0000-4000-8000-0000000000a1';
  const luis = '7e57c0de-0000-4000-8000-0000000000b2';

  beforeEach(async () => {
    db = createFakeOffersDb();
    db.tables.users.push(
      { id: ana, role: 'CUSTOMER', displayName: 'Ana' },
      { id: luis, role: 'CUSTOMER', displayName: 'Luis' },
    );
    business = addBusiness(db.tables);
    offerId = addOffer(db.tables, business.establishment.id, {
      quantityAvailable: 1,
      pickupStart: inHours(1),
      pickupEnd: inHours(2),
      expiresAt: inHours(2),
    }).id;
    app = await createE2eApp(db.client);
  });

  afterEach(async () => {
    await app.close();
  });

  describe('POST /reservations', () => {
    it('reserves the last unit once; the second customer hears there are none left', async () => {
      const [first, second] = await Promise.all([
        as(app, ana)
          .post('/api/v1/reservations')
          .send({ offerId, quantity: 1 }),
        as(app, luis)
          .post('/api/v1/reservations')
          .send({ offerId, quantity: 1 }),
      ]);

      const statuses = [first.status, second.status].sort();
      expect(statuses).toEqual([201, 409]);
      const won = first.status === 201 ? first : second;
      const lost = first.status === 201 ? second : first;
      expect(won.body).toMatchObject({
        code: expect.stringMatching(/^ZW-[2-9A-HJ-NP-Z]{4}$/),
        status: 'PENDING',
        quantity: 1,
        totalPrice: 6000,
      });
      expect(lost.body).toEqual({
        statusCode: 409,
        error: 'Conflicto',
        message: 'Ya no quedan unidades de esta oferta.',
      });
      expect(db.tables.offers[0]).toMatchObject({
        quantityAvailable: 0,
        status: 'SOLD_OUT',
      });
    });

    it('is for customers only', async () => {
      await as(app, business.userId)
        .post('/api/v1/reservations')
        .send({ offerId, quantity: 1 })
        .expect(403);
      expect(db.tables.reservations).toEqual([]);
    });

    it.each([
      ['no offer', { offerId: undefined, quantity: 1 }],
      ['a malformed offer id', { offerId: 'nope', quantity: 1 }],
      ['zero units', { quantity: 0 }],
      ['too many units', { quantity: 11 }],
      ['a fractional quantity', { quantity: 1.5 }],
      ['unknown fields', { quantity: 1, price: 1 }],
    ])('rejects %s with 400', async (_label, body) => {
      await as(app, ana)
        .post('/api/v1/reservations')
        .send({ offerId, ...body })
        .expect(400);
    });

    it('answers an unknown offer with 404', () =>
      as(app, ana)
        .post('/api/v1/reservations')
        .send({ offerId: '00000000-0000-4000-8000-000000000000', quantity: 1 })
        .expect(404));

    it('requires a session', () =>
      request(app.getHttpServer())
        .post('/api/v1/reservations')
        .send({ offerId, quantity: 1 })
        .expect(401));
  });

  describe('GET /reservations and /merchant/reservations', () => {
    it('shows the customer their code and the business its reservations without it', async () => {
      const { body: reserved } = await as(app, ana)
        .post('/api/v1/reservations')
        .send({ offerId, quantity: 1 })
        .expect(201);

      await as(app, ana)
        .get('/api/v1/reservations')
        .expect(200)
        .expect((r) => {
          expect(r.body).toHaveLength(1);
          expect(r.body[0]).toMatchObject({
            id: reserved.id,
            code: reserved.code,
            business: { name: 'Panadería La Esquina' },
          });
        });
      await as(app, luis).get('/api/v1/reservations').expect(200).expect([]);

      await as(app, business.userId)
        .get('/api/v1/merchant/reservations')
        .expect(200)
        .expect((r) => {
          expect(r.body).toEqual([
            expect.objectContaining({
              id: reserved.id,
              status: 'PENDING',
              customer: { displayName: 'Ana' },
            }),
          ]);
          expect(r.body[0]).not.toHaveProperty('code');
        });

      const other = addBusiness(db.tables, { businessName: 'Otra' });
      await as(app, other.userId)
        .get('/api/v1/merchant/reservations')
        .expect(200)
        .expect([]);
    });

    it('keeps each list to its role', async () => {
      await as(app, business.userId).get('/api/v1/reservations').expect(403);
      await as(app, ana).get('/api/v1/merchant/reservations').expect(403);
    });
  });
});
