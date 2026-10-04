import { Injectable, OnModuleDestroy, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient } from '../generated/prisma/client';

@Injectable()
export class DatabaseService implements OnModuleInit, OnModuleDestroy {
  readonly client: PrismaClient;

  constructor(configService: ConfigService) {
    try {
      const adapter = new PrismaPg({
        connectionString: configService.getOrThrow<string>('DATABASE_URL'),
        connectionTimeoutMillis: 5000,
        query_timeout: 5000,
      });
      this.client = new PrismaClient({
        adapter,
        log: [],
        errorFormat: 'minimal',
      });
    } catch {
      throw new Error(
        'Database initialization failed. Check the database configuration.',
      );
    }
  }

  async onModuleInit(): Promise<void> {
    try {
      await this.client.$connect();
    } catch {
      throw new Error(
        'Database connection failed. Check the database configuration and availability.',
      );
    }
  }

  async onModuleDestroy(): Promise<void> {
    try {
      await this.client.$disconnect();
    } catch {
      throw new Error('Database disconnection failed.');
    }
  }

  async checkConnection(): Promise<boolean> {
    try {
      await this.client.$queryRaw`SELECT 1`;
      return true;
    } catch {
      return false;
    }
  }
}
