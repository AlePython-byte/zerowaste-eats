import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { CustomerStatsController } from './customer-stats.controller';
import { CustomerStatsService } from './customer-stats.service';
import { MerchantReservationsController } from './merchant-reservations.controller';
import { MerchantReservationsService } from './merchant-reservations.service';
import { ReservationsController } from './reservations.controller';
import { ReservationsService } from './reservations.service';

/** Reserving units of an offer and picking them up with a code. */
@Module({
  imports: [AuthModule],
  controllers: [
    ReservationsController,
    MerchantReservationsController,
    CustomerStatsController,
  ],
  providers: [
    ReservationsService,
    MerchantReservationsService,
    CustomerStatsService,
  ],
})
export class ReservationsModule {}
