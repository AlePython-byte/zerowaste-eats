import { Logger, Module } from '@nestjs/common';
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
  const logger = new Logger('OfferSuggester');
  const apiKey = config.get<string>('ANTHROPIC_API_KEY');
  if (!apiKey) {
    logger.log('Offer suggestions: mock (no ANTHROPIC_API_KEY)');
    return new MockOfferSuggester();
  }
  const model = config.getOrThrow<string>('AI_MODEL');
  // Only the mode and the model: the key never reaches the log
  logger.log(`Offer suggestions: claude (model ${model})`);
  return new ClaudeOfferSuggester(createAnthropicClient(apiKey), model);
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
