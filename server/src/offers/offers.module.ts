import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { OffersController } from './offers.controller';
import { OffersService } from './offers.service';

/** Offers as customers see them: nearby, searchable, and one at a time. */
@Module({
  imports: [AuthModule],
  controllers: [OffersController],
  providers: [OffersService],
  exports: [OffersService],
})
export class OffersModule {}
