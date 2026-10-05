import { Transform } from 'class-transformer';
import { IsDefined, IsString, Length, ValidateIf } from 'class-validator';

export class UpdateUserProfileDto {
  // Require displayName when neither allowed field is supplied.
  @ValidateIf(
    (object: UpdateUserProfileDto, value: unknown) =>
      value !== undefined || object.city === undefined,
  )
  @IsDefined({ message: 'Debes enviar al menos un campo: displayName o city.' })
  @Transform(({ value }: { value: unknown }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString({ message: 'El nombre debe ser texto.' })
  @Length(2, 80, { message: 'El nombre debe tener entre 2 y 80 caracteres.' })
  displayName?: string;

  @ValidateIf((_object, value: unknown) => value !== undefined)
  @Transform(({ value }: { value: unknown }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString({ message: 'La ciudad debe ser texto.' })
  @Length(2, 80, { message: 'La ciudad debe tener entre 2 y 80 caracteres.' })
  city?: string;
}
