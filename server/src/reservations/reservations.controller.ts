import { Body, Controller, Get, Post, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { UserRole } from '../generated/prisma/enums';
import { CreateReservationDto } from './dto/create-reservation.dto';
import { ReservationsService } from './reservations.service';

/** A customer's own reservations. */
@Controller('reservations')
@UseGuards(SupabaseAuthGuard, RolesGuard)
@Roles(UserRole.CUSTOMER)
export class ReservationsController {
  constructor(private readonly reservations: ReservationsService) {}

  @Get()
  list(@CurrentUser() user: AuthenticatedUser) {
    return this.reservations.listForCustomer(user.id);
  }

  @Post()
  reserve(
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: CreateReservationDto,
  ) {
    return this.reservations.reserve(user.id, dto);
  }
}
