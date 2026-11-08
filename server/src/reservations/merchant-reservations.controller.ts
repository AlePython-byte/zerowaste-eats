import {
  Body,
  Controller,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  Post,
  UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { uuidOrNotFound } from '../common/uuid-param';
import { UserRole } from '../generated/prisma/enums';
import { RedeemReservationDto } from './dto/redeem-reservation.dto';
import { MerchantReservationsService } from './merchant-reservations.service';

/** An id that is not a UUID is answered like any reservation that is not the caller's. */
const reservationIdPipe = uuidOrNotFound('No encontramos esa reserva.');

/** The reservations on a business's own offers. */
@Controller('merchant/reservations')
@UseGuards(SupabaseAuthGuard, RolesGuard)
@Roles(UserRole.MERCHANT)
export class MerchantReservationsController {
  constructor(private readonly reservations: MerchantReservationsService) {}

  @Get()
  list(@CurrentUser() user: AuthenticatedUser) {
    return this.reservations.list(user.id);
  }

  @Post(':id/redeem')
  @HttpCode(HttpStatus.OK)
  redeem(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id', reservationIdPipe) id: string,
    @Body() dto: RedeemReservationDto,
  ) {
    return this.reservations.redeem(user.id, id, dto.code);
  }
}
