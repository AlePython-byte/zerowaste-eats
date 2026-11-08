import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/**
 * Guards the Render Blueprint at the repository root: secrets are never written in it, the
 * service starts by applying migrations, and the health check points at a real route.
 */
describe('render.yaml', () => {
  // Tests run from server/, the Blueprint lives one level up
  const blueprint = readFileSync(resolve(process.cwd(), '..', 'render.yaml'), 'utf8');

  /** Each `- key:` entry of envVars with the lines that belong to it. */
  const envVars = (): Map<string, string> => {
    const entries = new Map<string, string>();
    const parts = blueprint.split(/^\s*- key:\s*/m).slice(1);
    for (const part of parts) {
      const [key, ...rest] = part.split('\n');
      const body: string[] = [];
      for (const line of rest) {
        if (/^\s*- /.test(line) || /^\S/.test(line)) break;
        body.push(line);
      }
      entries.set(key.trim(), body.join('\n'));
    }
    return entries;
  };

  const secrets = [
    'DATABASE_URL',
    'SUPABASE_URL',
    'SUPABASE_PUBLISHABLE_KEY',
    'ANTHROPIC_API_KEY',
  ];

  it.each(secrets)('asks Render for %s instead of holding its value', (key) => {
    const entry = envVars().get(key);
    expect(entry).toBeDefined();
    expect(entry).toMatch(/sync:\s*false/);
    expect(entry).not.toMatch(/value:/);
  });

  it('holds no connection strings or keys anywhere', () => {
    expect(blueprint).not.toMatch(/postgres(ql)?:\/\//);
    expect(blueprint).not.toMatch(/sk-ant-/);
    expect(blueprint).not.toMatch(/sb_(publishable|secret)_/);
    expect(blueprint).not.toMatch(/https:\/\/[a-z0-9]+\.supabase\.co/);
  });

  it('runs in production with the other settings in plain sight', () => {
    const vars = envVars();
    expect(vars.get('NODE_ENV')).toMatch(/value:\s*production/);
    expect(vars.get('AI_MODEL')).toMatch(/value:\s*\S+/);
    expect(vars.get('AI_DAILY_LIMIT')).toMatch(/value:\s*"?\d+"?/);
  });

  it('builds the server folder and migrates before starting', () => {
    expect(blueprint).toMatch(/rootDir:\s*server/);
    expect(blueprint).toMatch(/buildCommand:\s*npm ci\b.*&& npm run build/);
    expect(blueprint).toMatch(
      /startCommand:\s*npx prisma migrate deploy && npm run start:prod/,
    );
  });

  it('checks the health route the API serves', () => {
    const controller = readFileSync(
      resolve(process.cwd(), 'src', 'health', 'health.controller.ts'),
      'utf8',
    );
    const route = /@Controller\('([^']+)'\)/.exec(controller)?.[1];
    expect(blueprint).toMatch(
      new RegExp(`healthCheckPath:\\s*/api/v1/${route}\\s*$`, 'm'),
    );
  });
});
