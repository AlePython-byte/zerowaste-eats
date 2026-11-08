import {
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  Put,
  UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { uuidOrNotFound } from '../common/uuid-param';
import { FavoritesService } from './favorites.service';

const offerIdPipe = uuidOrNotFound('Esta oferta ya no está disponible.');

/** The signed-in person's saved offers. Add and remove are idempotent. */
@Controller('favorites')
@UseGuards(SupabaseAuthGuard)
export class FavoritesController {
  constructor(private readonly favorites: FavoritesService) {}

  @Get()
  list(@CurrentUser() user: AuthenticatedUser) {
    return this.favorites.list(user.id);
  }

  @Put(':offerId')
  @HttpCode(HttpStatus.NO_CONTENT)
  add(
    @CurrentUser() user: AuthenticatedUser,
    @Param('offerId', offerIdPipe) offerId: string,
  ) {
    return this.favorites.add(user.id, offerId);
  }

  @Delete(':offerId')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(
    @CurrentUser() user: AuthenticatedUser,
    @Param('offerId', offerIdPipe) offerId: string,
  ) {
    return this.favorites.remove(user.id, offerId);
  }
}
