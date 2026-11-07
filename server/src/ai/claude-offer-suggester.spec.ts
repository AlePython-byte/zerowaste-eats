import Anthropic from '@anthropic-ai/sdk';
import { ServiceUnavailableException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { OfferCategory } from '../generated/prisma/enums';
import { jpegBytes, webpBytes } from '../../test/fixtures/images';
import { USER_ID, quota, resetQuota } from '../../test/fixtures/quota';
import { createOfferSuggester } from './ai.module';
import {
  CLAUDE_TIMEOUT_MS,
  ClaudeOfferSuggester,
  SuggestionSchema,
} from './claude-offer-suggester';
import { MockOfferSuggester } from './mock-offer-suggester';
import {
  NOT_FOOD_WARNING,
  OfferSuggestionsService,
} from './offer-suggestions.service';

/** A valid answer, as the model would produce it. */
const answer = {
  isFood: true,
  title: 'Bolsa de pan del día',
  category: 'BAKERY',
  description: 'Pan blanco y mogollas horneados esta mañana.',
  discountPercent: 50,
  confidence: 0.92,
  warnings: [],
};

describe('ClaudeOfferSuggester', () => {
  const parse = jest.fn();
  const client = { messages: { parse } } as unknown as Anthropic;
  const suggester = new ClaudeOfferSuggester(client, 'claude-haiku-5-5');
  const service = new OfferSuggestionsService(suggester, quota);
  let fetchSpy: jest.SpyInstance;
  let outputRead: boolean;

  /** A response whose content records being read, to prove stop_reason is checked first. */
  const respond = (stopReason: string, output: unknown = answer) => {
    outputRead = false;
    parse.mockResolvedValue({
      stop_reason: stopReason,
      get parsed_output() {
        outputRead = true;
        return output;
      },
    });
  };

  beforeEach(() => {
    // Any attempt to reach the real API fails the test
    fetchSpy = jest
      .spyOn(globalThis, 'fetch')
      .mockRejectedValue(new Error('Network disabled in tests'));
    resetQuota();
    parse.mockReset();
    respond('end_turn');
  });

  afterEach(() => {
    expect(fetchSpy).not.toHaveBeenCalled();
    jest.restoreAllMocks();
  });

  it('sends the photo, the price and a structured output format to the configured model', async () => {
    const bytes = webpBytes();
    await suggester.suggest({
      bytes,
      mediaType: 'image/webp',
      originalPrice: 12000,
    });

    const [params] = parse.mock.calls[0] as [Record<string, any>];
    expect(params.model).toBe('claude-haiku-5-5');
    expect(params.output_config.format.type).toBe('json_schema');
    expect(params.output_config.effort).toBe('low');
    expect(params.system).toMatch(/máximo 60 caracteres/);
    // No model fallbacks of any kind
    expect(params).not.toHaveProperty('fallbacks');
    expect(params).not.toHaveProperty('betas');
    const [image, text] = params.messages[0].content;
    expect(image).toEqual({
      type: 'image',
      source: {
        type: 'base64',
        media_type: 'image/webp',
        data: bytes.toString('base64'),
      },
    });
    expect(text.text).toContain('12000 pesos');
  });

  it('says when no price was given', async () => {
    await suggester.suggest({ bytes: jpegBytes(), mediaType: 'image/jpeg' });

    const [params] = parse.mock.calls[0] as [Record<string, any>];
    expect(params.messages[0].content[1].text).toContain('no indicó el precio');
  });

  it('returns the validated answer, finished by the service as source claude', async () => {
    await expect(
      service.suggest(USER_ID, { buffer: jpegBytes(), size: 256 }, 12345),
    ).resolves.toEqual({
      title: 'Bolsa de pan del día',
      category: OfferCategory.BAKERY,
      description: 'Pan blanco y mogollas horneados esta mañana.',
      suggestedDiscountPercent: 50,
      suggestedDiscountedPrice: 6200,
      confidence: 0.92,
      warnings: [],
      source: 'claude',
    });
  });

  it('holds an out-of-range answer to the rules instead of passing it on', async () => {
    respond('end_turn', {
      ...answer,
      title:
        'Bolsa grande de pan del día con mogollas, roscones y almojábanas surtidas',
      category: 'Panadería',
      description: 'x '.repeat(200),
      discountPercent: 85.4,
      confidence: 3,
    });

    const result = await service.suggest(
      USER_ID,
      { buffer: jpegBytes(), size: 256 },
      10000,
    );

    expect(result.title.length).toBeLessThanOrEqual(60);
    expect(result.description.length).toBeLessThanOrEqual(240);
    expect(result.category).toBe(OfferCategory.OTHER);
    expect(result.suggestedDiscountPercent).toBe(70);
    expect(result.suggestedDiscountedPrice).toBe(3000);
    expect(result.confidence).toBe(1);
  });

  it('turns a photo that is not food into warnings, not an error', async () => {
    respond('end_turn', {
      ...answer,
      isFood: false,
      category: 'OTHER',
      confidence: 0.1,
      warnings: ['La foto muestra un teclado.'],
    });

    const result = await service.suggest(
      USER_ID,
      { buffer: jpegBytes(), size: 256 },
      10000,
    );

    expect(result.warnings).toEqual([
      NOT_FOOD_WARNING,
      'La foto muestra un teclado.',
    ]);
    expect(result.confidence).toBe(0.1);
  });

  it.each(['max_tokens', 'refusal', 'pause_turn', 'stop_sequence'])(
    'answers 503 when the model stops with %s, without reading the content',
    async (stopReason) => {
      respond(stopReason);

      await expect(
        suggester.suggest({ bytes: jpegBytes(), mediaType: 'image/jpeg' }),
      ).rejects.toMatchObject({
        response: { statusCode: 503, error: 'Servicio no disponible' },
      });
      expect(outputRead).toBe(false);
    },
  );

  it.each([
    ['no parsed output', null],
    ['a missing field', { ...answer, title: undefined }],
    ['a field of the wrong type', { ...answer, confidence: 'alta' }],
  ])('validates the output again: %s is a 503', async (_label, output) => {
    respond('end_turn', output);

    await expect(
      suggester.suggest({ bytes: jpegBytes(), mediaType: 'image/jpeg' }),
    ).rejects.toBeInstanceOf(ServiceUnavailableException);
  });

  const headers = new Headers();
  it.each([
    [
      'a provider rate limit',
      new Anthropic.RateLimitError(429, {}, 'raw provider detail', headers),
      'El asistente está recibiendo demasiadas solicitudes. Inténtalo de nuevo en unos minutos.',
    ],
    [
      'a timeout',
      new Anthropic.APIConnectionTimeoutError({
        message: 'raw provider detail',
      }),
      'El asistente tardó demasiado en responder. Inténtalo de nuevo.',
    ],
    [
      'no connection',
      new Anthropic.APIConnectionError({ message: 'raw provider detail' }),
      'El asistente de sugerencias no está disponible en este momento. Inténtalo de nuevo más tarde.',
    ],
    [
      'an overloaded API',
      new Anthropic.InternalServerError(
        529,
        {},
        'raw provider detail',
        headers,
      ),
      'El asistente de sugerencias no está disponible en este momento. Inténtalo de nuevo más tarde.',
    ],
    [
      'a rejected key',
      new Anthropic.AuthenticationError(
        401,
        {},
        'raw provider detail',
        headers,
      ),
      'El asistente de sugerencias no está disponible en este momento. Inténtalo de nuevo más tarde.',
    ],
    [
      'a bad request',
      new Anthropic.BadRequestError(400, {}, 'raw provider detail', headers),
      'El asistente de sugerencias no está disponible en este momento. Inténtalo de nuevo más tarde.',
    ],
    [
      'anything else',
      new Error('raw provider detail'),
      'El asistente de sugerencias no está disponible en este momento. Inténtalo de nuevo más tarde.',
    ],
  ])(
    'answers %s with a Spanish 503 and never the raw error',
    async (_label, error, message) => {
      parse.mockRejectedValue(error);

      const result = suggester.suggest({
        bytes: jpegBytes(),
        mediaType: 'image/jpeg',
      });

      await expect(result).rejects.toMatchObject({
        response: { statusCode: 503, error: 'Servicio no disponible', message },
      });
      await expect(result).rejects.not.toThrow('raw provider detail');
    },
  );

  it('accepts exactly the answer shape it asks for', () => {
    expect(SuggestionSchema.safeParse(answer).success).toBe(true);
  });
});

describe('createOfferSuggester', () => {
  const config = (values: Record<string, string>) => new ConfigService(values);

  it('runs the simulated suggester without an API key', () => {
    expect(
      createOfferSuggester(config({ AI_MODEL: 'claude-haiku-5-5' })),
    ).toBeInstanceOf(MockOfferSuggester);
  });

  it('uses Claude with the key, the configured model, a 30 s timeout and no retries', () => {
    const suggester = createOfferSuggester(
      config({
        ANTHROPIC_API_KEY: 'test-key-placeholder',
        AI_MODEL: 'claude-haiku-5-5',
      }),
    ) as ClaudeOfferSuggester;

    expect(suggester).toBeInstanceOf(ClaudeOfferSuggester);
    expect(suggester.source).toBe('claude');
    expect(suggester['model']).toBe('claude-haiku-5-5');
    const client = suggester['client'];
    expect(client.timeout).toBe(CLAUDE_TIMEOUT_MS);
    expect(client.maxRetries).toBe(0);
  });
});
