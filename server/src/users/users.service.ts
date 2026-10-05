import {
  ConflictException,
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
import type { CreateUserProfileDto } from './dto/create-user-profile.dto';
import type { UpdateUserProfileDto } from './dto/update-user-profile.dto';
import { presentUserProfile } from './presenters/user-profile.presenter';

@Injectable()
export class UsersService {
  constructor(private readonly databaseService: DatabaseService) {}

  async createCurrentProfile(
    identity: AuthenticatedUser,
    dto: CreateUserProfileDto,
  ) {
    if (typeof identity.email !== 'string' || !isEmail(identity.email)) {
      throw new UnprocessableEntityException(
        'La identidad autenticada no contiene un correo electrónico válido.',
        'Datos no procesables',
      );
    }

    try {
      const user = await this.databaseService.client.user.create({
        data: {
          id: identity.id,
          email: identity.email,
          displayName: dto.displayName,
          city: dto.city ?? null,
          role: dto.role ?? UserRole.CUSTOMER,
        },
      });
      return presentUserProfile(user);
    } catch (error) {
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2002'
      ) {
        throw new ConflictException(
          'Ya existe un perfil con estos datos.',
          'Conflicto',
        );
      }
      throw new InternalServerErrorException(
        'No fue posible crear el perfil.',
        'Error interno',
      );
    }
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
