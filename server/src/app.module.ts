import { DynamicModule, Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { APP_GUARD } from '@nestjs/core';
import { createObserveModule } from '@nestjs/observe';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import { AuthModule } from './auth/auth.module';
import { AuthService } from './auth/auth.service';
import { SupabaseAuthGuard } from './auth/guards/supabase-auth.guard';
import { validateEnvironment } from './config/environment.validation';
import { readObserveCredentials } from './config/telemetry';
import { DatabaseModule } from './database/database.module';
import { DatabaseService } from './database/database.service';
import { HealthModule } from './health/health.module';
import { MerchantsModule } from './merchants/merchants.module';
import { OffersModule } from './offers/offers.module';
import { ReservationsModule } from './reservations/reservations.module';
import { UsersModule } from './users/users.module';

export const { ObserveModule, ObserveInstrument } = createObserveModule({
  // Credentials, access tokens, and raw database/auth errors must never enter telemetry.
  skipInstrumentation: (instance) =>
    instance instanceof ConfigService ||
    instance instanceof DatabaseService ||
    instance instanceof AuthService ||
    instance instanceof SupabaseAuthGuard,
});

/** Stands in for telemetry when it has no credentials: nothing runs and nothing is sent. */
@Module({})
export class TelemetryOffModule {}

/**
 * Distributed tracing, correlated logs and metrics (https://observe.nestjs.com), only when
 * its credentials are configured. Waits for the environment so a key in `.env` counts.
 */
async function telemetryModule(): Promise<DynamicModule> {
  await ConfigModule.envVariablesLoaded;
  const credentials = readObserveCredentials(process.env);
  return credentials
    ? ObserveModule.forRoot({ ...credentials, serviceId: 'server' })
    : { module: TelemetryOffModule };
}

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      ignoreEnvFile: process.env.NODE_ENV === 'test',
      validate: validateEnvironment,
    }),
    telemetryModule(),
    // Every route shares one per-client budget per minute; a busy client gets a 429
    ThrottlerModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        throttlers: [
          {
            ttl: 60_000,
            limit: config.getOrThrow<number>('RATE_LIMIT_PER_MINUTE'),
          },
        ],
        errorMessage: 'Demasiadas solicitudes. Intenta de nuevo en un momento.',
      }),
    }),
    DatabaseModule,
    HealthModule,
    AuthModule,
    UsersModule,
    MerchantsModule,
    OffersModule,
    ReservationsModule,
  ],
  providers: [{ provide: APP_GUARD, useClass: ThrottlerGuard }],
})
export class AppModule {}
