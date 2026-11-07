import {
  BadRequestException,
  UnsupportedMediaTypeException,
} from '@nestjs/common';
import { OfferCategory } from '../generated/prisma/enums';
import { jpegBytes, pngBytes, webpBytes } from '../../test/fixtures/images';
import { MockOfferSuggester } from './mock-offer-suggester';
import {
  OfferSuggester,
  SUGGESTION_RULES,
  type SuggestionDraft,
} from './offer-suggestion';
import {
  MISSING_PRICE_WARNING,
  NOT_FOOD_WARNING,
  OfferSuggestionsService,
  discountedPrice,
  finalizeSuggestion,
} from './offer-suggestions.service';

const photo = (buffer: Buffer) => ({ buffer, size: buffer.length });

const draft = (overrides: Partial<SuggestionDraft> = {}): SuggestionDraft => ({
  title: 'Pan del día',
  category: OfferCategory.BAKERY,
  description: 'Pan fresco de esta mañana.',
  discountPercent: 50,
  confidence: 0.9,
  isFood: true,
  warnings: [],
  ...overrides,
});

describe('OfferSuggestionsService', () => {
  const suggest = jest.fn<Promise<SuggestionDraft>, unknown[]>();
  const suggester = { source: 'claude', suggest } as unknown as OfferSuggester;
  const service = new OfferSuggestionsService(suggester);

  beforeEach(() => {
    suggest.mockReset().mockResolvedValue(draft());
  });

  it('passes the photo, its detected type and the price to the suggester', async () => {
    const bytes = webpBytes();
    await service.suggest(photo(bytes), 12000);

    expect(suggest).toHaveBeenCalledWith({
      bytes,
      mediaType: 'image/webp',
      originalPrice: 12000,
    });
  });

  it('answers with the finished suggestion and its source', async () => {
    await expect(service.suggest(photo(jpegBytes()), 12000)).resolves.toEqual({
      title: 'Pan del día',
      category: OfferCategory.BAKERY,
      description: 'Pan fresco de esta mañana.',
      suggestedDiscountPercent: 50,
      suggestedDiscountedPrice: 6000,
      confidence: 0.9,
      warnings: [],
      source: 'claude',
    });
  });

  it.each([undefined, photo(Buffer.alloc(0))])(
    'asks for a photo when none arrives (%#)',
    async (missing) => {
      await expect(service.suggest(missing)).rejects.toMatchObject({
        response: {
          statusCode: 400,
          message: 'Debes enviar una foto del producto.',
        },
      });
      expect(suggest).not.toHaveBeenCalled();
    },
  );

  it('rejects other formats by their bytes, whatever the file is called', async () => {
    const result = service.suggest(photo(pngBytes()));

    await expect(result).rejects.toBeInstanceOf(UnsupportedMediaTypeException);
    await expect(result).rejects.toMatchObject({
      response: {
        statusCode: 415,
        error: 'Formato no admitido',
        message: 'La foto debe ser una imagen JPEG o WebP.',
      },
    });
    expect(suggest).not.toHaveBeenCalled();
  });

  it('is a BadRequestException for a missing photo', async () => {
    await expect(service.suggest(undefined)).rejects.toBeInstanceOf(
      BadRequestException,
    );
  });
});

describe('finalizeSuggestion', () => {
  it.each([
    [5, SUGGESTION_RULES.minDiscountPercent],
    [29.6, 30],
    [45.4, 45],
    [95, SUGGESTION_RULES.maxDiscountPercent],
    [Number.NaN, SUGGESTION_RULES.minDiscountPercent],
  ])('keeps a discount of %d within 30-70 %% (→ %d)', (proposed, expected) => {
    expect(
      finalizeSuggestion(draft({ discountPercent: proposed }), 10000, 'claude')
        .suggestedDiscountPercent,
    ).toBe(expected);
  });

  it.each([
    [12000, 50, 6000],
    [12350, 40, 7400],
    [9999, 33, 6700],
    [500, 70, 200],
  ])(
    'rounds the price of %d at %d %% to the nearest 100 (→ %d)',
    (price, percent, expected) => {
      expect(discountedPrice(price, percent)).toBe(expected);
      expect(expected % SUGGESTION_RULES.priceStep).toBe(0);
    },
  );

  it('cuts long texts at a word, within the limits, with an ellipsis', () => {
    const longTitle =
      'Bolsa sorpresa de panadería con pan, croissants, galletas y más cosas';
    const result = finalizeSuggestion(
      draft({ title: longTitle, description: 'palabra '.repeat(60) }),
      undefined,
      'claude',
    );

    expect(result.title.length).toBeLessThanOrEqual(60);
    expect(result.title.endsWith('…')).toBe(true);
    expect(longTitle.startsWith(result.title.slice(0, -1))).toBe(true);
    expect(result.description.length).toBeLessThanOrEqual(240);
  });

  it('collapses whitespace in every text', () => {
    const result = finalizeSuggestion(
      draft({
        title: '  Pan \n del   día ',
        warnings: ['  Revisa\tla foto  ', ''],
      }),
      1000,
      'claude',
    );

    expect(result.title).toBe('Pan del día');
    expect(result.warnings).toEqual(['Revisa la foto']);
  });

  it('falls back to OTHER for a category that does not exist', () => {
    expect(
      finalizeSuggestion(draft({ category: 'SNACKS' }), 1000, 'claude')
        .category,
    ).toBe(OfferCategory.OTHER);
  });

  it('keeps confidence between 0 and 1 with two decimals', () => {
    expect(
      finalizeSuggestion(draft({ confidence: 1.7 }), 1, 'claude').confidence,
    ).toBe(1);
    expect(
      finalizeSuggestion(draft({ confidence: -2 }), 1, 'claude').confidence,
    ).toBe(0);
    expect(
      finalizeSuggestion(draft({ confidence: 0.876 }), 1, 'claude').confidence,
    ).toBe(0.88);
  });

  it('warns first when the photo is not food, and when the price is missing', () => {
    const result = finalizeSuggestion(
      draft({ isFood: false, warnings: ['Foto borrosa.'] }),
      undefined,
      'claude',
    );

    expect(result.warnings).toEqual([
      NOT_FOOD_WARNING,
      'Foto borrosa.',
      MISSING_PRICE_WARNING,
    ]);
    expect(result.suggestedDiscountedPrice).toBeNull();
  });

  it('drops repeated warnings and keeps at most five', () => {
    const result = finalizeSuggestion(
      draft({ warnings: ['a', 'a', 'b', 'c', 'd', 'e', 'f'] }),
      1000,
      'claude',
    );

    expect(result.warnings).toEqual(['a', 'b', 'c', 'd', 'e']);
  });
});

describe('MockOfferSuggester', () => {
  const suggester = new MockOfferSuggester();
  const service = new OfferSuggestionsService(suggester);

  it('is deterministic: the same photo always gets the same suggestion', async () => {
    const bytes = jpegBytes(512, 7);

    const first = await service.suggest(photo(bytes), 15000);
    const second = await service.suggest(photo(Buffer.from(bytes)), 15000);

    expect(second).toEqual(first);
    expect(first.source).toBe('mock');
  });

  it('stays within the rules for many different photos', async () => {
    const categories = new Set<string>();
    for (let seed = 0; seed < 64; seed++) {
      const result = await service.suggest(photo(jpegBytes(300, seed)), 10000);
      categories.add(result.category);
      expect(Object.values(OfferCategory)).toContain(result.category);
      expect(result.suggestedDiscountPercent % 5).toBe(0);
      expect(result.suggestedDiscountPercent).toBeGreaterThanOrEqual(30);
      expect(result.suggestedDiscountPercent).toBeLessThanOrEqual(70);
      expect(result.suggestedDiscountedPrice! % 100).toBe(0);
      expect(result.title.length).toBeLessThanOrEqual(60);
      expect(result.description.length).toBeLessThanOrEqual(240);
      expect(result.warnings[0]).toMatch(/^Sugerencia de ejemplo/);
    }
    // Different photos do not all collapse onto one answer
    expect(categories.size).toBeGreaterThan(1);
  });
});
