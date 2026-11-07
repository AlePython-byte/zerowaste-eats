import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { MerchantOffersController } from './offers/merchant-offers.controller';
import { MerchantOffersService } from './offers/merchant-offers.service';

/** What a business manages about itself: its offers and its profile. */
@Module({
  imports: [AuthModule],
  controllers: [MerchantOffersController],
  providers: [MerchantOffersService],
})
export class MerchantsModule {}
