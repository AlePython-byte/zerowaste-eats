import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { AiController } from './ai.controller';
import { MockOfferSuggester } from './mock-offer-suggester';
import { OfferSuggester } from './offer-suggestion';
import { OfferSuggestionsService } from './offer-suggestions.service';
import { PhotoUploadInterceptor } from './photo-upload.interceptor';

@Module({
  imports: [AuthModule],
  controllers: [AiController],
  providers: [
    OfferSuggestionsService,
    PhotoUploadInterceptor,
    { provide: OfferSuggester, useClass: MockOfferSuggester },
  ],
})
export class AiModule {}
