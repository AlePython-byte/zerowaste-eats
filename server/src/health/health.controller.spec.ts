import { Test, TestingModule } from '@nestjs/testing';
import { HealthController } from './health.controller';
import { HealthModule } from './health.module';

describe('HealthController', () => {
  let module: TestingModule;
  let healthController: HealthController;

  beforeEach(async () => {
    module = await Test.createTestingModule({
      imports: [HealthModule],
    }).compile();

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
});
