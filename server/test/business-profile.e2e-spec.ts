import { INestApplication } from '@nestjs/common';
import { App } from 'supertest/types';
import { addBusiness, createFakeOffersDb } from './fixtures/fake-offers-db';
import { as, createE2eApp } from './support/e2e-app';

describe('Business profile (e2e)', () => {
  let app: INestApplication<App>;
  let db: ReturnType<typeof createFakeOffersDb>;
  let mine: ReturnType<typeof addBusiness>;
  const customerId = '7e57c0de-0000-4000-8000-00000000c057';

  beforeEach(async () => {
    db = createFakeOffersDb();
    mine = addBusiness(db.tables, { businessName: 'Mi panadería' });
    db.tables.users.push({ id: customerId, role: 'CUSTOMER' });
    app = await createE2eApp(db.client);
  });

  afterEach(async () => {
    await app.close();
  });

  it('reads and updates the business with its location', async () => {
    await as(app, mine.userId)
      .get('/api/v1/merchant/profile')
      .expect(200)
      .expect((r) => expect(r.body.establishment.city).toBe('Pasto'));

    await as(app, mine.userId)
      .patch('/api/v1/merchant/profile')
      .send({
        businessName: 'Panadería La Esquina',
        establishment: {
          address: 'Calle 18 # 25-30',
          latitude: 1.2141,
          longitude: -77.2783,
        },
      })
      .expect(200)
      .expect((r) =>
        expect(r.body).toMatchObject({
          businessName: 'Panadería La Esquina',
          establishment: {
            address: 'Calle 18 # 25-30',
            latitude: 1.2141,
            longitude: -77.2783,
          },
        }),
      );
  });

  it('is only for businesses', () =>
    as(app, customerId).get('/api/v1/merchant/profile').expect(403));

  it.each([
    ['a latitude out of range', { establishment: { latitude: 91 } }],
    ['a longitude that is text', { establishment: { longitude: '-77' } }],
    ['a short name', { businessName: 'P' }],
    [
      'an unknown nested field',
      { establishment: { merchantProfileId: 'otro' } },
    ],
    ['an unknown field', { isVerified: true }],
    ['a long description', { description: 'x'.repeat(501) }],
  ])('refuses %s with a safe 400', (_label, body) =>
    as(app, mine.userId)
      .patch('/api/v1/merchant/profile')
      .send(body)
      .expect(400)
      .expect({
        statusCode: 400,
        error: 'Solicitud inválida',
        message: 'Los datos enviados no son válidos.',
      }),
  );

  it('cannot verify itself', async () => {
    await as(app, mine.userId)
      .patch('/api/v1/merchant/profile')
      .send({ isVerified: true });

    expect(db.tables.merchantProfiles[0].isVerified).toBe(false);
  });
});
