import { Test, TestingModule } from '@nestjs/testing';
import {
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

@Module({})
class TestObserveModule {}

describe('Application (e2e)', () => {
  let app: INestApplication<App>;
  const databaseService = { checkConnection: jest.fn<Promise<boolean>, []>() };
  const authService = {
    verifyAccessToken: jest.fn<Promise<AuthenticatedUser>, [string]>(),
  };

  beforeEach(async () => {
    databaseService.checkConnection.mockReset().mockResolvedValue(true);
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
});
