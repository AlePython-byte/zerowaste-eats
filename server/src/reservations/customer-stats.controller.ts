import { Controller, Get, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { UserRole } from '../generated/prisma/enums';
import { CustomerStatsService } from './customer-stats.service';

/** The signed-in customer's impact: orders picked up, money saved and food rescued. */
@Controller('users/me/stats')
@UseGuards(SupabaseAuthGuard, RolesGuard)
@Roles(UserRole.CUSTOMER)
export class CustomerStatsController {
  constructor(private readonly stats: CustomerStatsService) {}

  @Get()
  get(@CurrentUser() user: AuthenticatedUser) {
    return this.stats.forCustomer(user.id);
  }
}
