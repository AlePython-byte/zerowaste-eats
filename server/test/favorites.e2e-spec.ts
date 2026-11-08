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
const HERE = 'lat=1.213611&lng=-77.281111';

describe('Favorites (e2e)', () => {
  let app: INestApplication<App>;
  let db: ReturnType<typeof createFakeOffersDb>;
  let offerId: string;
  const ana = '7e57c0de-0000-4000-8000-0000000000a1';
  const luis = '7e57c0de-0000-4000-8000-0000000000b2';

  beforeEach(async () => {
    db = createFakeOffersDb();
    db.tables.users.push(
      { id: ana, role: 'CUSTOMER' },
      { id: luis, role: 'CUSTOMER' },
    );
    const business = addBusiness(db.tables);
    offerId = addOffer(db.tables, business.establishment.id, {
      pickupStart: inHours(1),
      pickupEnd: inHours(2),
    }).id;
    app = await createE2eApp(db.client);
  });

  afterEach(async () => {
    await app.close();
  });

  it('saves, lists, marks and removes a favorite', async () => {
    await as(app, ana).put(`/api/v1/favorites/${offerId}`).expect(204);
    await as(app, ana).put(`/api/v1/favorites/${offerId}`).expect(204);

    await as(app, ana)
      .get('/api/v1/favorites')
      .expect(200)
      .expect((r) => {
        expect(r.body).toHaveLength(1);
        expect(r.body[0]).toMatchObject({
          id: offerId,
          isFavorite: true,
          isAvailable: true,
        });
      });
    await as(app, ana)
      .get(`/api/v1/offers?${HERE}`)
      .expect((r) => expect(r.body[0].isFavorite).toBe(true));
    await as(app, ana)
      .get(`/api/v1/offers/${offerId}`)
      .expect((r) => expect(r.body.isFavorite).toBe(true));

    // Someone else sees it unmarked
    await as(app, luis)
      .get(`/api/v1/offers?${HERE}`)
      .expect((r) => expect(r.body[0].isFavorite).toBe(false));
    await as(app, luis).get('/api/v1/favorites').expect(200).expect([]);

    await as(app, ana).delete(`/api/v1/favorites/${offerId}`).expect(204);
    await as(app, ana).delete(`/api/v1/favorites/${offerId}`).expect(204);
    await as(app, ana).get('/api/v1/favorites').expect([]);
  });

  it("cannot remove another person's favorite", async () => {
    await as(app, ana).put(`/api/v1/favorites/${offerId}`).expect(204);

    await as(app, luis).delete(`/api/v1/favorites/${offerId}`).expect(204);

    expect(db.tables.favorites).toEqual([
      expect.objectContaining({ userId: ana }),
    ]);
  });

  it('answers an unknown or malformed offer with 404', async () => {
    await as(app, ana)
      .put('/api/v1/favorites/00000000-0000-4000-8000-000000000000')
      .expect(404);
    await as(app, ana).put('/api/v1/favorites/no-es-un-id').expect(404);
  });

  it('requires a session', () =>
    request(app.getHttpServer()).get('/api/v1/favorites').expect(401));
});
