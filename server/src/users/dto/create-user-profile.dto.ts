import { Transform } from 'class-transformer';
import { IsEnum, IsString, Length, ValidateIf } from 'class-validator';
import { UserRole } from '../../generated/prisma/enums';

export class CreateUserProfileDto {
  @Transform(({ value }: { value: unknown }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString({ message: 'El nombre debe ser texto.' })
  @Length(2, 80, { message: 'El nombre debe tener entre 2 y 80 caracteres.' })
  displayName: string;

  @ValidateIf((_object, value: unknown) => value !== undefined)
  @Transform(({ value }: { value: unknown }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString({ message: 'La ciudad debe ser texto.' })
  @Length(2, 80, { message: 'La ciudad debe tener entre 2 y 80 caracteres.' })
  city?: string;

  @ValidateIf((_object, value: unknown) => value !== undefined)
  @IsEnum(UserRole, { message: 'El rol debe ser CUSTOMER o MERCHANT.' })
  role?: UserRole;
}
