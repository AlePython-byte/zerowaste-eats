import {
  Body,
  Controller,
  HttpCode,
  HttpStatus,
  Post,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { Roles } from '../auth/decorators/roles.decorator';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { RolesGuard } from '../auth/guards/roles.guard';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import { UserRole } from '../generated/prisma/enums';
import { OfferSuggestionRequestDto } from './dto/offer-suggestion-request.dto';
import type { OfferSuggestion } from './offer-suggestion';
import {
  OfferSuggestionsService,
  type UploadedPhoto,
} from './offer-suggestions.service';
import { PhotoUploadInterceptor } from './photo-upload.interceptor';

export const SUGGESTIONS_PER_MINUTE = 5;
const ONE_MINUTE_MS = 60_000;

@Controller('ai')
@UseGuards(SupabaseAuthGuard, RolesGuard)
export class AiController {
  constructor(private readonly suggestions: OfferSuggestionsService) {}

  /**
   * Multipart: `foto` (JPEG or WebP, up to 2 MB) and optionally `precioOriginal`.
   * Only businesses: the role is read from the database by RolesGuard. At most
   * SUGGESTIONS_PER_MINUTE calls per client, on top of the daily quota per business.
   */
  @Post('offer-suggestions')
  @HttpCode(HttpStatus.OK)
  @Throttle({
    default: { limit: SUGGESTIONS_PER_MINUTE, ttl: ONE_MINUTE_MS },
  })
  @Roles(UserRole.MERCHANT)
  @UseInterceptors(PhotoUploadInterceptor)
  suggest(
    @CurrentUser() user: AuthenticatedUser,
    @UploadedFile() photo: UploadedPhoto | undefined,
    @Body() dto: OfferSuggestionRequestDto,
  ): Promise<OfferSuggestion> {
    return this.suggestions.suggest(user.id, photo, dto.precioOriginal);
  }
}
