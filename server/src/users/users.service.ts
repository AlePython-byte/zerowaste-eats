import {
  ConflictException,
  HttpException,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
  UnprocessableEntityException,
} from '@nestjs/common';
import { isEmail } from 'class-validator';
import type { AuthenticatedUser } from '../auth/interfaces/authenticated-user.interface';
import { DatabaseService } from '../database/database.service';
import { Prisma, UserRole } from '../generated/prisma/client';
import type { User } from '../generated/prisma/client';
import { DEFAULT_ESTABLISHMENT_LOCATION } from '../merchants/default-establishment-location';
import type { CreateUserProfileDto } from './dto/create-user-profile.dto';
import type { UpdateUserProfileDto } from './dto/update-user-profile.dto';
import { presentUserProfile } from './presenters/user-profile.presenter';

export interface CreatedProfile {
  profile: ReturnType<typeof presentUserProfile>;
  /** False when the profile already existed and was returned unchanged. */
  created: boolean;
}

@Injectable()
export class UsersService {
  constructor(private readonly databaseService: DatabaseService) {}

  /**
   * Creates the caller's profile, and for a merchant its merchant profile and first
   * establishment, all in one transaction. Repeating the request returns the existing
   * profile, so a client can retry safely; the role chosen first can never change.
   */
  async createCurrentProfile(
    identity: AuthenticatedUser,
    dto: CreateUserProfileDto,
  ): Promise<CreatedProfile> {
    if (typeof identity.email !== 'string' || !isEmail(identity.email)) {
      throw new UnprocessableEntityException(
        'La identidad autenticada no contiene un correo electrónico válido.',
        'Datos no procesables',
      );
    }
    const email = identity.email;

    try {
      return await this.databaseService.client.$transaction(async (tx) => {
        const existing = await tx.user.findUnique({
          where: { id: identity.id },
          include: { merchantProfile: { select: { id: true } } },
        });
        if (existing) {
          assertSameRole(existing, dto);
          // A merchant left without its profile (e.g. created before this flow) is completed
          if (
            existing.role === UserRole.MERCHANT &&
            !existing.merchantProfile
          ) {
            await createMerchantRecords(tx, existing, dto);
          }
          return { profile: presentUserProfile(existing), created: false };
        }

        const user = await tx.user.create({
          data: {
            id: identity.id,
            email,
            displayName: dto.displayName,
            city: dto.city ?? null,
            role: dto.role ?? UserRole.CUSTOMER,
          },
        });
        if (user.role === UserRole.MERCHANT) {
          await createMerchantRecords(tx, user, dto);
        }
        return { profile: presentUserProfile(user), created: true };
      });
    } catch (error) {
      if (error instanceof HttpException) {
        throw error;
      }
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2002'
      ) {
        return this.resolveConcurrentCreation(identity, dto);
      }
      throw new InternalServerErrorException(
        'No fue posible crear el perfil.',
        'Error interno',
      );
    }
  }

  /**
   * A uniqueness conflict means a parallel request for this identity won the race, or the
   * email belongs to another account. The first case is answered with the winner's profile.
   */
  private async resolveConcurrentCreation(
    identity: AuthenticatedUser,
    dto: CreateUserProfileDto,
  ): Promise<CreatedProfile> {
    let existing: User | null;
    try {
      existing = await this.databaseService.client.user.findUnique({
        where: { id: identity.id },
      });
    } catch {
      throw new InternalServerErrorException(
        'No fue posible crear el perfil.',
        'Error interno',
      );
    }
    if (!existing) {
      throw new ConflictException(
        'Ya existe un perfil con estos datos.',
        'Conflicto',
      );
    }
    assertSameRole(existing, dto);
    return { profile: presentUserProfile(existing), created: false };
  }

  async getCurrentProfile(identity: AuthenticatedUser) {
    let user: User | null;
    try {
      user = await this.databaseService.client.user.findUnique({
        where: { id: identity.id },
      });
    } catch {
      throw new InternalServerErrorException(
        'No fue posible consultar el perfil.',
        'Error interno',
      );
    }
    if (!user) {
      throw new NotFoundException('No se encontró tu perfil.', 'No encontrado');
    }
    return presentUserProfile(user);
  }

  async updateCurrentProfile(
    identity: AuthenticatedUser,
    dto: UpdateUserProfileDto,
  ) {
    try {
      const user = await this.databaseService.client.user.update({
        where: { id: identity.id },
        data: {
          ...(dto.displayName !== undefined
            ? { displayName: dto.displayName }
            : {}),
          ...(dto.city !== undefined ? { city: dto.city } : {}),
        },
      });
      return presentUserProfile(user);
    } catch (error) {
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2025'
      ) {
        throw new NotFoundException(
          'No se encontró tu perfil.',
          'No encontrado',
        );
      }
      throw new InternalServerErrorException(
        'No fue posible actualizar el perfil.',
        'Error interno',
      );
    }
  }
}

/** Retrying with the same role (or none) is fine; asking for another role is not. */
function assertSameRole(existing: User, dto: CreateUserProfileDto): void {
  if (dto.role !== undefined && dto.role !== existing.role) {
    throw new ConflictException(
      'Tu perfil ya existe con otro rol y el rol no se puede cambiar.',
      'Conflicto',
    );
  }
}

async function createMerchantRecords(
  tx: Prisma.TransactionClient,
  user: User,
  dto: CreateUserProfileDto,
): Promise<void> {
  const businessName = dto.businessName ?? user.displayName;
  const merchantProfile = await tx.merchantProfile.create({
    data: { userId: user.id, businessName },
  });
  await tx.establishment.create({
    data: {
      merchantProfileId: merchantProfile.id,
      name: businessName,
      ...DEFAULT_ESTABLISHMENT_LOCATION,
    },
  });
}
