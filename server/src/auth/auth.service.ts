import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { isUUID } from 'class-validator';
import type { AuthenticatedUser } from './interfaces/authenticated-user.interface';

@Injectable()
export class AuthService {
  private readonly supabase: SupabaseClient;
  private readonly issuer: string;

  constructor(configService: ConfigService) {
    try {
      const url = configService.getOrThrow<string>('SUPABASE_URL');
      this.issuer = `${url.replace(/\/+$/, '')}/auth/v1`;
      this.supabase = createClient(
        url,
        configService.getOrThrow<string>('SUPABASE_PUBLISHABLE_KEY'),
        {
          auth: {
            persistSession: false,
            autoRefreshToken: false,
            detectSessionInUrl: false,
            debug: false,
          },
        },
      );
    } catch {
      throw new Error(
        'Authentication initialization failed. Check the Supabase configuration.',
      );
    }
  }

  async verifyAccessToken(token: string): Promise<AuthenticatedUser> {
    try {
      if (!token || token.trim() === '') {
        throw new Error();
      }
      const { data, error } = await this.supabase.auth.getClaims(token, {
        allowExpired: false,
      });
      const claims = data?.claims;
      if (
        error ||
        !claims ||
        typeof claims.sub !== 'string' ||
        !isUUID(claims.sub) ||
        claims.iss !== this.issuer
      ) {
        throw new Error();
      }

      return {
        id: claims.sub,
        email: typeof claims.email === 'string' ? claims.email : null,
      };
    } catch {
      throw new UnauthorizedException(
        'Se requiere un token de acceso válido.',
        'No autorizado',
      );
    }
  }
}
