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

@Module({})
class TestObserveModule {}

describe('Application (e2e)', () => {
  let app: INestApplication<App>;

  beforeEach(async () => {
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
});
