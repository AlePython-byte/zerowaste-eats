import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import { ReservationStatus } from '../generated/prisma/enums';
import { pesos } from '../offers/offer-rules';

/**
 * Food rescued per unit picked up. Offers carry no weight, so this is a rough average for
 * a bag or a portion; every figure built on it is marked as an estimate.
 */
export const ESTIMATED_KG_PER_UNIT = 0.4;

export interface CustomerImpactStats {
  /** Reservations picked up. */
  completedOrders: number;
  /** Units picked up across those reservations. */
  unitsRescued: number;
  /** Pesos saved: (original price − price paid) × units, over what was picked up. */
  moneySaved: number;
  /** Approximate kilograms of food rescued: units × [ESTIMATED_KG_PER_UNIT]. */
  foodRescuedKg: number;
  /** Always true: [foodRescuedKg] is an estimate, not a weighed amount. */
  foodRescuedKgIsEstimate: true;
  kgPerUnitEstimate: number;
}

/** What a customer's pickups added up to. Only COMPLETED reservations count. */
@Injectable()
export class CustomerStatsService {
  constructor(private readonly databaseService: DatabaseService) {}

  async forCustomer(customerId: string): Promise<CustomerImpactStats> {
    let rows: {
      quantity: number;
      unitPrice: { toNumber(): number } | number;
      offer: { originalPrice: { toNumber(): number } | number };
    }[];
    try {
      rows = await this.databaseService.client.reservation.findMany({
        where: { customerId, status: ReservationStatus.COMPLETED },
        select: {
          quantity: true,
          unitPrice: true,
          offer: { select: { originalPrice: true } },
        },
      });
    } catch {
      throw new InternalServerErrorException(
        'No fue posible calcular tus estadísticas.',
        'Error interno',
      );
    }

    // In cents, so adding many prices never drifts
    let savedCents = 0;
    let units = 0;
    for (const row of rows) {
      const perUnit = Math.max(
        0,
        Math.round(pesos(row.offer.originalPrice) * 100) -
          Math.round(pesos(row.unitPrice) * 100),
      );
      savedCents += perUnit * row.quantity;
      units += row.quantity;
    }

    return {
      completedOrders: rows.length,
      unitsRescued: units,
      moneySaved: savedCents / 100,
      foodRescuedKg: Math.round(units * ESTIMATED_KG_PER_UNIT * 10) / 10,
      foodRescuedKgIsEstimate: true,
      kgPerUnitEstimate: ESTIMATED_KG_PER_UNIT,
    };
  }
}
