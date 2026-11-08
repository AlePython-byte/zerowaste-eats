import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { MerchantOffersController } from './offers/merchant-offers.controller';
import { MerchantOffersService } from './offers/merchant-offers.service';
import { BusinessProfileController } from './profile/business-profile.controller';
import { BusinessProfileService } from './profile/business-profile.service';

/** What a business manages about itself: its offers and its profile. */
@Module({
  imports: [AuthModule],
  controllers: [MerchantOffersController, BusinessProfileController],
  providers: [MerchantOffersService, BusinessProfileService],
})
export class MerchantsModule {}
