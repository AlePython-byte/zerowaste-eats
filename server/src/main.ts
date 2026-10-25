import { BadRequestException, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import { AppModule, ObserveInstrument } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    instrument: ObserveInstrument,
  });
  app.setGlobalPrefix('api/v1');
  const corsOrigins = app
    .get(ConfigService)
    .getOrThrow<string[]>('CORS_ORIGINS');
  // The mobile app does not need CORS; browsers get it only for listed origins
  if (corsOrigins.length > 0) {
    app.enableCors({ origin: corsOrigins });
  }
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      exceptionFactory: () =>
        new BadRequestException(
          'Los datos enviados no son válidos.',
          'Solicitud inválida',
        ),
    }),
  );
  app.enableShutdownHooks();
  await app.listen(app.get(ConfigService).getOrThrow<number>('PORT'));
}
bootstrap();
