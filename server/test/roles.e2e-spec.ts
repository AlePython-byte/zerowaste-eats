import { Controller, Get, INestApplication, UseGuards } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { AuthService } from './../src/auth/auth.service';
import { Roles } from './../src/auth/decorators/roles.decorator';
import { RolesGuard } from './../src/auth/guards/roles.guard';
import { SupabaseAuthGuard } from './../src/auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from './../src/auth/interfaces/authenticated-user.interface';
import { DatabaseService } from './../src/database/database.service';
import { UserRole } from './../src/generated/prisma/enums';
import { authenticatedUser } from './fixtures/user-profile';

/** Stands in for the merchant routes that will use the guard. */
@Controller('probe')
@UseGuards(SupabaseAuthGuard, RolesGuard)
class ProbeController {
  @Get('merchant')
  @Roles(UserRole.MERCHANT)
  merchantOnly() {
    return { ok: true };
  }

  @Get('any')
  anyAuthenticated() {
    return { ok: true };
  }
}

describe('Roles (e2e)', () => {
  let app: INestApplication<App>;
  const findUnique = jest.fn();
  const verifyAccessToken = jest.fn<Promise<AuthenticatedUser>, [string]>();

  beforeEach(async () => {
    findUnique.mockReset().mockResolvedValue({ role: UserRole.MERCHANT });
    verifyAccessToken.mockReset().mockResolvedValue(authenticatedUser);
    const moduleFixture = await Test.createTestingModule({
      controllers: [ProbeController],
      providers: [
        SupabaseAuthGuard,
        RolesGuard,
        { provide: AuthService, useValue: { verifyAccessToken } },
        {
          provide: DatabaseService,
          useValue: { client: { user: { findUnique } } },
        },
      ],
    }).compile();
    app = moduleFixture.createNestApplication();
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  it('rejects an anonymous request with 401 before reading any role', async () => {
    await request(app.getHttpServer()).get('/probe/merchant').expect(401);
    expect(findUnique).not.toHaveBeenCalled();
  });

  it('lets a merchant through a merchant-only route', async () => {
    await request(app.getHttpServer())
      .get('/probe/merchant')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(200)
      .expect({ ok: true });
    expect(findUnique).toHaveBeenCalledWith({
      where: { id: authenticatedUser.id },
      select: { role: true },
    });
  });

  it('answers a customer with a safe 403', () => {
    findUnique.mockResolvedValue({ role: UserRole.CUSTOMER });
    return request(app.getHttpServer())
      .get('/probe/merchant')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(403)
      .expect({
        statusCode: 403,
        error: 'Prohibido',
        message: 'No tienes permiso para realizar esta acción.',
      });
  });

  it('answers a caller without a profile with a 403 asking to complete it', () => {
    findUnique.mockResolvedValue(null);
    return request(app.getHttpServer())
      .get('/probe/merchant')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(403)
      .expect({
        statusCode: 403,
        error: 'Prohibido',
        message: 'Completa tu perfil para continuar.',
      });
  });

  it('does not query roles on authenticated routes without @Roles', async () => {
    await request(app.getHttpServer())
      .get('/probe/any')
      .set('Authorization', 'Bearer opaque-test-token')
      .expect(200);
    expect(findUnique).not.toHaveBeenCalled();
  });
});
