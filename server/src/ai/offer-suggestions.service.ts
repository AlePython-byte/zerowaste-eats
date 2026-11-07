import {
  BadRequestException,
  Injectable,
  UnsupportedMediaTypeException,
} from '@nestjs/common';
import { OfferCategory } from '../generated/prisma/enums';
import { detectImageType } from './image-type';
import {
  OfferSuggester,
  SUGGESTION_RULES,
  type OfferSuggestion,
  type SuggestionDraft,
} from './offer-suggestion';

/** The part of an uploaded file the service reads. */
export interface UploadedPhoto {
  buffer: Buffer;
  size: number;
}

export const NOT_FOOD_WARNING =
  'La foto no parece mostrar comida. Usa una foto clara del producto que vas a ofrecer.';
export const MISSING_PRICE_WARNING =
  'Agrega el precio original para calcular el precio con descuento.';

const CATEGORIES = new Set<string>(Object.values(OfferCategory));
const MAX_WARNINGS = 5;

/**
 * Turns a photo into an offer suggestion. Whatever the suggester proposes is checked again
 * here: text lengths, discount range, category and price rounding are enforced the same way
 * for the simulated mode and for the model.
 */
@Injectable()
export class OfferSuggestionsService {
  constructor(private readonly suggester: OfferSuggester) {}

  async suggest(
    photo: UploadedPhoto | undefined,
    originalPrice?: number,
  ): Promise<OfferSuggestion> {
    if (!photo || photo.size === 0 || photo.buffer.length === 0) {
      throw new BadRequestException(
        'Debes enviar una foto del producto.',
        'Solicitud inválida',
      );
    }
    const mediaType = detectImageType(photo.buffer);
    if (!mediaType) {
      throw new UnsupportedMediaTypeException(
        'La foto debe ser una imagen JPEG o WebP.',
        'Formato no admitido',
      );
    }

    const draft = await this.suggester.suggest({
      bytes: photo.buffer,
      mediaType,
      originalPrice,
    });
    return finalizeSuggestion(draft, originalPrice, this.suggester.source);
  }
}

/** Enforces [SUGGESTION_RULES] on a draft and works out the discounted price. */
export function finalizeSuggestion(
  draft: SuggestionDraft,
  originalPrice: number | undefined,
  source: OfferSuggestion['source'],
): OfferSuggestion {
  const discount = clamp(
    Math.round(
      Number.isFinite(draft.discountPercent) ? draft.discountPercent : 0,
    ),
    SUGGESTION_RULES.minDiscountPercent,
    SUGGESTION_RULES.maxDiscountPercent,
  );
  const warnings = draft.warnings.map((w) => collapse(w)).filter(Boolean);
  if (!draft.isFood) warnings.unshift(NOT_FOOD_WARNING);
  if (originalPrice === undefined) warnings.push(MISSING_PRICE_WARNING);

  return {
    title: truncate(collapse(draft.title), SUGGESTION_RULES.titleMaxLength),
    category: CATEGORIES.has(draft.category)
      ? (draft.category as OfferCategory)
      : OfferCategory.OTHER,
    description: truncate(
      collapse(draft.description),
      SUGGESTION_RULES.descriptionMaxLength,
    ),
    suggestedDiscountPercent: discount,
    suggestedDiscountedPrice:
      originalPrice === undefined
        ? null
        : discountedPrice(originalPrice, discount),
    confidence:
      Math.round(
        clamp(Number.isFinite(draft.confidence) ? draft.confidence : 0, 0, 1) *
          100,
      ) / 100,
    warnings: [...new Set(warnings)].slice(0, MAX_WARNINGS),
    source,
  };
}

/** The price after [discountPercent], rounded to the nearest 100 pesos (at least 100). */
export function discountedPrice(
  originalPrice: number,
  discountPercent: number,
): number {
  const step = SUGGESTION_RULES.priceStep;
  const raw = (originalPrice * (100 - discountPercent)) / 100;
  return Math.max(step, Math.round(raw / step) * step);
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function collapse(text: string): string {
  return typeof text === 'string' ? text.replace(/\s+/g, ' ').trim() : '';
}

/** Cuts at a word boundary when one is close, and marks the cut with an ellipsis. */
function truncate(text: string, max: number): string {
  if (text.length <= max) return text;
  const room = text.slice(0, max - 1);
  const lastSpace = room.lastIndexOf(' ');
  const cut = lastSpace >= max * 0.6 ? room.slice(0, lastSpace) : room;
  return `${cut.replace(/[\s,;:.-]+$/, '')}…`;
}
