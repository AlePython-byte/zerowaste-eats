import { IsString, MaxLength, MinLength } from 'class-validator';

/** The pickup code the customer shows, as the business typed it. */
export class RedeemReservationDto {
  @IsString()
  @MinLength(1)
  @MaxLength(16)
  code: string;
}
