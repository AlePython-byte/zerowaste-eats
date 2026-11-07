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
import { Roles } from '../auth/decorators/roles.decorator';
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

@Controller('ai')
@UseGuards(SupabaseAuthGuard, RolesGuard)
export class AiController {
  constructor(private readonly suggestions: OfferSuggestionsService) {}

  /**
   * Multipart: `foto` (JPEG or WebP, up to 2 MB) and optionally `precioOriginal`.
   * Only businesses: the role is read from the database by RolesGuard.
   */
  @Post('offer-suggestions')
  @HttpCode(HttpStatus.OK)
  @Roles(UserRole.MERCHANT)
  @UseInterceptors(PhotoUploadInterceptor)
  suggest(
    @UploadedFile() photo: UploadedPhoto | undefined,
    @Body() dto: OfferSuggestionRequestDto,
  ): Promise<OfferSuggestion> {
    return this.suggestions.suggest(photo, dto.precioOriginal);
  }
}
