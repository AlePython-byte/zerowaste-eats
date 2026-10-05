import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { AuthService } from '../auth.service';
import type { AuthenticatedRequest } from '../interfaces/authenticated-request.interface';

@Injectable()
export class SupabaseAuthGuard implements CanActivate {
  constructor(private readonly authService: AuthService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    delete request.user;
    const authorization = request.headers.authorization;
    const match =
      typeof authorization === 'string'
        ? /^Bearer ([^\s,]+)$/i.exec(authorization)
        : null;

    try {
      if (!match) {
        throw new Error();
      }
      request.user = await this.authService.verifyAccessToken(match[1]);
      return true;
    } catch {
      throw new UnauthorizedException(
        'Se requiere un token de acceso válido.',
        'No autorizado',
      );
    }
  }
}
