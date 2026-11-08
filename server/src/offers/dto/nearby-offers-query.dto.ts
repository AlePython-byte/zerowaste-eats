import { Transform, Type } from 'class-transformer';
import {
  IsEnum,
  IsIn,
  IsInt,
  IsNumber,
  IsString,
  Length,
  Max,
  Min,
  ValidateIf,
} from 'class-validator';
import { OfferCategory } from '../../generated/prisma/enums';

export const NEARBY_LIMITS = {
  defaultRadiusKm: 5,
  minRadiusKm: 0.1,
  maxRadiusKm: 50,
  defaultLimit: 30,
  maxLimit: 50,
  queryMin: 2,
  queryMax: 60,
} as const;

export const NEARBY_SORTS = [
  'distance',
  'price',
  'discount',
  'pickup',
] as const;
export type NearbySort = (typeof NEARBY_SORTS)[number];

const present = (_object: unknown, value: unknown) => value !== undefined;

/** Where the customer is. Query values arrive as text and are read as numbers. */
export class LocationQueryDto {
  @Type(() => Number)
  @IsNumber({ allowNaN: false, allowInfinity: false })
  @Min(-90)
  @Max(90)
  lat: number;

  @Type(() => Number)
  @IsNumber({ allowNaN: false, allowInfinity: false })
  @Min(-180)
  @Max(180)
  lng: number;
}

/** GET /offers: offers on sale around a point, filtered and sorted. */
export class NearbyOffersQueryDto extends LocationQueryDto {
  @ValidateIf(present)
  @Type(() => Number)
  @IsNumber({ allowNaN: false, allowInfinity: false })
  @Min(NEARBY_LIMITS.minRadiusKm)
  @Max(NEARBY_LIMITS.maxRadiusKm)
  radiusKm?: number;

  /** Searched in the title, the description and the business name. */
  @ValidateIf(present)
  @Transform(({ value }: { value: unknown }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString()
  @Length(NEARBY_LIMITS.queryMin, NEARBY_LIMITS.queryMax)
  q?: string;

  @ValidateIf(present)
  @IsEnum(OfferCategory)
  category?: OfferCategory;

  /** distance (nearest first, the default), price (cheapest), discount (largest), pickup (ends soonest). */
  @ValidateIf(present)
  @IsIn(NEARBY_SORTS)
  sort?: NearbySort;

  @ValidateIf(present)
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(NEARBY_LIMITS.maxLimit)
  limit?: number;
}

/** GET /offers/:id: the location is optional, only for the distance. */
export class OfferDetailQueryDto {
  @ValidateIf(present)
  @Type(() => Number)
  @IsNumber({ allowNaN: false, allowInfinity: false })
  @Min(-90)
  @Max(90)
  lat?: number;

  @ValidateIf(present)
  @Type(() => Number)
  @IsNumber({ allowNaN: false, allowInfinity: false })
  @Min(-180)
  @Max(180)
  lng?: number;
}
