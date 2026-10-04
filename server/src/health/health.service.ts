import { Injectable } from '@nestjs/common';

@Injectable()
export class HealthService {
  getHealth() {
    return {
      estado: 'ok',
      servicio: 'ZeroWaste Eats API',
      version: '1.0.0',
    };
  }
}
