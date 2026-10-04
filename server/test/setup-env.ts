// Tests must never load developer credentials or use an external database.
process.env.NODE_ENV = 'test';
process.env.PORT = '3000';
process.env.DATABASE_URL = 'postgresql://test:test@127.0.0.1:5432/test';
