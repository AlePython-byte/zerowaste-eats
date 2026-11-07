import {
  BadRequestException,
  INestApplication,
  ValidationPipe,
} from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { MAX_PHOTO_BYTES } from '../src/ai/photo-upload.interceptor';
import { AppModule } from './../src/app.module';
import { AuthService } from './../src/auth/auth.service';
import type { AuthenticatedUser } from './../src/auth/interfaces/authenticated-user.interface';
import { DatabaseService } from './../src/database/database.service';
import { authenticatedUser } from './fixtures/user-profile';
import { jpegBytes, pngBytes, webpBytes } from './fixtures/images';

describe('Offer suggestions (e2e)', () => {
  let app: INestApplication<App>;
  const findUnique = jest.fn();
  const verifyAccessToken = jest.fn<Promise<AuthenticatedUser>, [string]>();
  const path = '/api/v1/ai/offer-suggestions';

  beforeEach(async () => {
    findUnique.mockReset().mockResolvedValue({ role: 'MERCHANT' });
    verifyAccessToken.mockReset().mockResolvedValue(authenticatedUser);
    const moduleFixture = await Test.createTestingModule({
      imports: [AppModule],
    })
      .overrideProvider(DatabaseService)
      .useValue({
        checkConnection: jest.fn(),
        client: { user: { findUnique } },
      })
      .overrideProvider(AuthService)
      .useValue({ verifyAccessToken })
      .compile();

    app = moduleFixture.createNestApplication();
    app.setGlobalPrefix('api/v1');
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
        exceptionFactory: () =>
          new BadRequestException(
            'Los datos enviados no son válidos.',
            'Solicitud inválida',
          ),
      }),
    );
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  const post = () =>
    request(app.getHttpServer())
      .post(path)
      .set('Authorization', 'Bearer opaque-test-token');

  it('suggests an offer from a JPEG in simulated mode without any API key', async () => {
    const response = await post()
      .attach('foto', jpegBytes(), {
        filename: 'pan.jpg',
        contentType: 'image/jpeg',
      })
      .field('precioOriginal', '12000')
      .expect(200);

    expect(response.body).toEqual({
      title: expect.any(String),
      category: expect.stringMatching(
        /^(BAKERY|PREPARED_FOOD|DESSERTS|PRODUCE|DAIRY|OTHER)$/,
      ),
      description: expect.any(String),
      suggestedDiscountPercent: expect.any(Number),
      suggestedDiscountedPrice: expect.any(Number),
      confidence: 0.5,
      warnings: [expect.stringMatching(/^Sugerencia de ejemplo/)],
      source: 'mock',
    });
    expect(response.body.suggestedDiscountedPrice % 100).toBe(0);
    expect(findUnique).toHaveBeenCalledWith({
      where: { id: authenticatedUser.id },
      select: { role: true },
    });
  });

  it('accepts WebP, and answers without a price when none is given', async () => {
    const response = await post()
      .attach('foto', webpBytes(), {
        filename: 'fruta.webp',
        contentType: 'image/webp',
      })
      .expect(200);

    expect(response.body.suggestedDiscountedPrice).toBeNull();
    expect(response.body.warnings).toContain(
      'Agrega el precio original para calcular el precio con descuento.',
    );
  });

  it('gives the same answer for the same photo', async () => {
    const photo = jpegBytes(400, 3);
    const first = await post().attach('foto', photo, 'a.jpg').expect(200);
    const second = await post().attach('foto', photo, 'b.jpg').expect(200);

    expect(second.body).toEqual(first.body);
  });

  it('requires authentication', async () => {
    await request(app.getHttpServer())
      .post(path)
      .attach('foto', jpegBytes(), 'pan.jpg')
      .expect(401);
    expect(findUnique).not.toHaveBeenCalled();
  });

  it('is only for businesses', () => {
    findUnique.mockResolvedValue({ role: 'CUSTOMER' });
    return post().attach('foto', jpegBytes(), 'pan.jpg').expect(403).expect({
      statusCode: 403,
      error: 'Prohibido',
      message: 'No tienes permiso para realizar esta acción.',
    });
  });

  it('asks for a photo when the body has none', () =>
    post().field('precioOriginal', '12000').expect(400).expect({
      statusCode: 400,
      error: 'Solicitud inválida',
      message: 'Debes enviar una foto del producto.',
    }));

  it('judges the type by the bytes: a PNG named .jpg is refused', () =>
    post()
      .attach('foto', pngBytes(), {
        filename: 'pan.jpg',
        contentType: 'image/jpeg',
      })
      .expect(415)
      .expect({
        statusCode: 415,
        error: 'Formato no admitido',
        message: 'La foto debe ser una imagen JPEG o WebP.',
      }));

  it('refuses photos over 2 MB with a Spanish message', () =>
    post()
      .attach('foto', jpegBytes(MAX_PHOTO_BYTES + 1), 'grande.jpg')
      .expect(413)
      .expect({
        statusCode: 413,
        error: 'Archivo demasiado grande',
        message: 'La foto no puede pesar más de 2 MB.',
      }));

  it('accepts a photo of exactly 2 MB', () =>
    post().attach('foto', jpegBytes(MAX_PHOTO_BYTES), 'justo.jpg').expect(200));

  it.each([
    [
      'a second file',
      (r: request.Test) =>
        r
          .attach('foto', jpegBytes(), 'a.jpg')
          .attach('foto', jpegBytes(), 'b.jpg'),
    ],
    [
      'a file under another name',
      (r: request.Test) => r.attach('imagen', jpegBytes(), 'a.jpg'),
    ],
    [
      'an unknown text field',
      (r: request.Test) =>
        r.attach('foto', jpegBytes(), 'a.jpg').field('userId', 'otro'),
    ],
    [
      'a price with letters',
      (r: request.Test) =>
        r.attach('foto', jpegBytes(), 'a.jpg').field('precioOriginal', '12mil'),
    ],
    [
      'a price below 500',
      (r: request.Test) =>
        r.attach('foto', jpegBytes(), 'a.jpg').field('precioOriginal', '300'),
    ],
    [
      'a fractional price',
      (r: request.Test) =>
        r
          .attach('foto', jpegBytes(), 'a.jpg')
          .field('precioOriginal', '1200.5'),
    ],
  ])('rejects %s with a safe 400', async (_label, build) => {
    const response = await build(post()).expect(400);

    expect(response.body).toEqual({
      statusCode: 400,
      error: 'Solicitud inválida',
      message: 'Los datos enviados no son válidos.',
    });
  });
});
