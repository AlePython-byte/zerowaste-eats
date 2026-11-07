import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  InternalServerErrorException,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { DatabaseService } from '../../database/database.service';
import type { UserRole } from '../../generated/prisma/enums';
import { ROLES_KEY } from '../decorators/roles.decorator';
import type { AuthenticatedRequest } from '../interfaces/authenticated-request.interface';

/**
 * Allows a request only when the caller's profile has one of the roles named by `@Roles`.
 * The role is read from the database on every request, never from the access token:
 * token claims can be stale or user-editable metadata, the profile row is the source of truth.
 */
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(
    private readonly reflector: Reflector,
    private readonly databaseService: DatabaseService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const required = this.reflector.getAllAndOverride<UserRole[] | undefined>(
      ROLES_KEY,
      [context.getHandler(), context.getClass()],
    );
    if (required === undefined) {
      return true;
    }

    const identity = context
      .switchToHttp()
      .getRequest<AuthenticatedRequest>().user;
    if (!identity) {
      throw new UnauthorizedException(
        'Autenticación requerida.',
        'No autorizado',
      );
    }

    let profile: { role: UserRole } | null;
    try {
      profile = await this.databaseService.client.user.findUnique({
        where: { id: identity.id },
        select: { role: true },
      });
    } catch {
      throw new InternalServerErrorException(
        'No fue posible verificar tus permisos.',
        'Error interno',
      );
    }

    if (!profile) {
      throw new ForbiddenException(
        'Completa tu perfil para continuar.',
        'Prohibido',
      );
    }
    // An empty @Roles() allows nobody: a forgotten argument must not open the route
    if (!required.includes(profile.role)) {
      throw new ForbiddenException(
        'No tienes permiso para realizar esta acción.',
        'Prohibido',
      );
    }
    return true;
  }
}
