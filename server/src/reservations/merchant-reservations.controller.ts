import { Controller, Get, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { UserRole } from '../generated/prisma/enums';
import { MerchantReservationsService } from './merchant-reservations.service';

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
}
