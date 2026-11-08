import { SetMetadata } from '@nestjs/common';
import type { UserRole } from '../../generated/prisma/enums';

export const ROLES_KEY = 'roles';

/**
 * Restricts a route or controller to the given profile roles. Enforced by `RolesGuard`,
 * which must run after `SupabaseAuthGuard`: `@UseGuards(SupabaseAuthGuard, RolesGuard)`.
 */
export const Roles = (...roles: UserRole[]) => SetMetadata(ROLES_KEY, roles);
