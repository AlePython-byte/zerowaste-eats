import {
  Body,
  Controller,
  Get,
  HttpStatus,
  Patch,
  Post,
  Res,
  UseGuards,
} from '@nestjs/common';
import type { Response } from 'express';
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

  /** 201 when the profile is created, 200 when it already existed (safe retry). */
  @Post()
  async createCurrentProfile(
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: CreateUserProfileDto,
    @Res({ passthrough: true }) response: Response,
  ) {
    const { profile, created } = await this.usersService.createCurrentProfile(
      user,
      dto,
    );
    response.status(created ? HttpStatus.CREATED : HttpStatus.OK);
    return profile;
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
