import { Transform } from 'class-transformer';
import {
  IsEnum,
  IsString,
  Length,
  ValidateBy,
  ValidateIf,
} from 'class-validator';
import { UserRole } from '../../generated/prisma/enums';

const trim = ({ value }: { value: unknown }) =>
  typeof value === 'string' ? value.trim() : value;

/** A business name only makes sense for a merchant account. */
function OnlyForMerchants(): PropertyDecorator {
  return ValidateBy({
    name: 'onlyForMerchants',
    validator: {
      validate: (_value, args) =>
        (args?.object as CreateUserProfileDto | undefined)?.role ===
        UserRole.MERCHANT,
      defaultMessage: () =>
        'El nombre del negocio solo aplica a cuentas de comercio.',
    },
  });
}

export class CreateUserProfileDto {
  @Transform(trim)
  @IsString({ message: 'El nombre debe ser texto.' })
  @Length(2, 80, { message: 'El nombre debe tener entre 2 y 80 caracteres.' })
  displayName: string;

  @ValidateIf((_object, value: unknown) => value !== undefined)
  @Transform(trim)
  @IsString({ message: 'La ciudad debe ser texto.' })
  @Length(2, 80, { message: 'La ciudad debe tener entre 2 y 80 caracteres.' })
  city?: string;

  @ValidateIf((_object, value: unknown) => value !== undefined)
  @IsEnum(UserRole, { message: 'El rol debe ser CUSTOMER o MERCHANT.' })
  role?: UserRole;

  /** Merchants only; defaults to the display name. */
  @ValidateIf((_object, value: unknown) => value !== undefined)
  @Transform(trim)
  @OnlyForMerchants()
  @IsString({ message: 'El nombre del negocio debe ser texto.' })
  @Length(2, 80, {
    message: 'El nombre del negocio debe tener entre 2 y 80 caracteres.',
  })
  businessName?: string;
}
