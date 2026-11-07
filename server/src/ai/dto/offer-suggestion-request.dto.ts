import { Transform } from 'class-transformer';
import { IsInt, Max, Min, ValidateIf } from 'class-validator';

/** The text fields that may come with the photo. Multipart fields arrive as text. */
export class OfferSuggestionRequestDto {
  /** Whole Colombian pesos, without separators. */
  @ValidateIf((_object, value: unknown) => value !== undefined)
  @Transform(({ value }: { value: unknown }) =>
    typeof value === 'string' && /^\s*\d{1,9}\s*$/.test(value)
      ? Number(value.trim())
      : value,
  )
  @IsInt({ message: 'El precio original debe ser un número entero de pesos.' })
  @Min(500, { message: 'El precio original debe ser de al menos 500 pesos.' })
  @Max(10_000_000, {
    message: 'El precio original no puede superar 10.000.000 de pesos.',
  })
  precioOriginal?: number;
}
