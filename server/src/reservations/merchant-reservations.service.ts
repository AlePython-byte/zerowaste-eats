import {
  HttpException,
  Injectable,
  InternalServerErrorException,
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

const MAX_LISTED = 200;

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
