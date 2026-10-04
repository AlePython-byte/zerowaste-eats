import { Injectable, ServiceUnavailableException } from '@nestjs/common';
import { DatabaseService } from '../database/database.service';

@Injectable()
export class HealthService {
  constructor(private readonly databaseService: DatabaseService) {}

  getHealth() {
    return {
      estado: 'ok',
      servicio: 'ZeroWaste Eats API',
      version: '1.0.0',
    };
  }

  async getDatabaseHealth() {
    if (!(await this.databaseService.checkConnection())) {
      throw new ServiceUnavailableException({
        estado: 'error',
        baseDatos: 'desconectada',
        mensaje: 'La base de datos no está disponible.',
      });
    }

    return { estado: 'ok', baseDatos: 'conectada' };
  }
}
