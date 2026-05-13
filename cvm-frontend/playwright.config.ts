import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright-Konfiguration fuer die CVM-E2E-Smoke-Suite.
 *
 * Die Test-Suite faehrt headless Chromium gegen den lokal oder im CI
 * laufenden Stack (Backend 8081, Frontend 4200, Keycloak 8080) und
 * deckt den kritischen Pfad Login -> Dashboard ab.
 *
 * Stack-Startup uebernimmt der CI-Workflow (.github/workflows/ci.yml)
 * oder lokal `scripts/start.sh`. Diese Config startet NICHTS selbst,
 * damit sie sowohl in CI als auch lokal gegen einen schon laufenden
 * Stack einsetzbar ist.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 15_000,
  },
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never' }]]
    : [['list'], ['html']],
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
