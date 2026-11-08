import type {
  Establishment,
  MerchantProfile,
  Offer,
  Reservation,
} from '../generated/prisma/client';
import { effectiveStatus, pesos } from '../offers/offer-rules';

/** A Prisma Decimal, such as a coordinate, as a plain number. */
const number = pesos;

export type ReservationWithOffer = Reservation & {
  offer: Offer & {
    establishment: Establishment & { merchantProfile: MerchantProfile };
  };
};

/** How the include for [ReservationWithOffer] is spelled in queries. */
export const WITH_OFFER_AND_BUSINESS = {
  offer: { include: { establishment: { include: { merchantProfile: true } } } },
} as const;

/** What the customer sees: their pickup code, what they reserved and where to pick it up. */
export function presentCustomerReservation(
  reservation: ReservationWithOffer,
  now: Date,
) {
  const { offer } = reservation;
  const { establishment } = offer;
  return {
    id: reservation.id,
    code: reservation.reservationCode,
    status: reservation.status,
    quantity: reservation.quantity,
    unitPrice: pesos(reservation.unitPrice),
    totalPrice: pesos(reservation.totalPrice),
    createdAt: reservation.createdAt.toISOString(),
    completedAt: reservation.completedAt?.toISOString() ?? null,
    offer: {
      id: offer.id,
      title: offer.title,
      category: offer.category,
      imageUrl: offer.imageUrl,
      originalPrice: pesos(offer.originalPrice),
      pickupStart: offer.pickupStart.toISOString(),
      pickupEnd: offer.pickupEnd.toISOString(),
      status: effectiveStatus(offer.status, offer.pickupEnd, now),
    },
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

export type CustomerReservationView = ReturnType<
  typeof presentCustomerReservation
>;
