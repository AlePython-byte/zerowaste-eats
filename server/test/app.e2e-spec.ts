import { Test, TestingModule } from '@nestjs/testing';
import {
  BadRequestException,
  DynamicModule,
  INestApplication,
  Module,
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

@Module({})
class TestObserveModule {}

describe('Application (e2e)', () => {
  let app: INestApplication<App>;
  const userRepository = {
    create: jest.fn(),
    findUnique: jest.fn(),
    update: jest.fn(),
  };
  const databaseService = {
    checkConnection: jest.fn<Promise<boolean>, []>(),
    client: { user: userRepository },
  };
  const authService = {
    verifyAccessToken: jest.fn<Promise<AuthenticatedUser>, [string]>(),
  };

  beforeEach(async () => {
    databaseService.checkConnection.mockReset().mockResolvedValue(true);
    for (const method of Object.values(userRepository))
      method.mockReset().mockResolvedValue(userRecord);
    authService.verifyAccessToken.mockReset().mockResolvedValue({
      id: 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13',
      email: 'customer@example.test',
    });
    // Keep telemetry workers and external requests out of HTTP tests.
    const imports = Reflect.getMetadata(
      'imports',
      AppModule,
    ) as DynamicModule[];
    const observeModule = imports.find(
      (entry) => entry.module === ObserveModule,
    );
    if (!observeModule) {
      throw new Error('ObserveModule is not registered in AppModule');
    }

    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    })
      .overrideProvider(DatabaseService)
      .useValue(databaseService)
      .overrideProvider(AuthService)
      .useValue(authService)
      .overrideModule(observeModule)
      .useModule(TestObserveModule)
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
    ['patch', {}],
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
