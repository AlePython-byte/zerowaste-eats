import { IsInt, IsUUID, Max, Min } from 'class-validator';

export const RESERVATION_LIMITS = {
  quantityMin: 1,
  /** Units one person may take from an offer in a single reservation. */
  quantityMax: 10,
} as const;

export class CreateReservationDto {
  @IsUUID()
  offerId: string;

  @IsInt()
  @Min(RESERVATION_LIMITS.quantityMin)
  @Max(RESERVATION_LIMITS.quantityMax)
  quantity: number;
}
