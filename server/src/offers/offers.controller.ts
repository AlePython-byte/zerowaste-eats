import { Controller, Get, Param, Query, UseGuards } from '@nestjs/common';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import { uuidOrNotFound } from '../common/uuid-param';
import {
  NearbyOffersQueryDto,
  OfferDetailQueryDto,
} from './dto/nearby-offers-query.dto';
import { OffersService } from './offers.service';

/** What customers browse. Any signed-in person may look; no role is required. */
@Controller('offers')
@UseGuards(SupabaseAuthGuard)
export class OffersController {
  constructor(private readonly offers: OffersService) {}

  @Get()
  nearby(@Query() query: NearbyOffersQueryDto) {
    return this.offers.nearby(query);
  }

  @Get(':id')
  detail(
    @Param('id', uuidOrNotFound('Esta oferta ya no está disponible.'))
    id: string,
    @Query() query: OfferDetailQueryDto,
  ) {
    const from =
      query.lat !== undefined && query.lng !== undefined
        ? { latitude: query.lat, longitude: query.lng }
        : null;
    return this.offers.detail(id, from);
  }
}
