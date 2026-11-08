import { timingSafeEqual } from 'node:crypto';
import {
  BadRequestException,
  ConflictException,
  HttpException,
  HttpStatus,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import type { Prisma } from '../generated/prisma/client';
import { ReservationStatus } from '../generated/prisma/enums';
import {
  WITH_OFFER_AND_CUSTOMER,
  presentBusinessReservation,
  type BusinessReservationView,
  type ReservationForBusiness,
} from './reservation-presenters';
import { pickupCodeBody } from './pickup-code';

const MAX_LISTED = 200;

/** Wrong codes a business may type for one reservation within the window. */
export const REDEEM_LIMITS = {
  maxFailures: 5,
  windowMs: 10 * 60_000,
} as const;

/** What happened to a redeem attempt, decided inside the transaction. */
type RedeemOutcome =
  | { kind: 'redeemed'; reservation: ReservationForBusiness }
  | { kind: 'not-pending'; status: ReservationStatus }
  | { kind: 'locked'; retryAt: Date }
  | { kind: 'wrong'; remaining: number };

const sameCode = (typed: string, stored: string): boolean => {
  const a = Buffer.from(pickupCodeBody(typed));
  const b = Buffer.from(pickupCodeBody(stored));
  return a.length === b.length && timingSafeEqual(a, b);
};

/** When the oldest failure still counting leaves the window and one more try is allowed. */
const retryAt = (failures: Date[]): Date =>
  new Date(
    failures[failures.length - REDEEM_LIMITS.maxFailures].getTime() +
      REDEEM_LIMITS.windowMs,
  );

const NOT_PENDING = Object.values(ReservationStatus).filter(
  (status) => status !== ReservationStatus.PENDING,
);

/** Every query of this service is limited to reservations of the caller's own offers. */
const ownedBy = (userId: string): Prisma.ReservationWhereInput => ({
  offer: { establishment: { merchantProfile: { userId } } },
});

/** The reservations made on a business's offers. */
@Injectable()
export class MerchantReservationsService {
  /** The current time; tests replace it. */
  clock: () => Date = () => new Date();

  constructor(private readonly databaseService: DatabaseService) {}

  private get db() {
    return this.databaseService.client;
  }

  /**
   * Pending ones first, the oldest at the top (who has waited longest), then the rest,
   * newest first.
   */
  async list(userId: string): Promise<BusinessReservationView[]> {
    const now = this.clock();
    const [pending, others] = await this.guard(() =>
      Promise.all([
        this.db.reservation.findMany({
          where: { ...ownedBy(userId), status: ReservationStatus.PENDING },
          include: WITH_OFFER_AND_CUSTOMER,
          orderBy: { createdAt: 'asc' },
          take: MAX_LISTED,
        }),
        this.db.reservation.findMany({
          where: { ...ownedBy(userId), status: { in: NOT_PENDING } },
          include: WITH_OFFER_AND_CUSTOMER,
          orderBy: { createdAt: 'desc' },
          take: MAX_LISTED,
        }),
      ]),
    );
    return [...pending, ...others]
      .slice(0, MAX_LISTED)
      .map((row) =>
        presentBusinessReservation(row as ReservationForBusiness, now),
      );
  }

  /**
   * Hands over a reservation: with the code the customer shows, a PENDING reservation of
   * this business becomes COMPLETED, once. A wrong code counts as a failed attempt; after
   * [REDEEM_LIMITS.maxFailures] of them within the window the reservation answers 429, even
   * to the right code, until the oldest one ages out.
   */
  async redeem(
    userId: string,
    reservationId: string,
    typedCode: string,
  ): Promise<BusinessReservationView> {
    const now = this.clock();
    const own = await this.guard(() =>
      this.db.reservation.findFirst({
        where: { id: reservationId, ...ownedBy(userId) },
        select: { id: true },
      }),
    );
    if (!own) {
      throw new NotFoundException(
        'No encontramos esa reserva.',
        'No encontrado',
      );
    }

    const outcome = await this.guard(() =>
      this.db.$transaction((tx) =>
        this.attempt(tx, reservationId, typedCode, now),
      ),
    );
    switch (outcome.kind) {
      case 'redeemed':
        return presentBusinessReservation(outcome.reservation, now);
      case 'not-pending':
        throw new ConflictException(
          outcome.status === ReservationStatus.COMPLETED
            ? 'Ya canjeado: esta reserva ya se entregó.'
            : outcome.status === ReservationStatus.CANCELLED
              ? 'Esta reserva fue cancelada.'
              : 'Esta reserva no se puede canjear ahora.',
          'Conflicto',
        );
      case 'locked': {
        const minutes = Math.max(
          1,
          Math.ceil((outcome.retryAt.getTime() - now.getTime()) / 60_000),
        );
        throw new HttpException(
          {
            statusCode: HttpStatus.TOO_MANY_REQUESTS,
            error: 'Demasiadas solicitudes',
            message: `Demasiados intentos con un código incorrecto. Intenta de nuevo en ${minutes} ${minutes === 1 ? 'minuto' : 'minutos'}.`,
          },
          HttpStatus.TOO_MANY_REQUESTS,
        );
      }
      case 'wrong':
        throw new BadRequestException(
          `El código no corresponde a esta reserva. ${
            outcome.remaining === 1
              ? 'Te queda 1 intento.'
              : `Te quedan ${outcome.remaining} intentos.`
          }`,
          'Solicitud inválida',
        );
    }
  }

  /**
   * One attempt, with the reservation row locked: attempts on the same reservation run one
   * at a time, so parallel guesses cannot slip past the limit and a code is never redeemed
   * twice. Failed attempts are written and committed even though the request fails.
   */
  private async attempt(
    tx: Prisma.TransactionClient,
    reservationId: string,
    typedCode: string,
    now: Date,
  ): Promise<RedeemOutcome> {
    await tx.$executeRaw`SELECT 1 FROM "reservations" WHERE "id" = ${reservationId}::uuid FOR UPDATE`;
    const reservation = (await tx.reservation.findFirst({
      where: { id: reservationId },
      include: WITH_OFFER_AND_CUSTOMER,
    })) as ReservationForBusiness;
    if (reservation.status !== ReservationStatus.PENDING) {
      return { kind: 'not-pending', status: reservation.status };
    }

    const windowStart = new Date(now.getTime() - REDEEM_LIMITS.windowMs);
    await tx.redemptionAttempt.deleteMany({
      where: { reservationId, attemptedAt: { lte: windowStart } },
    });
    const failures = (
      await tx.redemptionAttempt.findMany({
        where: { reservationId },
        orderBy: { attemptedAt: 'asc' },
        select: { attemptedAt: true },
      })
    ).map((row) => row.attemptedAt);
    if (failures.length >= REDEEM_LIMITS.maxFailures) {
      return { kind: 'locked', retryAt: retryAt(failures) };
    }

    if (!sameCode(typedCode, reservation.reservationCode)) {
      await tx.redemptionAttempt.create({
        data: { reservationId, attemptedAt: now },
      });
      failures.push(now);
      return failures.length >= REDEEM_LIMITS.maxFailures
        ? { kind: 'locked', retryAt: retryAt(failures) }
        : {
            kind: 'wrong',
            remaining: REDEEM_LIMITS.maxFailures - failures.length,
          };
    }

    await tx.redemptionAttempt.deleteMany({ where: { reservationId } });
    const redeemed = await tx.reservation.update({
      where: { id: reservationId },
      data: { status: ReservationStatus.COMPLETED, completedAt: now },
      include: WITH_OFFER_AND_CUSTOMER,
    });
    return {
      kind: 'redeemed',
      reservation: redeemed as ReservationForBusiness,
    };
  }

  /** Database failures become a safe 500; HTTP errors pass through. */
  private async guard<T>(operation: () => Promise<T>): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      if (error instanceof HttpException) throw error;
      throw new InternalServerErrorException(
        'No fue posible completar la operación con las reservas de tu negocio.',
        'Error interno',
      );
    }
  }
}
