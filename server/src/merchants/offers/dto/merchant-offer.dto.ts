import { Transform } from 'class-transformer';
import {
  IsEnum,
  IsISO8601,
  IsInt,
  IsString,
  IsUUID,
  Length,
  Max,
  MaxLength,
  Min,
  ValidateIf,
} from 'class-validator';
import { OfferCategory, OfferStatus } from '../../../generated/prisma/enums';

export const OFFER_LIMITS = {
  titleMin: 3,
  titleMax: 80,
  descriptionMax: 240,
  imageUrlMax: 500,
  priceMin: 100,
  priceMax: 10_000_000,
  quantityMin: 1,
  quantityMax: 99,
  /** How far ahead a pickup may end, in days. */
  pickupDaysAhead: 7,
} as const;

const trim = ({ value }: { value: unknown }) =>
  typeof value === 'string' ? value.trim() : value;

const present = (_object: unknown, value: unknown) => value !== undefined;

/** A new offer. Prices are whole Colombian pesos; times are ISO 8601 with a time zone. */
export class CreateMerchantOfferDto {
  @Transform(trim)
  @IsString()
  @Length(OFFER_LIMITS.titleMin, OFFER_LIMITS.titleMax)
  title: string;

  @ValidateIf(present)
  @Transform(trim)
  @IsString()
  @MaxLength(OFFER_LIMITS.descriptionMax)
  description?: string;

  @IsEnum(OfferCategory)
  category: OfferCategory;

  /** A photo in the business's own folder of the offer-photos bucket. */
  @ValidateIf(present)
  @IsString()
  @MaxLength(OFFER_LIMITS.imageUrlMax)
  imageUrl?: string;

  @IsInt()
  @Min(OFFER_LIMITS.priceMin)
  @Max(OFFER_LIMITS.priceMax)
  originalPrice: number;

  @IsInt()
  @Min(OFFER_LIMITS.priceMin)
  @Max(OFFER_LIMITS.priceMax)
  discountedPrice: number;

  @IsInt()
  @Min(OFFER_LIMITS.quantityMin)
  @Max(OFFER_LIMITS.quantityMax)
  quantityTotal: number;

  @IsISO8601({ strict: true, strictSeparator: true })
  pickupStart: string;

  @IsISO8601({ strict: true, strictSeparator: true })
  pickupEnd: string;

  /** Defaults to the business's first establishment. */
  @ValidateIf(present)
  @IsUUID()
  establishmentId?: string;
}

/** Changes to an offer: any subset of the fields. `null` clears the description or photo. */
export class UpdateMerchantOfferDto {
  @ValidateIf(present)
  @Transform(trim)
  @IsString()
  @Length(OFFER_LIMITS.titleMin, OFFER_LIMITS.titleMax)
  title?: string;

  @ValidateIf((_o, value: unknown) => value !== undefined && value !== null)
  @Transform(trim)
  @IsString()
  @MaxLength(OFFER_LIMITS.descriptionMax)
  description?: string | null;

  @ValidateIf(present)
  @IsEnum(OfferCategory)
  category?: OfferCategory;

  @ValidateIf((_o, value: unknown) => value !== undefined && value !== null)
  @IsString()
  @MaxLength(OFFER_LIMITS.imageUrlMax)
  imageUrl?: string | null;

  @ValidateIf(present)
  @IsInt()
  @Min(OFFER_LIMITS.priceMin)
  @Max(OFFER_LIMITS.priceMax)
  originalPrice?: number;

  @ValidateIf(present)
  @IsInt()
  @Min(OFFER_LIMITS.priceMin)
  @Max(OFFER_LIMITS.priceMax)
  discountedPrice?: number;

  @ValidateIf(present)
  @IsInt()
  @Min(OFFER_LIMITS.quantityMin)
  @Max(OFFER_LIMITS.quantityMax)
  quantityTotal?: number;

  @ValidateIf(present)
  @IsISO8601({ strict: true, strictSeparator: true })
  pickupStart?: string;

  @ValidateIf(present)
  @IsISO8601({ strict: true, strictSeparator: true })
  pickupEnd?: string;
}

/** GET /merchant/offers?status=… */
export class ListMerchantOffersQueryDto {
  @ValidateIf(present)
  @IsEnum(OfferStatus)
  status?: OfferStatus;
}
