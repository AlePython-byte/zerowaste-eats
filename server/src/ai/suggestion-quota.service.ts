import {
  ForbiddenException,
  HttpException,
  HttpStatus,
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { DatabaseService } from '../database/database.service';

/** A suggestion counted against a business's day; give it back with [release] if it failed. */
export interface QuotaReservation {
  merchantProfileId: string;
  /**
   * The Bogotá day it was counted on, as YYYY-MM-DD text (no time zone conversions), so a
   * release after midnight returns it to that day.
   */
  day: string;
}

/**
 * Daily suggestions per business (AI_DAILY_LIMIT). The count lives in ai_suggestion_usage,
 * one row per business and Bogotá day, and is raised in a single statement that refuses to
 * pass the limit: parallel requests can never go over it.
 */
@Injectable()
export class SuggestionQuotaService {
  private readonly logger = new Logger(SuggestionQuotaService.name);
  private readonly dailyLimit: number;

  constructor(
    private readonly databaseService: DatabaseService,
    config: ConfigService,
  ) {
    this.dailyLimit = config.getOrThrow<number>('AI_DAILY_LIMIT');
  }

  async reserve(userId: string): Promise<QuotaReservation> {
    let merchant: { id: string } | null;
    let counted: { day: string }[];
    try {
      merchant = await this.databaseService.client.merchantProfile.findUnique({
        where: { userId },
        select: { id: true },
      });
      if (!merchant) {
        throw new ForbiddenException(
          'Completa el perfil de tu negocio para usar las sugerencias.',
          'Prohibido',
        );
      }
      // Adds one only while under the limit; no row back means the limit was reached
      counted = await this.databaseService.client.$queryRaw<{ day: string }[]>`
        INSERT INTO "ai_suggestion_usage" ("merchantProfileId", "day", "count", "updatedAt")
        VALUES (${merchant.id}::uuid, (now() AT TIME ZONE 'America/Bogota')::date, 1, now())
        ON CONFLICT ("merchantProfileId", "day") DO UPDATE
          SET "count" = "ai_suggestion_usage"."count" + 1, "updatedAt" = now()
          WHERE "ai_suggestion_usage"."count" < ${this.dailyLimit}
        RETURNING "day"::text AS "day"`;
    } catch (error) {
      if (error instanceof HttpException) throw error;
      throw new InternalServerErrorException(
        'No fue posible verificar tu cupo de sugerencias.',
        'Error interno',
      );
    }

    if (counted.length === 0) {
      throw new HttpException(
        {
          statusCode: HttpStatus.TOO_MANY_REQUESTS,
          error: 'Demasiadas solicitudes',
          message: `Llegaste al límite de ${this.dailyLimit} sugerencias de hoy. Vuelve a intentarlo mañana.`,
        },
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }
    return { merchantProfileId: merchant.id, day: counted[0].day };
  }

  /**
   * Gives back a reservation whose suggestion failed on our side (the model was down, say),
   * so the business is not charged for it. Best effort: a failure here is only logged.
   */
  async release(reservation: QuotaReservation): Promise<void> {
    try {
      await this.databaseService.client.$executeRaw`
        UPDATE "ai_suggestion_usage"
        SET "count" = "count" - 1, "updatedAt" = now()
        WHERE "merchantProfileId" = ${reservation.merchantProfileId}::uuid
          AND "day" = ${reservation.day}::date
          AND "count" > 0`;
    } catch {
      this.logger.warn('Could not release a suggestion reservation');
    }
  }
}
