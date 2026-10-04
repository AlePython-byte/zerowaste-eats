import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { createObserveModule } from '@nestjs/observe';
import { AuthModule } from './auth/auth.module';
import { validateEnvironment } from './config/environment.validation';
import { DatabaseModule } from './database/database.module';
import { DatabaseService } from './database/database.service';
import { HealthModule } from './health/health.module';
import { MerchantsModule } from './merchants/merchants.module';
import { OffersModule } from './offers/offers.module';
import { ReservationsModule } from './reservations/reservations.module';
import { UsersModule } from './users/users.module';

export const { ObserveModule, ObserveInstrument } = createObserveModule({
  // Configuration values and database errors must never enter telemetry.
  skipInstrumentation: (instance) =>
    instance instanceof ConfigService || instance instanceof DatabaseService,
});

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      ignoreEnvFile: process.env.NODE_ENV === 'test',
      validate: validateEnvironment,
    }),
    // Distributed tracing, auto-correlated logs, request/job metrics, error
    // telemetry, alarms, and more — out of the box. Sign up at https://observe.nestjs.com
    ObserveModule.forRoot({
      appKey: 'YOUR_APP_KEY',
      appSecret: 'YOUR_APP_SECRET',
      serviceId: 'server',
    }),
    DatabaseModule,
    HealthModule,
    AuthModule,
    UsersModule,
    MerchantsModule,
    OffersModule,
    ReservationsModule,
  ],
})
export class AppModule {}
