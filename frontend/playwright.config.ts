import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests run against the full stack started by `docker compose up` (PostgreSQL, Kafka, backend and the
 * federated frontend), so they exercise the real REST calls and the real Kafka projection.
 *
 * Tests run one at a time: the exposure test measures a change in a shared total, which a parallel test could move.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 15_000 },
  reporter: [['list']],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
