import { OfferStatus } from '../generated/prisma/enums';

/** Statuses an offer can be paused from: it is on sale, or sold out but still current. */
export const PAUSABLE_STATUSES: readonly OfferStatus[] = [
  OfferStatus.AVAILABLE,
  OfferStatus.SOLD_OUT,
];

/**
 * The status as people should see it now. An offer whose pickup window has ended is
 * EXPIRED, whatever was stored: nothing has to run at the end of the window for that to
 * hold. A cancelled offer stays cancelled.
 */
export function effectiveStatus(
  stored: OfferStatus,
  pickupEnd: Date,
  now: Date,
): OfferStatus {
  if (stored === OfferStatus.CANCELLED) return stored;
  return pickupEnd.getTime() <= now.getTime() ? OfferStatus.EXPIRED : stored;
}

/** The status after stock changes: sold out at zero, back on sale above it; pauses stay. */
export function statusForStock(
  stored: OfferStatus,
  quantityAvailable: number,
): OfferStatus {
  if (stored === OfferStatus.AVAILABLE && quantityAvailable === 0) {
    return OfferStatus.SOLD_OUT;
  }
  if (stored === OfferStatus.SOLD_OUT && quantityAvailable > 0) {
    return OfferStatus.AVAILABLE;
  }
  return stored;
}

/** Whole percent off, rounded down; 0 for a price that makes no sense. */
export function discountPercent(original: number, discounted: number): number {
  if (original <= 0 || discounted < 0 || discounted >= original) return 0;
  return Math.floor(((original - discounted) * 100) / original);
}

/** A Prisma Decimal (or anything with toNumber) as a plain number of pesos. */
export function pesos(value: { toNumber(): number } | number): number {
  return typeof value === 'number' ? value : value.toNumber();
}
