import { Transform, Type } from 'class-transformer';
import {
  IsNumber,
  IsObject,
  IsString,
  Length,
  Max,
  MaxLength,
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';

export const PROFILE_LIMITS = {
  nameMin: 2,
  nameMax: 80,
  descriptionMax: 500,
  addressMin: 3,
  addressMax: 160,
} as const;

const trim = ({ value }: { value: unknown }) =>
  typeof value === 'string' ? value.trim() : value;
const present = (_object: unknown, value: unknown) => value !== undefined;

/** Where customers pick up the offers. Coordinates in decimal degrees. */
export class UpdateEstablishmentDto {
  @ValidateIf(present)
  @Transform(trim)
  @IsString()
  @Length(PROFILE_LIMITS.nameMin, PROFILE_LIMITS.nameMax)
  name?: string;

  @ValidateIf(present)
  @Transform(trim)
  @IsString()
  @Length(PROFILE_LIMITS.addressMin, PROFILE_LIMITS.addressMax)
  address?: string;

  @ValidateIf(present)
  @Transform(trim)
  @IsString()
  @Length(PROFILE_LIMITS.nameMin, PROFILE_LIMITS.nameMax)
  city?: string;

  @ValidateIf(present)
  @IsNumber({ allowNaN: false, allowInfinity: false })
  @Min(-90)
  @Max(90)
  latitude?: number;

  @ValidateIf(present)
  @IsNumber({ allowNaN: false, allowInfinity: false })
  @Min(-180)
  @Max(180)
  longitude?: number;
}

/** Any subset of the business's own data. `null` clears the description. */
export class UpdateBusinessProfileDto {
  @ValidateIf(present)
  @Transform(trim)
  @IsString()
  @Length(PROFILE_LIMITS.nameMin, PROFILE_LIMITS.nameMax)
  businessName?: string;

  @ValidateIf((_o, value: unknown) => value !== undefined && value !== null)
  @Transform(trim)
  @IsString()
  @MaxLength(PROFILE_LIMITS.descriptionMax)
  description?: string | null;

  @ValidateIf(present)
  @IsObject()
  @ValidateNested()
  @Type(() => UpdateEstablishmentDto)
  establishment?: UpdateEstablishmentDto;
}
