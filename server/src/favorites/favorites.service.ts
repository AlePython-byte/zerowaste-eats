import {
  ForbiddenException,
  HttpException,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import type {
  Establishment,
  MerchantProfile,
  Offer,
} from '../generated/prisma/client';
import { effectiveStatus } from '../offers/offer-rules';
import {
  presentPublicOffer,
  visibleToCustomers,
} from '../offers/offers.service';
import { OfferStatus } from '../generated/prisma/enums';

type OfferWithBusiness = Offer & {
  establishment: Establishment & { merchantProfile: MerchantProfile };
};

/**
 * A person's saved offers. Saving works only for an offer customers can see now; a saved
 * offer stays in the list after it is paused, sold out or over, marked as not available.
 */
@Injectable()
export class FavoritesService {
  /** The current time; tests replace it. */
  clock: () => Date = () => new Date();

  constructor(private readonly databaseService: DatabaseService) {}

  private get db() {
    return this.databaseService.client;
  }

  /** Saving twice is the same as saving once. */
  async add(userId: string, offerId: string): Promise<void> {
    await this.guard(async () => {
      const user = await this.db.user.findUnique({
        where: { id: userId },
        select: { id: true },
      });
      if (!user) {
        throw new ForbiddenException(
          'Completa tu perfil para guardar favoritos.',
          'Prohibido',
        );
      }
      const offer = await this.db.offer.findFirst({
        where: { id: offerId, ...visibleToCustomers(this.clock()) },
        select: { id: true },
      });
      if (!offer) {
        throw new NotFoundException(
          'Esta oferta ya no está disponible.',
          'No encontrado',
        );
      }
      await this.db.favorite.upsert({
        where: { userId_offerId: { userId, offerId } },
        create: { userId, offerId },
        update: {},
      });
    });
  }

  /** Removing one that is not saved changes nothing. */
  async remove(userId: string, offerId: string): Promise<void> {
    await this.guard(() =>
      this.db.favorite.deleteMany({ where: { userId, offerId } }),
    );
  }

  /** Newest first, each with whether it can still be reserved. */
  async list(userId: string) {
    const now = this.clock();
    const favorites = await this.guard(() =>
      this.db.favorite.findMany({
        where: { userId },
        include: {
          offer: {
            include: { establishment: { include: { merchantProfile: true } } },
          },
        },
        orderBy: { createdAt: 'desc' },
      }),
    );
    return favorites.map(({ offer }) => {
      const full = offer as OfferWithBusiness;
      const status = effectiveStatus(full.status, full.pickupEnd, now);
      return {
        ...presentPublicOffer(full, null, true),
        status,
        isAvailable:
          status === OfferStatus.AVAILABLE && full.quantityAvailable > 0,
      };
    });
  }

  /** Which of [offerIds] the person saved, in one query, for marking a list. */
  async savedAmong(userId: string, offerIds: string[]): Promise<Set<string>> {
    if (offerIds.length === 0) return new Set();
    const rows = await this.guard(() =>
      this.db.favorite.findMany({
        where: { userId, offerId: { in: offerIds } },
      }),
    );
    return new Set(rows.map((row) => row.offerId));
  }

  private async guard<T>(operation: () => Promise<T>): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      if (error instanceof HttpException) throw error;
      throw new InternalServerErrorException(
        'No fue posible completar la operación con tus favoritos.',
        'Error interno',
      );
    }
  }
}
