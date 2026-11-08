import {
  HttpException,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import { OfferStatus } from '../generated/prisma/enums';
import type {
  Establishment,
  MerchantProfile,
  Offer,
  Prisma,
} from '../generated/prisma/client';
import {
  NEARBY_LIMITS,
  type NearbyOffersQueryDto,
  type NearbySort,
} from './dto/nearby-offers-query.dto';
import { boundingBox, haversineKm, type Point } from './geo';
import { discountPercent, pesos } from './offer-rules';

/** How many candidates the database returns before the exact distance is checked. */
const MAX_CANDIDATES = 500;

type OfferWithBusiness = Offer & {
  establishment: Establishment & { merchantProfile: MerchantProfile };
};

const number = (value: { toNumber(): number } | number): number =>
  typeof value === 'number' ? value : value.toNumber();

/** On sale, with units left, and still in time to pick up. */
export function visibleToCustomers(now: Date): Prisma.OfferWhereInput {
  return {
    status: OfferStatus.AVAILABLE,
    quantityAvailable: { gt: 0 },
    pickupEnd: { gt: now },
  };
}

export function presentPublicOffer(
  offer: OfferWithBusiness,
  distanceKm: number | null,
  isFavorite: boolean,
) {
  const originalPrice = pesos(offer.originalPrice);
  const discountedPrice = pesos(offer.discountedPrice);
  const { establishment } = offer;
  return {
    id: offer.id,
    title: offer.title,
    description: offer.description,
    category: offer.category,
    imageUrl: offer.imageUrl,
    originalPrice,
    discountedPrice,
    discountPercent: discountPercent(originalPrice, discountedPrice),
    quantityAvailable: offer.quantityAvailable,
    pickupStart: offer.pickupStart.toISOString(),
    pickupEnd: offer.pickupEnd.toISOString(),
    /** One decimal, in kilometres; null when the customer's location is unknown. */
    distanceKm: distanceKm === null ? null : Math.round(distanceKm * 10) / 10,
    isFavorite,
    business: {
      name: establishment.merchantProfile.businessName,
      isVerified: establishment.merchantProfile.isVerified,
      establishmentName: establishment.name,
      address: establishment.address,
      city: establishment.city,
      latitude: number(establishment.latitude),
      longitude: number(establishment.longitude),
    },
  };
}

export type PublicOfferView = ReturnType<typeof presentPublicOffer>;

const distanceOf = (offer: OfferWithBusiness, from: Point) =>
  haversineKm(from, {
    latitude: number(offer.establishment.latitude),
    longitude: number(offer.establishment.longitude),
  });

/** Each sort, with distance then id as tie-breakers so the order is stable. */
const comparators: Record<
  NearbySort,
  (a: PublicOfferView, b: PublicOfferView) => number
> = {
  distance: () => 0,
  price: (a, b) => a.discountedPrice - b.discountedPrice,
  discount: (a, b) => b.discountPercent - a.discountPercent,
  pickup: (a, b) => a.pickupEnd.localeCompare(b.pickupEnd),
};

/**
 * What customers browse: offers on sale near them. The database narrows candidates to a
 * latitude/longitude box around the customer (cheap, indexed columns); the exact Haversine
 * distance then decides who is inside the radius.
 */
@Injectable()
export class OffersService {
  /** The current time; tests replace it. */
  clock: () => Date = () => new Date();

  constructor(private readonly databaseService: DatabaseService) {}

  async nearby(
    query: NearbyOffersQueryDto,
    favoriteIds: (offerIds: string[]) => Promise<Set<string>> = async () =>
      new Set(),
  ): Promise<PublicOfferView[]> {
    const now = this.clock();
    const from: Point = { latitude: query.lat, longitude: query.lng };
    const radiusKm = query.radiusKm ?? NEARBY_LIMITS.defaultRadiusKm;
    const box = boundingBox(from, radiusKm);
    const text = query.q?.trim();

    const where: Prisma.OfferWhereInput = {
      ...visibleToCustomers(now),
      ...(query.category ? { category: query.category } : {}),
      establishment: {
        latitude: { gte: box.minLatitude, lte: box.maxLatitude },
        ...(box.minLongitude !== null && box.maxLongitude !== null
          ? { longitude: { gte: box.minLongitude, lte: box.maxLongitude } }
          : {}),
      },
      ...(text
        ? {
            OR: [
              { title: { contains: text, mode: 'insensitive' } },
              { description: { contains: text, mode: 'insensitive' } },
              {
                establishment: {
                  merchantProfile: {
                    businessName: { contains: text, mode: 'insensitive' },
                  },
                },
              },
            ],
          }
        : {}),
    };

    const candidates = (await this.guard(() =>
      this.databaseService.client.offer.findMany({
        where,
        include: { establishment: { include: { merchantProfile: true } } },
        orderBy: { pickupEnd: 'asc' },
        take: MAX_CANDIDATES,
      }),
    )) as OfferWithBusiness[];

    const inRadius = candidates
      .map((offer) => ({ offer, distance: distanceOf(offer, from) }))
      .filter(({ distance }) => distance <= radiusKm);
    const favorites = await this.guard(() =>
      favoriteIds(inRadius.map(({ offer }) => offer.id)),
    );

    const sort = comparators[query.sort ?? 'distance'];
    return inRadius
      .map(({ offer, distance }) => ({
        view: presentPublicOffer(offer, distance, favorites.has(offer.id)),
        distance,
      }))
      .sort(
        (a, b) =>
          sort(a.view, b.view) ||
          a.distance - b.distance ||
          a.view.id.localeCompare(b.view.id),
      )
      .slice(0, query.limit ?? NEARBY_LIMITS.defaultLimit)
      .map(({ view }) => view);
  }

  /** One offer customers can still reserve; anything else is a 404. */
  async detail(
    offerId: string,
    from: Point | null,
    isFavorite: (offerId: string) => Promise<boolean> = async () => false,
  ): Promise<PublicOfferView> {
    const offer = (await this.guard(() =>
      this.databaseService.client.offer.findFirst({
        where: { id: offerId, ...visibleToCustomers(this.clock()) },
        include: { establishment: { include: { merchantProfile: true } } },
      }),
    )) as OfferWithBusiness | null;
    if (!offer) {
      throw new NotFoundException(
        'Esta oferta ya no está disponible.',
        'No encontrado',
      );
    }
    return presentPublicOffer(
      offer,
      from ? distanceOf(offer, from) : null,
      await this.guard(() => isFavorite(offer.id)),
    );
  }

  private async guard<T>(operation: () => Promise<T>): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      if (error instanceof HttpException) throw error;
      throw new InternalServerErrorException(
        'No fue posible consultar las ofertas.',
        'Error interno',
      );
    }
  }
}
