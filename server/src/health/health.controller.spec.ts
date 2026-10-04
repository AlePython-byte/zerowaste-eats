import { Test, TestingModule } from '@nestjs/testing';
import { ServiceUnavailableException } from '@nestjs/common';
import { DatabaseService } from '../database/database.service';
import { HealthController } from './health.controller';
import { HealthModule } from './health.module';

describe('HealthController', () => {
  let module: TestingModule;
  let healthController: HealthController;
  const databaseService = { checkConnection: jest.fn<Promise<boolean>, []>() };

  beforeEach(async () => {
    databaseService.checkConnection.mockReset().mockResolvedValue(true);
    module = await Test.createTestingModule({
      imports: [HealthModule],
    })
      .overrideProvider(DatabaseService)
      .useValue(databaseService)
      .compile();

    healthController = module.get<HealthController>(HealthController);
  });

  afterEach(async () => {
    await module.close();
  });

  it('should return the deterministic API health response', () => {
    expect(healthController.getHealth()).toEqual({
      estado: 'ok',
      servicio: 'ZeroWaste Eats API',
      version: '1.0.0',
    });
  });

  it('should report successful database connectivity', async () => {
    await expect(healthController.getDatabaseHealth()).resolves.toEqual({
      estado: 'ok',
      baseDatos: 'conectada',
    });
    expect(databaseService.checkConnection).toHaveBeenCalledTimes(1);
  });

  it('should report database unavailability with a safe Spanish response', async () => {
    databaseService.checkConnection.mockResolvedValue(false);
    const response = healthController.getDatabaseHealth();
    await expect(response).rejects.toBeInstanceOf(ServiceUnavailableException);
    await expect(response).rejects.toMatchObject({
      status: 503,
      response: {
        estado: 'error',
        baseDatos: 'desconectada',
        mensaje: 'La base de datos no está disponible.',
      },
    });
  });
});
