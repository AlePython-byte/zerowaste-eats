import {
  BadRequestException,
  ConflictException,
  HttpException,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { DatabaseService } from '../../database/database.service';
import { OfferStatus } from '../../generated/prisma/enums';
import type { Offer, Prisma } from '../../generated/prisma/client';
import { isOwnOfferPhotoUrl } from '../../offers/offer-image-url';
import {
  PAUSABLE_STATUSES,
  discountPercent,
  effectiveStatus,
  pesos,
  statusForStock,
} from '../../offers/offer-rules';
import {
  OFFER_LIMITS,
  type CreateMerchantOfferDto,
  type UpdateMerchantOfferDto,
} from './dto/merchant-offer.dto';

const DAY_MS = 24 * 60 * 60 * 1000;
const MAX_LISTED = 200;

/** Every query of this service is limited to offers of the caller's own establishments. */
const ownedBy = (userId: string): Prisma.OfferWhereInput => ({
  establishment: { merchantProfile: { userId } },
});

export function presentMerchantOffer(offer: Offer, now: Date) {
  const originalPrice = pesos(offer.originalPrice);
  const discountedPrice = pesos(offer.discountedPrice);
  return {
    id: offer.id,
    establishmentId: offer.establishmentId,
    title: offer.title,
    description: offer.description,
    category: offer.category,
    imageUrl: offer.imageUrl,
    originalPrice,
    discountedPrice,
    discountPercent: discountPercent(originalPrice, discountedPrice),
    quantityTotal: offer.quantityTotal,
    quantityAvailable: offer.quantityAvailable,
    pickupStart: offer.pickupStart.toISOString(),
    pickupEnd: offer.pickupEnd.toISOString(),
    status: effectiveStatus(offer.status, offer.pickupEnd, now),
    createdAt: offer.createdAt.toISOString(),
    updatedAt: offer.updatedAt.toISOString(),
  };
}

export type MerchantOfferView = ReturnType<typeof presentMerchantOffer>;

/**
 * A business's own offers. Another business's offer is indistinguishable from one that does
 * not exist (404), so ids cannot be probed. EXPIRED is worked out when reading.
 */
@Injectable()
export class MerchantOffersService {
  private readonly supabaseUrl: string;

  /** The current time; tests replace it to check expiry. */
  clock: () => Date = () => new Date();

  constructor(
    private readonly databaseService: DatabaseService,
    config: ConfigService,
  ) {
    this.supabaseUrl = config.getOrThrow<string>('SUPABASE_URL');
  }

  private get db() {
    return this.databaseService.client;
  }

  async list(
    userId: string,
    status?: OfferStatus,
  ): Promise<MerchantOfferView[]> {
    const offers = await this.guard(() =>
      this.db.offer.findMany({
        where: ownedBy(userId),
        orderBy: { pickupStart: 'desc' },
        take: MAX_LISTED,
      }),
    );
    const now = this.clock();
    return offers
      .map((offer) => presentMerchantOffer(offer, now))
      .filter((offer) => status === undefined || offer.status === status);
  }

  async get(userId: string, offerId: string): Promise<MerchantOfferView> {
    return presentMerchantOffer(
      await this.findOwn(userId, offerId),
      this.clock(),
    );
  }

  async create(
    userId: string,
    dto: CreateMerchantOfferDto,
  ): Promise<MerchantOfferView> {
    const now = this.clock();
    const pickupStart = new Date(dto.pickupStart);
    const pickupEnd = new Date(dto.pickupEnd);
    this.checkPrices(dto.originalPrice, dto.discountedPrice);
    this.checkWindow(pickupStart, pickupEnd, now);
    this.checkImage(dto.imageUrl, userId);

    const establishment = await this.guard(() =>
      this.db.establishment.findFirst({
        where: {
          merchantProfile: { userId },
          ...(dto.establishmentId ? { id: dto.establishmentId } : {}),
        },
        orderBy: { createdAt: 'asc' },
        select: { id: true },
      }),
    );
    if (!establishment) {
      throw new NotFoundException(
        dto.establishmentId
          ? 'No encontramos ese establecimiento.'
          : 'Completa el perfil de tu negocio antes de publicar ofertas.',
        'No encontrado',
      );
    }

    const offer = await this.guard(() =>
      this.db.offer.create({
        data: {
          establishmentId: establishment.id,
          title: dto.title.trim(),
          description: dto.description?.trim() || null,
          category: dto.category,
          imageUrl: dto.imageUrl ?? null,
          originalPrice: dto.originalPrice,
          discountedPrice: dto.discountedPrice,
          quantityTotal: dto.quantityTotal,
          quantityAvailable: dto.quantityTotal,
          pickupStart,
          pickupEnd,
          expiresAt: pickupEnd,
          status: OfferStatus.AVAILABLE,
        },
      }),
    );
    return presentMerchantOffer(offer, now);
  }

  async update(
    userId: string,
    offerId: string,
    dto: UpdateMerchantOfferDto,
  ): Promise<MerchantOfferView> {
    if (Object.values(dto).every((value) => value === undefined)) {
      throw new BadRequestException(
        'Envía al menos un dato para cambiar.',
        'Solicitud inválida',
      );
    }
    const now = this.clock();
    const current = await this.findOwn(userId, offerId);
    if (current.status === OfferStatus.CANCELLED) {
      throw new ConflictException(
        'Esta oferta fue cancelada y ya no se puede editar.',
        'Conflicto',
      );
    }

    const originalPrice = dto.originalPrice ?? pesos(current.originalPrice);
    const discountedPrice =
      dto.discountedPrice ?? pesos(current.discountedPrice);
    this.checkPrices(originalPrice, discountedPrice);

    const pickupStart = dto.pickupStart
      ? new Date(dto.pickupStart)
      : current.pickupStart;
    const pickupEnd = dto.pickupEnd
      ? new Date(dto.pickupEnd)
      : current.pickupEnd;
    // An untouched window may already be over: editing the text of a past offer is fine
    if (dto.pickupStart !== undefined || dto.pickupEnd !== undefined) {
      this.checkWindow(pickupStart, pickupEnd, now);
    }
    if (dto.imageUrl) this.checkImage(dto.imageUrl, userId);

    // Units already reserved stay reserved: the new total cannot go below them
    const reserved = current.quantityTotal - current.quantityAvailable;
    const quantityTotal = dto.quantityTotal ?? current.quantityTotal;
    if (quantityTotal < reserved) {
      throw new ConflictException(
        `Ya hay ${reserved} unidades reservadas; la cantidad no puede ser menor.`,
        'Conflicto',
      );
    }
    const quantityAvailable = quantityTotal - reserved;

    const offer = await this.guard(() =>
      this.db.offer.update({
        where: { id: current.id },
        data: {
          ...(dto.title !== undefined ? { title: dto.title.trim() } : {}),
          ...(dto.description !== undefined
            ? { description: dto.description?.trim() || null }
            : {}),
          ...(dto.category !== undefined ? { category: dto.category } : {}),
          ...(dto.imageUrl !== undefined ? { imageUrl: dto.imageUrl } : {}),
          originalPrice,
          discountedPrice,
          quantityTotal,
          quantityAvailable,
          pickupStart,
          pickupEnd,
          expiresAt: pickupEnd,
          status: statusForStock(current.status, quantityAvailable),
        },
      }),
    );
    return presentMerchantOffer(offer, now);
  }

  /** Hides a current offer from customers. Pausing a paused offer changes nothing. */
  async pause(userId: string, offerId: string): Promise<MerchantOfferView> {
    const now = this.clock();
    const { count } = await this.guard(() =>
      this.db.offer.updateMany({
        where: {
          id: offerId,
          ...ownedBy(userId),
          status: { in: [...PAUSABLE_STATUSES] },
          pickupEnd: { gt: now },
        },
        data: { status: OfferStatus.PAUSED },
      }),
    );
    const offer = await this.findOwn(userId, offerId);
    if (count === 0 && offer.status !== OfferStatus.PAUSED) {
      throw this.transitionConflict(offer, now);
    }
    return presentMerchantOffer(offer, now);
  }

  /**
   * Puts a paused offer back on sale, or sold out if nothing is left. Resuming an offer that
   * is not paused changes nothing.
   */
  async resume(userId: string, offerId: string): Promise<MerchantOfferView> {
    const now = this.clock();
    const paused = {
      id: offerId,
      ...ownedBy(userId),
      status: OfferStatus.PAUSED,
      pickupEnd: { gt: now },
    };
    const withStock = await this.guard(() =>
      this.db.offer.updateMany({
        where: { ...paused, quantityAvailable: { gt: 0 } },
        data: { status: OfferStatus.AVAILABLE },
      }),
    );
    const withoutStock =
      withStock.count > 0
        ? { count: 0 }
        : await this.guard(() =>
            this.db.offer.updateMany({
              where: { ...paused, quantityAvailable: 0 },
              data: { status: OfferStatus.SOLD_OUT },
            }),
          );
    const offer = await this.findOwn(userId, offerId);
    const changed = withStock.count + withoutStock.count > 0;
    if (!changed && !PAUSABLE_STATUSES.includes(offer.status)) {
      throw this.transitionConflict(offer, now);
    }
    if (
      !changed &&
      effectiveStatus(offer.status, offer.pickupEnd, now) ===
        OfferStatus.EXPIRED
    ) {
      throw this.transitionConflict(offer, now);
    }
    return presentMerchantOffer(offer, now);
  }

  /**
   * Deletes an offer nobody reserved. One with reservations keeps its history: it can be
   * paused instead.
   */
  async remove(userId: string, offerId: string): Promise<void> {
    const offer = await this.findOwn(userId, offerId);
    const reservations = await this.guard(() =>
      this.db.reservation.count({ where: { offerId: offer.id } }),
    );
    if (reservations > 0) {
      throw new ConflictException(
        'Esta oferta tiene reservas y no se puede borrar. Puedes pausarla.',
        'Conflicto',
      );
    }
    await this.guard(() => this.db.offer.delete({ where: { id: offer.id } }));
  }

  private async findOwn(userId: string, offerId: string): Promise<Offer> {
    const offer = await this.guard(() =>
      this.db.offer.findFirst({ where: { id: offerId, ...ownedBy(userId) } }),
    );
    if (!offer) {
      throw new NotFoundException(
        'No encontramos esa oferta.',
        'No encontrado',
      );
    }
    return offer;
  }

  private transitionConflict(offer: Offer, now: Date): ConflictException {
    const status = effectiveStatus(offer.status, offer.pickupEnd, now);
    const message =
      status === OfferStatus.EXPIRED
        ? 'La oferta ya venció. Cambia el horario de recogida para volver a publicarla.'
        : status === OfferStatus.CANCELLED
          ? 'Esta oferta fue cancelada.'
          : 'La oferta no se puede cambiar a ese estado ahora.';
    return new ConflictException(message, 'Conflicto');
  }

  private checkPrices(original: number, discounted: number): void {
    if (discounted >= original) {
      throw new BadRequestException(
        'El precio con descuento debe ser menor que el precio original.',
        'Solicitud inválida',
      );
    }
  }

  private checkWindow(start: Date, end: Date, now: Date): void {
    if (end.getTime() <= start.getTime()) {
      throw new BadRequestException(
        'La recogida debe terminar después de empezar.',
        'Solicitud inválida',
      );
    }
    if (end.getTime() <= now.getTime()) {
      throw new BadRequestException(
        'La recogida debe terminar en el futuro.',
        'Solicitud inválida',
      );
    }
    if (end.getTime() > now.getTime() + OFFER_LIMITS.pickupDaysAhead * DAY_MS) {
      throw new BadRequestException(
        `La recogida debe terminar dentro de los próximos ${OFFER_LIMITS.pickupDaysAhead} días.`,
        'Solicitud inválida',
      );
    }
  }

  private checkImage(url: string | null | undefined, userId: string): void {
    if (url && !isOwnOfferPhotoUrl(url, this.supabaseUrl, userId)) {
      throw new BadRequestException(
        'La foto debe estar en tu carpeta de fotos de ofertas.',
        'Solicitud inválida',
      );
    }
  }

  /** Database failures become a safe 500; HTTP errors pass through. */
  private async guard<T>(operation: () => Promise<T>): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      if (error instanceof HttpException) throw error;
      throw new InternalServerErrorException(
        'No fue posible completar la operación con tus ofertas.',
        'Error interno',
      );
    }
  }
}
