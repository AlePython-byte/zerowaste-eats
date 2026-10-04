import { ConfigService } from '@nestjs/config';
import { DatabaseService } from './database.service';

describe('DatabaseService', () => {
  let service: DatabaseService;

  beforeEach(() => {
    service = new DatabaseService(
      new ConfigService({
        DATABASE_URL: 'postgresql://test:test@127.0.0.1:5432/test',
      }),
    );
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('connects and disconnects the same client through lifecycle hooks', async () => {
    const connect = jest.spyOn(service.client, '$connect').mockResolvedValue();
    const disconnect = jest
      .spyOn(service.client, '$disconnect')
      .mockResolvedValue();
    await service.onModuleInit();
    await service.onModuleDestroy();
    expect(connect).toHaveBeenCalledTimes(1);
    expect(disconnect).toHaveBeenCalledTimes(1);
  });

  it('checks connectivity with a static SELECT 1 query', async () => {
    const query = jest
      .spyOn(service.client, '$queryRaw')
      .mockResolvedValue([{ result: 1 }]);
    await expect(service.checkConnection()).resolves.toBe(true);
    expect(query).toHaveBeenCalledWith(['SELECT 1']);
  });

  it('returns false without exposing database errors', async () => {
    jest
      .spyOn(service.client, '$queryRaw')
      .mockRejectedValue(new Error('private-database-details'));
    await expect(service.checkConnection()).resolves.toBe(false);
  });

  it('sanitizes connection failures at startup', async () => {
    jest
      .spyOn(service.client, '$connect')
      .mockRejectedValue(new Error('private-database-details'));
    await expect(service.onModuleInit()).rejects.toThrow(
      'Database connection failed. Check the database configuration and availability.',
    );
  });

  it('sanitizes disconnection failures at shutdown', async () => {
    jest
      .spyOn(service.client, '$disconnect')
      .mockRejectedValue(new Error('private-database-details'));
    await expect(service.onModuleDestroy()).rejects.toThrow(
      'Database disconnection failed.',
    );
  });
});
