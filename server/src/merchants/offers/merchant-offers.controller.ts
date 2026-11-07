import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../../auth/decorators/current-user.decorator';
import { uuidOrNotFound } from '../../common/uuid-param';
import { Roles } from '../../auth/decorators/roles.decorator';
import { RolesGuard } from '../../auth/guards/roles.guard';
import { SupabaseAuthGuard } from '../../auth/guards/supabase-auth.guard';
import type { AuthenticatedUser } from '../../auth/interfaces/authenticated-user.interface';
import { UserRole } from '../../generated/prisma/enums';
import {
  CreateMerchantOfferDto,
  ListMerchantOffersQueryDto,
  UpdateMerchantOfferDto,
} from './dto/merchant-offer.dto';
import { MerchantOffersService } from './merchant-offers.service';

/** An id that is not a UUID is answered like any offer that is not the caller's. */
const offerIdPipe = uuidOrNotFound('No encontramos esa oferta.');

@Controller('merchant/offers')
@UseGuards(SupabaseAuthGuard, RolesGuard)
@Roles(UserRole.MERCHANT)
export class MerchantOffersController {
  constructor(private readonly offers: MerchantOffersService) {}

  @Get()
  list(
    @CurrentUser() user: AuthenticatedUser,
    @Query() query: ListMerchantOffersQueryDto,
  ) {
    return this.offers.list(user.id, query.status);
  }

  @Get(':id')
  get(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id', offerIdPipe) id: string,
  ) {
    return this.offers.get(user.id, id);
  }

  @Post()
  create(
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: CreateMerchantOfferDto,
  ) {
    return this.offers.create(user.id, dto);
  }

  @Patch(':id')
  update(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id', offerIdPipe) id: string,
    @Body() dto: UpdateMerchantOfferDto,
  ) {
    return this.offers.update(user.id, id, dto);
  }

  @Post(':id/pause')
  @HttpCode(HttpStatus.OK)
  pause(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id', offerIdPipe) id: string,
  ) {
    return this.offers.pause(user.id, id);
  }

  @Post(':id/resume')
  @HttpCode(HttpStatus.OK)
  resume(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id', offerIdPipe) id: string,
  ) {
    return this.offers.resume(user.id, id);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id', offerIdPipe) id: string,
  ) {
    return this.offers.remove(user.id, id);
  }
}
