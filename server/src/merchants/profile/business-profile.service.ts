import {
  BadRequestException,
  HttpException,
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from '@nestjs/common';
import { DatabaseService } from '../../database/database.service';
import type {
  Establishment,
  MerchantProfile,
} from '../../generated/prisma/client';
import { DEFAULT_ESTABLISHMENT_LOCATION } from '../default-establishment-location';
import type { UpdateBusinessProfileDto } from './dto/update-business-profile.dto';

/** Stored with 6 decimals (about 11 cm): more would only be noise. */
const COORDINATE_DECIMALS = 6;

const toNumber = (value: { toNumber(): number } | number | string): number =>
  typeof value === 'object' ? value.toNumber() : Number(value);

const round = (value: number) => Number(value.toFixed(COORDINATE_DECIMALS));

export function presentBusinessProfile(
  profile: MerchantProfile,
  establishment: Establishment | null,
) {
  return {
    id: profile.id,
    businessName: profile.businessName,
    description: profile.description,
    isVerified: profile.isVerified,
    establishment: establishment && {
      id: establishment.id,
      name: establishment.name,
      address: establishment.address,
      city: establishment.city,
      latitude: toNumber(establishment.latitude),
      longitude: toNumber(establishment.longitude),
    },
  };
}

export type BusinessProfileView = ReturnType<typeof presentBusinessProfile>;

/**
 * The caller's own business: its name and description, and its establishment (the first
 * one, for now each business has one). Always looked up by the caller's id.
 */
@Injectable()
export class BusinessProfileService {
  constructor(private readonly databaseService: DatabaseService) {}

  async get(userId: string): Promise<BusinessProfileView> {
    return this.guard(async () => {
      const profile = await this.findProfile(userId);
      return presentBusinessProfile(
        profile,
        await this.firstEstablishment(profile.id),
      );
    });
  }

  /** Profile and establishment change together or not at all. */
  async update(
    userId: string,
    dto: UpdateBusinessProfileDto,
  ): Promise<BusinessProfileView> {
    const establishmentChanges = dto.establishment ?? {};
    const nothing =
      dto.businessName === undefined &&
      dto.description === undefined &&
      Object.values(establishmentChanges).every((v) => v === undefined);
    if (nothing) {
      throw new BadRequestException(
        'Envía al menos un dato para cambiar.',
        'Solicitud inválida',
      );
    }

    return this.guard(() =>
      this.databaseService.client.$transaction(async (tx) => {
        const profile = await tx.merchantProfile.findUnique({
          where: { userId },
        });
        if (!profile) throw notFound();

        const updatedProfile =
          dto.businessName !== undefined || dto.description !== undefined
            ? await tx.merchantProfile.update({
                where: { id: profile.id },
                data: {
                  ...(dto.businessName !== undefined
                    ? { businessName: dto.businessName.trim() }
                    : {}),
                  ...(dto.description !== undefined
                    ? { description: dto.description?.trim() || null }
                    : {}),
                },
              })
            : profile;

        const data = {
          ...(establishmentChanges.name !== undefined
            ? { name: establishmentChanges.name.trim() }
            : {}),
          ...(establishmentChanges.address !== undefined
            ? { address: establishmentChanges.address.trim() }
            : {}),
          ...(establishmentChanges.city !== undefined
            ? { city: establishmentChanges.city.trim() }
            : {}),
          ...(establishmentChanges.latitude !== undefined
            ? { latitude: round(establishmentChanges.latitude) }
            : {}),
          ...(establishmentChanges.longitude !== undefined
            ? { longitude: round(establishmentChanges.longitude) }
            : {}),
        };
        let establishment = await tx.establishment.findFirst({
          where: { merchantProfileId: profile.id },
          orderBy: { createdAt: 'asc' },
        });
        if (Object.keys(data).length > 0) {
          establishment = establishment
            ? await tx.establishment.update({
                where: { id: establishment.id },
                data,
              })
            : // A business left without one gets it now, from the defaults
              await tx.establishment.create({
                data: {
                  merchantProfileId: profile.id,
                  name: updatedProfile.businessName,
                  ...DEFAULT_ESTABLISHMENT_LOCATION,
                  ...data,
                },
              });
        }
        return presentBusinessProfile(updatedProfile, establishment);
      }),
    );
  }

  private async findProfile(userId: string): Promise<MerchantProfile> {
    const profile =
      await this.databaseService.client.merchantProfile.findUnique({
        where: { userId },
      });
    if (!profile) throw notFound();
    return profile;
  }

  private firstEstablishment(
    merchantProfileId: string,
  ): Promise<Establishment | null> {
    return this.databaseService.client.establishment.findFirst({
      where: { merchantProfileId },
      orderBy: { createdAt: 'asc' },
    });
  }

  private async guard<T>(operation: () => Promise<T>): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      if (error instanceof HttpException) throw error;
      throw new InternalServerErrorException(
        'No fue posible completar la operación con tu negocio.',
        'Error interno',
      );
    }
  }
}

function notFound(): NotFoundException {
  return new NotFoundException(
    'Completa el perfil de tu negocio para continuar.',
    'No encontrado',
  );
}
