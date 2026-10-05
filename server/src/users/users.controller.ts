import { Body, Controller, Get, Patch, Post, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { SupabaseAuthGuard } from '../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { CreateUserProfileDto } from './dto/create-user-profile.dto';
import { UpdateUserProfileDto } from './dto/update-user-profile.dto';
import { UsersService } from './users.service';

@Controller('users/me')
@UseGuards(SupabaseAuthGuard)
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post()
  createCurrentProfile(
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: CreateUserProfileDto,
  ) {
    return this.usersService.createCurrentProfile(user, dto);
  }

  @Get()
  getCurrentProfile(@CurrentUser() user: AuthenticatedUser) {
    return this.usersService.getCurrentProfile(user);
  }

  @Patch()
  updateCurrentProfile(
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: UpdateUserProfileDto,
  ) {
    return this.usersService.updateCurrentProfile(user, dto);
  }
}
