import { Test, TestingModule } from '@nestjs/testing';
import {
  BadRequestException,
  DynamicModule,
  INestApplication,
  ValidationPipe,
} from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule, ObserveModule } from './../src/app.module';
import { DatabaseService } from './../src/database/database.service';
import { AuthService } from './../src/auth/auth.service';
import type { AuthenticatedUser } from './../src/auth/interfaces/authenticated-user.interface';
import { Prisma } from './../src/generated/prisma/client';
import {
  authenticatedUser,
  publicUserProfile,
  userRecord,
} from './fixtures/user-profile';

describe('Application (e2e)', () => {
  let app: INestApplication<App>;
  const userRepository = {
    create: jest.fn(),
    findUnique: jest.fn(),
    update: jest.fn(),
  };
  const merchantProfileRepository = { create: jest.fn() };
  const establishmentRepository = { create: jest.fn() };
  const client = {
    user: userRepository,
    merchantProfile: merchantProfileRepository,
    establishment: establishmentRepository,
    // The transaction client exposes the same repositories
    $transaction: jest.fn(
      (callback: (tx: unknown) => Promise<unknown>): Promise<unknown> =>
        callback(client),
    ),
  };
  const databaseService = {
    checkConnection: jest.fn<Promise<boolean>, []>(),
    client,
  };
  const authService = {
    verifyAccessToken: jest.fn<Promise<AuthenticatedUser>, [string]>(),
  };

  beforeEach(async () => {
    databaseService.checkConnection.mockReset().mockResolvedValue(true);
    for (const method of Object.values(userRepository))
      method.mockReset().mockResolvedValue(userRecord);
    merchantProfileRepository.create
      .mockReset()
      .mockResolvedValue({ id: '5d0c7a3e-3f43-4c58-9a57-1f1f3f0b7c11' });
    establishmentRepository.create.mockReset().mockResolvedValue({});
    client.$transaction.mockClear();
    authService.verifyAccessToken.mockReset().mockResolvedValue({
      id: 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13',
      email: 'customer@example.test',
    });
    // Without credentials telemetry is not registered, so HTTP tests start no
    // telemetry workers and make no external requests.
    const imports = (await Promise.all(
      Reflect.getMetadata('imports', AppModule) as unknown[],
    )) as DynamicModule[];
    if (imports.some((entry) => entry?.module === ObserveModule)) {
      throw new Error('Telemetry must stay off without credentials');
    }

    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    })
      .overrideProvider(DatabaseService)
      .useValue(databaseService)
      .overrideProvider(AuthService)
      .useValue(authService)
      .compile();

    app = moduleFixture.createNestApplication();
    app.setGlobalPrefix('api/v1');
    app.enableCors();
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

  it('GET /api/v1/salud returns the API health response', () => {
    return request(app.getHttpServer())
      .get('/api/v1/salud')
      .expect(200)
      .expect('Content-Type', /json/)
      .expect({
        estado: 'ok',
        servicio: 'ZeroWaste Eats API',
        version: '1.0.0',
      });
  });

  it.each(['/', '/api/v1', '/salud'])(
    'GET %s does not expose a default or unprefixed endpoint',
    (path) => request(app.getHttpServer()).get(path).expect(404),
  );

  it('throttles a client past the per-minute budget with a safe Spanish 429', async () => {
    // The test environment uses the default budget of 60 requests per minute
    for (let i = 0; i < 60; i++) {
      await request(app.getHttpServer()).get('/api/v1/salud').expect(200);
    }
    const response = await request(app.getHttpServer())
      .get('/api/v1/salud')
      .expect(429);
    expect(response.body.message).toBe(
      'Demasiadas solicitudes. Intenta de nuevo en un momento.',
    );
  });

  it('GET /api/v1/salud/base-datos reports database connectivity', () => {
    return request(app.getHttpServer())
      .get('/api/v1/salud/base-datos')
      .expect(200)
      .expect({ estado: 'ok', baseDatos: 'conectada' });
  });

  it('GET /api/v1/salud/base-datos returns a safe 503 when unavailable', () => {
    databaseService.checkConnection.mockResolvedValue(false);
    return request(app.getHttpServer())
      .get('/api/v1/salud/base-datos')
      .expect(503)
      .expect({
        estado: 'error',
        baseDatos: 'desconectada',
        mensaje: 'La base de datos no está disponible.',
      });
  });

  it('allows development CORS preflight requests', () => {
    return request(app.getHttpServer())
      .options('/api/v1/salud')
      .set('Origin', 'http://localhost:5173')
      .set('Access-Control-Request-Method', 'GET')
      .expect(204)
      .expect('Access-Control-Allow-Origin', '*')
      .expect('Access-Control-Allow-Methods', /GET/);
  });

  afterEach(async () => {
    await app.close();
  });

  it('GET /api/v1/auth/me requires a Bearer token', async () => {
    await request(app.getHttpServer())
      .get('/api/v1/auth/me')
      .expect(401)
      .expect({
        statusCode: 401,
        error: 'No autorizado',
        message: 'Se requiere un token de acceso válido.',
      });
    expect(authService.verifyAccessToken).not.toHaveBeenCalled();
  });

  it('GET /api/v1/auth/me rejects an invalid token safely', () => {
    authService.verifyAccessToken.mockRejectedValue(
      new Error('private-auth-details'),
    );
    return request(app.getHttpServer())
      .get('/api/v1/auth/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(401)
      .expect({
        statusCode: 401,
        error: 'No autorizado',
        message: 'Se requiere un token de acceso válido.',
      });
  });

  it('GET /api/v1/auth/me returns the verified identity through CurrentUser', () => {
    return request(app.getHttpServer())
      .get('/api/v1/auth/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(200)
      .expect({
        id: 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13',
        email: 'customer@example.test',
      });
  });

  it.each(['get', 'post', 'patch'] as const)(
    '%s /api/v1/users/me requires authentication',
    async (method) => {
      await request(app.getHttpServer())
        [method]('/api/v1/users/me')
        .expect(401);
      for (const operation of Object.values(userRepository))
        expect(operation).not.toHaveBeenCalled();
    },
  );

  it('POST /api/v1/users/me creates a trimmed profile for the verified identity', async () => {
    userRepository.findUnique.mockResolvedValue(null);
    await request(app.getHttpServer())
      .post('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ displayName: '  Alejandro  ', city: '  Pasto  ' })
      .expect(201)
      .expect(publicUserProfile);
    expect(userRepository.create).toHaveBeenCalledWith({
      data: {
        id: authenticatedUser.id,
        email: authenticatedUser.email,
        displayName: 'Alejandro',
        city: 'Pasto',
        role: 'CUSTOMER',
      },
    });
    expect(merchantProfileRepository.create).not.toHaveBeenCalled();
  });

  it('POST /api/v1/users/me creates a merchant with its profile and first establishment in one transaction', async () => {
    userRepository.findUnique.mockResolvedValue(null);
    userRepository.create.mockResolvedValue({
      ...userRecord,
      role: 'MERCHANT',
    });
    await request(app.getHttpServer())
      .post('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({
        displayName: 'Alejandro',
        role: 'MERCHANT',
        businessName: ' Panadería La Esquina ',
      })
      .expect(201)
      .expect({ ...publicUserProfile, role: 'MERCHANT' });
    expect(client.$transaction).toHaveBeenCalledTimes(1);
    expect(merchantProfileRepository.create).toHaveBeenCalledWith({
      data: {
        userId: authenticatedUser.id,
        businessName: 'Panadería La Esquina',
      },
    });
    expect(establishmentRepository.create).toHaveBeenCalledWith({
      data: {
        merchantProfileId: '5d0c7a3e-3f43-4c58-9a57-1f1f3f0b7c11',
        name: 'Panadería La Esquina',
        address: 'Centro de Pasto',
        city: 'Pasto',
        latitude: '1.213611',
        longitude: '-77.281111',
      },
    });
  });

  it('POST /api/v1/users/me is idempotent: a repeat answers 200 with the existing profile', async () => {
    await request(app.getHttpServer())
      .post('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ displayName: 'Alejandro' })
      .expect(200)
      .expect(publicUserProfile);
    expect(userRepository.create).not.toHaveBeenCalled();
  });

  it('POST /api/v1/users/me refuses to change the role of an existing profile', async () => {
    await request(app.getHttpServer())
      .post('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ displayName: 'Alejandro', role: 'MERCHANT' })
      .expect(409)
      .expect({
        statusCode: 409,
        error: 'Conflicto',
        message:
          'Tu perfil ya existe con otro rol y el rol no se puede cambiar.',
      });
    expect(userRepository.create).not.toHaveBeenCalled();
    expect(merchantProfileRepository.create).not.toHaveBeenCalled();
  });

  it('POST /api/v1/users/me returns a safe 500 when the merchant transaction fails', async () => {
    userRepository.findUnique.mockResolvedValue(null);
    userRepository.create.mockResolvedValue({
      ...userRecord,
      role: 'MERCHANT',
    });
    establishmentRepository.create.mockRejectedValue(
      new Error('private-database-details'),
    );
    const response = await request(app.getHttpServer())
      .post('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ displayName: 'Alejandro', role: 'MERCHANT' })
      .expect(500);
    expect(response.body).toEqual({
      statusCode: 500,
      error: 'Error interno',
      message: 'No fue posible crear el perfil.',
    });
  });

  it('GET /api/v1/users/me uses the verified UUID even with an unrelated query parameter', async () => {
    await request(app.getHttpServer())
      .get('/api/v1/users/me?userId=another-user')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(200)
      .expect(publicUserProfile);
    expect(userRepository.findUnique).toHaveBeenCalledWith({
      where: { id: authenticatedUser.id },
    });
    expect(userRepository.create).not.toHaveBeenCalled();
  });

  it('PATCH /api/v1/users/me updates allowed fields and returns the public representation', async () => {
    userRepository.update.mockResolvedValue({ ...userRecord, city: 'Bogotá' });
    await request(app.getHttpServer())
      .patch('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ city: ' Bogotá ' })
      .expect(200)
      .expect({ ...publicUserProfile, city: 'Bogotá' });
    expect(userRepository.update).toHaveBeenCalledWith({
      where: { id: authenticatedUser.id },
      data: { city: 'Bogotá' },
    });
  });

  it.each([
    ['post', { displayName: '  ' }],
    ['post', { displayName: 'Alejandro', email: 'other@example.test' }],
    ['post', { displayName: 'Alejandro', id: 'another-user' }],
    ['post', { displayName: 'Alejandro', businessName: 'Tienda' }],
    ['patch', {}],
    ['patch', { role: 'MERCHANT' }],
    ['patch', { city: 'Pasto', role: 'MERCHANT' }],
    ['patch', { city: 'Pasto', email: 'other@example.test' }],
    ['patch', { city: 'Pasto', id: 'another-user' }],
  ] as const)(
    '%s /api/v1/users/me rejects invalid or forbidden input (%j)',
    async (method, body) => {
      await request(app.getHttpServer())
        [method]('/api/v1/users/me')
        .set('Authorization', 'Bearer opaque-test-token')
        .send(body)
        .expect(400)
        .expect({
          statusCode: 400,
          error: 'Solicitud inválida',
          message: 'Los datos enviados no son válidos.',
        });
      expect(userRepository.create).not.toHaveBeenCalled();
      expect(userRepository.update).not.toHaveBeenCalled();
    },
  );

  it('GET /api/v1/users/me returns a safe 404 for a missing profile', () => {
    userRepository.findUnique.mockResolvedValue(null);
    return request(app.getHttpServer())
      .get('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(404)
      .expect({
        statusCode: 404,
        error: 'No encontrado',
        message: 'No se encontró tu perfil.',
      });
  });

  it('POST /api/v1/users/me rejects a verified identity without usable email', async () => {
    authService.verifyAccessToken.mockResolvedValue({
      ...authenticatedUser,
      email: null,
    });
    await request(app.getHttpServer())
      .post('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ displayName: 'Alejandro' })
      .expect(422)
      .expect({
        statusCode: 422,
        error: 'Datos no procesables',
        message:
          'La identidad autenticada no contiene un correo electrónico válido.',
      });
    expect(userRepository.create).not.toHaveBeenCalled();
  });

  it('POST /api/v1/users/me returns 409 for a unique conflict without exposing Prisma details', () => {
    // The email belongs to another account: no profile exists for this identity
    userRepository.findUnique.mockResolvedValue(null);
    userRepository.create.mockRejectedValue(
      new Prisma.PrismaClientKnownRequestError('private-database-details', {
        code: 'P2002',
        clientVersion: '7.10.0',
      }),
    );
    return request(app.getHttpServer())
      .post('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ displayName: 'Alejandro' })
      .expect(409)
      .expect({
        statusCode: 409,
        error: 'Conflicto',
        message: 'Ya existe un perfil con estos datos.',
      });
  });

  it('PATCH /api/v1/users/me returns 404 for a missing profile', () => {
    userRepository.update.mockRejectedValue(
      new Prisma.PrismaClientKnownRequestError('private-database-details', {
        code: 'P2025',
        clientVersion: '7.10.0',
      }),
    );
    return request(app.getHttpServer())
      .patch('/api/v1/users/me')
      .set('Authorization', 'Bearer opaque-test-token')
      .send({ city: 'Pasto' })
      .expect(404)
      .expect({
        statusCode: 404,
        error: 'No encontrado',
        message: 'No se encontró tu perfil.',
      });
  });
});
