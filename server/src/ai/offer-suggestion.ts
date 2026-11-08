import type { OfferCategory } from '../generated/prisma/enums';
import type { SupportedImageType } from './image-type';

/** The bounds every suggestion is held to, whatever produced it. */
export const SUGGESTION_RULES = {
  titleMaxLength: 60,
  descriptionMaxLength: 240,
  minDiscountPercent: 30,
  maxDiscountPercent: 70,
  /** Prices are rounded to this many pesos. */
  priceStep: 100,
} as const;

export interface PhotoInput {
  bytes: Buffer;
  mediaType: SupportedImageType;
  /** In Colombian pesos, when the merchant gave it. */
  originalPrice?: number;
}

/**
 * What a suggester proposes, before the service enforces [SUGGESTION_RULES] and works out
 * the price. Nothing here is trusted as is.
 */
export interface SuggestionDraft {
  title: string;
  category: string;
  description: string;
  discountPercent: number;
  /** From 0 to 1. */
  confidence: number;
  /** False when the photo does not seem to show food. */
  isFood: boolean;
  warnings: string[];
}

export type SuggestionSource = 'mock' | 'claude';

/** The response of POST /ai/offer-suggestions. */
export interface OfferSuggestion {
  title: string;
  category: OfferCategory;
  description: string;
  suggestedDiscountPercent: number;
  /** Null when no original price was given. */
  suggestedDiscountedPrice: number | null;
  confidence: number;
  warnings: string[];
  source: SuggestionSource;
}

/**
 * Proposes offer details from a photo. Bound in the AI module to the simulated version or
 * to the model, depending on the configuration.
 */
export abstract class OfferSuggester {
  abstract readonly source: SuggestionSource;

  abstract suggest(photo: PhotoInput): Promise<SuggestionDraft>;
}
