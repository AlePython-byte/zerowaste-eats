import {
  ConflictException,
  HttpException,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import { Prisma } from '../generated/prisma/client';
import { OfferStatus } from '../generated/prisma/enums';
import { effectiveStatus, pesos } from '../offers/offer-rules';
import type { CreateReservationDto } from './dto/create-reservation.dto';
import { randomPickupCode } from './pickup-code';
import {
  WITH_OFFER_AND_BUSINESS,
  presentCustomerReservation,
  type CustomerReservationView,
  type ReservationWithOffer,
} from './reservation-presenters';

/** Fresh codes tried when the drawn one already belongs to another reservation. */
const CODE_ATTEMPTS = 5;

const isUniqueViolation = (error: unknown) =>
  error instanceof Prisma.PrismaClientKnownRequestError &&
  error.code === 'P2002';

/**
 * Customers reserving units of an offer.
 *
 * Units are taken with one conditional UPDATE (on sale, enough left, pickup not over) inside
 * the transaction that creates the reservation. Postgres locks the offer row for that
 * UPDATE, so a second reservation racing for the same units waits, re-checks the condition
 * against the new stock and updates nothing: it can never take units that are gone.
 */
@Injectable()
export class ReservationsService {
  /** The current time; tests replace it. */
  clock: () => Date = () => new Date();
  /** Draws pickup codes; tests replace it to force a collision. */
  nextCode: () => string = randomPickupCode;

  constructor(private readonly databaseService: DatabaseService) {}

  private get db() {
    return this.databaseService.client;
  }

  async reserve(
    customerId: string,
    dto: CreateReservationDto,
  ): Promise<CustomerReservationView> {
    const now = this.clock();
    for (let attempt = 1; ; attempt++) {
      try {
        const reservation = await this.db.$transaction((tx) =>
          this.takeUnits(tx, customerId, dto, now),
        );
        if (!reservation) throw await this.whyUnavailable(dto, now);
        return presentCustomerReservation(reservation, now);
      } catch (error) {
        // The whole transaction was rolled back, units included: try again with a new code
        if (isUniqueViolation(error) && attempt < CODE_ATTEMPTS) continue;
        throw this.safe(error);
      }
    }
  }

  /** Null when the offer could not give the units; nothing was written then. */
  private async takeUnits(
    tx: Prisma.TransactionClient,
    customerId: string,
    { offerId, quantity }: CreateReservationDto,
    now: Date,
  ): Promise<ReservationWithOffer | null> {
    const { count } = await tx.offer.updateMany({
      where: {
        id: offerId,
        status: OfferStatus.AVAILABLE,
        quantityAvailable: { gte: quantity },
        pickupEnd: { gt: now },
        expiresAt: { gt: now },
      },
      data: { quantityAvailable: { decrement: quantity } },
    });
    if (count === 0) return null;

    // The row is still locked by the UPDATE above, so this reads our own result
    await tx.offer.updateMany({
      where: {
        id: offerId,
        status: OfferStatus.AVAILABLE,
        quantityAvailable: 0,
      },
      data: { status: OfferStatus.SOLD_OUT },
    });
    const offer = await tx.offer.findFirst({
      where: { id: offerId },
      select: { discountedPrice: true },
    });
    const unitPrice = pesos(offer!.discountedPrice);

    return (await tx.reservation.create({
      data: {
        customerId,
        offerId,
        quantity,
        reservationCode: this.nextCode(),
        unitPrice,
        totalPrice: unitPrice * quantity,
      },
      include: WITH_OFFER_AND_BUSINESS,
    })) as ReservationWithOffer;
  }

  /** Why the units could not be taken: 404 for an offer that does not exist, 409 otherwise. */
  private async whyUnavailable(
    { offerId, quantity }: CreateReservationDto,
    now: Date,
  ): Promise<HttpException> {
    const offer = await this.db.offer.findFirst({
      where: { id: offerId },
      select: { status: true, quantityAvailable: true, pickupEnd: true },
    });
    if (!offer) {
      return new NotFoundException(
        'Esta oferta ya no está disponible.',
        'No encontrado',
      );
    }
    const onSale =
      effectiveStatus(offer.status, offer.pickupEnd, now) ===
      OfferStatus.AVAILABLE;
    if (onSale && offer.quantityAvailable > 0) {
      return new ConflictException(
        offer.quantityAvailable === 1
          ? `Solo queda 1 unidad; no alcanza para ${quantity}.`
          : `Solo quedan ${offer.quantityAvailable} unidades; no alcanzan para ${quantity}.`,
        'Conflicto',
      );
    }
    return new ConflictException(
      'Ya no quedan unidades de esta oferta.',
      'Conflicto',
    );
  }

  /** Database failures become a safe 500; HTTP errors pass through. */
  private safe(error: unknown): HttpException {
    if (error instanceof HttpException) return error;
    return new InternalServerErrorException(
      'No fue posible completar la operación con tus reservas.',
      'Error interno',
    );
  }
}
