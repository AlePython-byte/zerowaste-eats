import type { Response } from 'express';
import {
  authenticatedUser,
  publicUserProfile,
} from '../../test/fixtures/user-profile';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';

describe('UsersController', () => {
  const service = {
    createCurrentProfile: jest.fn(),
    getCurrentProfile: jest.fn(),
    updateCurrentProfile: jest.fn(),
  };
  const controller = new UsersController(service as unknown as UsersService);

  beforeEach(() => {
    for (const method of Object.values(service))
      method.mockReset().mockResolvedValue(publicUserProfile);
  });

  it.each([
    [true, 201],
    [false, 200],
  ])(
    'passes identity and DTO to the service; created=%s answers %i',
    async (created, status) => {
      service.createCurrentProfile.mockResolvedValue({
        profile: publicUserProfile,
        created,
      });
      const response = { status: jest.fn() };
      const dto = { displayName: 'Alejandro', city: 'Pasto' };
      await expect(
        controller.createCurrentProfile(
          authenticatedUser,
          dto,
          response as unknown as Response,
        ),
      ).resolves.toEqual(publicUserProfile);
      expect(service.createCurrentProfile).toHaveBeenCalledWith(
        authenticatedUser,
        dto,
      );
      expect(response.status).toHaveBeenCalledWith(status);
    },
  );

  it('retrieves the current identity without another user selector', async () => {
    await expect(
      controller.getCurrentProfile(authenticatedUser),
    ).resolves.toEqual(publicUserProfile);
    expect(service.getCurrentProfile).toHaveBeenCalledWith(authenticatedUser);
  });

  it('passes the authenticated identity and allowed update to the service', async () => {
    const dto = { city: 'Pasto' };
    await expect(
      controller.updateCurrentProfile(authenticatedUser, dto),
    ).resolves.toEqual(publicUserProfile);
    expect(service.updateCurrentProfile).toHaveBeenCalledWith(
      authenticatedUser,
      dto,
    );
  });
});
