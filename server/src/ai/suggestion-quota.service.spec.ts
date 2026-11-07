import { ConfigService } from '@nestjs/config';
import type { DatabaseService } from '../database/database.service';
import { RESERVATION, USER_ID } from '../../test/fixtures/quota';
import { SuggestionQuotaService } from './suggestion-quota.service';

describe('SuggestionQuotaService', () => {
  const findUnique = jest.fn();
  const queryRaw = jest.fn();
  const executeRaw = jest.fn();
  const service = new SuggestionQuotaService(
    {
      client: {
        merchantProfile: { findUnique },
        $queryRaw: queryRaw,
        $executeRaw: executeRaw,
      },
    } as unknown as DatabaseService,
    new ConfigService({ AI_DAILY_LIMIT: 20 }),
  );

  /** The SQL text and the bound values of a tagged-template call. */
  const sqlOf = (mock: jest.Mock) => {
    const [strings, ...values] = mock.mock.calls[0] as [
      TemplateStringsArray,
      ...unknown[],
    ];
    return { text: strings.join('?'), values };
  };

  beforeEach(() => {
    findUnique
      .mockReset()
      .mockResolvedValue({ id: RESERVATION.merchantProfileId });
    queryRaw.mockReset().mockResolvedValue([{ day: RESERVATION.day }]);
    executeRaw.mockReset().mockResolvedValue(1);
  });

  it('counts one suggestion for the caller business on the Bogotá day, under the limit', async () => {
    await expect(service.reserve(USER_ID)).resolves.toEqual(RESERVATION);

    expect(findUnique).toHaveBeenCalledWith({
      where: { userId: USER_ID },
      select: { id: true },
    });
    const { text, values } = sqlOf(queryRaw);
    expect(values).toEqual([RESERVATION.merchantProfileId, 20]);
    expect(text).toContain("AT TIME ZONE 'America/Bogota'");
    // The limit is checked in the same statement that raises the count
    expect(text).toMatch(
      /ON CONFLICT[\s\S]*WHERE "ai_suggestion_usage"\."count" < \?/,
    );
  });

  it('answers 429 in Spanish once the day is used up', async () => {
    queryRaw.mockResolvedValue([]);

    await expect(service.reserve(USER_ID)).rejects.toMatchObject({
      status: 429,
      response: {
        statusCode: 429,
        error: 'Demasiadas solicitudes',
        message:
          'Llegaste al límite de 20 sugerencias de hoy. Vuelve a intentarlo mañana.',
      },
    });
  });

  it('asks a merchant without a business profile to complete it', async () => {
    findUnique.mockResolvedValue(null);

    await expect(service.reserve(USER_ID)).rejects.toMatchObject({
      response: {
        statusCode: 403,
        message: 'Completa el perfil de tu negocio para usar las sugerencias.',
      },
    });
    expect(queryRaw).not.toHaveBeenCalled();
  });

  it('sanitizes database failures', async () => {
    queryRaw.mockRejectedValue(new Error('private-database-details'));

    const result = service.reserve(USER_ID);

    await expect(result).rejects.toMatchObject({
      response: { statusCode: 500, error: 'Error interno' },
    });
    await expect(result).rejects.not.toThrow('private-database-details');
  });

  it('gives a reservation back to the same day, never below zero', async () => {
    await service.release(RESERVATION);

    const { text, values } = sqlOf(executeRaw);
    expect(values).toEqual([RESERVATION.merchantProfileId, RESERVATION.day]);
    expect(text).toContain('"count" = "count" - 1');
    expect(text).toContain('"count" > 0');
  });

  it('never fails the request because a release failed', async () => {
    executeRaw.mockRejectedValue(new Error('private-database-details'));

    await expect(service.release(RESERVATION)).resolves.toBeUndefined();
  });
});
