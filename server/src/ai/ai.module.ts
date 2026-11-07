import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AuthModule } from '../auth/auth.module';
import { AiController } from './ai.controller';
import {
  ClaudeOfferSuggester,
  createAnthropicClient,
} from './claude-offer-suggester';
import { MockOfferSuggester } from './mock-offer-suggester';
import { OfferSuggester } from './offer-suggestion';
import { OfferSuggestionsService } from './offer-suggestions.service';
import { PhotoUploadInterceptor } from './photo-upload.interceptor';
import { SuggestionQuotaService } from './suggestion-quota.service';

/**
 * Claude when ANTHROPIC_API_KEY is set (validated and trimmed at startup), the simulated
 * suggester otherwise. The model comes from AI_MODEL.
 */
export function createOfferSuggester(config: ConfigService): OfferSuggester {
  const apiKey = config.get<string>('ANTHROPIC_API_KEY');
  if (!apiKey) {
    return new MockOfferSuggester();
  }
  return new ClaudeOfferSuggester(
    createAnthropicClient(apiKey),
    config.getOrThrow<string>('AI_MODEL'),
  );
}

@Module({
  imports: [AuthModule],
  controllers: [AiController],
  providers: [
    OfferSuggestionsService,
    SuggestionQuotaService,
    PhotoUploadInterceptor,
    {
      provide: OfferSuggester,
      inject: [ConfigService],
      useFactory: createOfferSuggester,
    },
  ],
})
export class AiModule {}
