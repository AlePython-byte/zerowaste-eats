import {
  BadRequestException,
  INestApplication,
  ValidationPipe,
} from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppModule } from '../../src/app.module';
import { AuthService } from '../../src/auth/auth.service';
import type { AuthenticatedUser } from '../../src/auth/interfaces/authenticated-user.interface';
import { DatabaseService } from '../../src/database/database.service';

/**
 * The whole app over a fake database, with the same global prefix and validation as
 * main.ts. The bearer token is the user id itself, so a test signs in as anyone by id;
 * the token "invalid" is refused.
 */
export async function createE2eApp(
  client: object,
): Promise<INestApplication<App>> {
  const verifyAccessToken = jest.fn(
    async (token: string): Promise<AuthenticatedUser> => {
      if (token === 'invalid') throw new Error('invalid token');
      return { id: token, email: `${token}@example.test` };
    },
  );
  const moduleFixture = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(DatabaseService)
    .useValue({ checkConnection: jest.fn(), client })
    .overrideProvider(AuthService)
    .useValue({ verifyAccessToken })
    .compile();

  const app = moduleFixture.createNestApplication<INestApplication<App>>();
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
  return app;
}

/** A request signed in as [userId]. */
export function as(app: INestApplication<App>, userId: string) {
  const server = app.getHttpServer();
  const auth = (test: request.Test) =>
    test.set('Authorization', `Bearer ${userId}`);
  return {
    get: (path: string) => auth(request(server).get(path)),
    post: (path: string) => auth(request(server).post(path)),
    patch: (path: string) => auth(request(server).patch(path)),
    put: (path: string) => auth(request(server).put(path)),
    delete: (path: string) => auth(request(server).delete(path)),
  };
}
