import { Controller, Get, Param, Query, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { FavoritesService } from '../favorites/favorites.service';
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
  constructor(
    private readonly offers: OffersService,
    private readonly favorites: FavoritesService,
  ) {}

  @Get()
  nearby(
    @CurrentUser() user: AuthenticatedUser,
    @Query() query: NearbyOffersQueryDto,
  ) {
    return this.offers.nearby(query, (offerIds) =>
      this.favorites.savedAmong(user.id, offerIds),
    );
  }

  @Get(':id')
  detail(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id', uuidOrNotFound('Esta oferta ya no está disponible.'))
    id: string,
    @Query() query: OfferDetailQueryDto,
  ) {
    const from =
      query.lat !== undefined && query.lng !== undefined
        ? { latitude: query.lat, longitude: query.lng }
        : null;
    return this.offers.detail(id, from, async (offerId) =>
      (await this.favorites.savedAmong(user.id, [offerId])).has(offerId),
    );
  }
}
