import { expect, test, type Page } from '@playwright/test';

/**
 * Smoke-Test: Login -> Dashboard.
 *
 * Faehrt eine geschuetzte Route an, durchlaeuft den Keycloak-Login mit
 * dem Demo-Admin (`a.admin@ahs.test`, Realm `cvm-local`) und verifiziert,
 * dass das Dashboard im authentifizierten Zustand laedt.
 *
 * Erwartet eine laufende Umgebung:
 *   Frontend:  E2E_BASE_URL (default http://localhost:4200)
 *   Keycloak:  E2E_KEYCLOAK_URL (default http://localhost:8080)
 *   Backend:   ueber das Frontend angebunden, kein direkter Aufruf.
 *
 * Credentials kommen aus Env, damit das CI-Profil sie ueberschreiben kann:
 *   E2E_ADMIN_USER (default a.admin@ahs.test)
 *   E2E_ADMIN_PASS (default admin)
 */

const FRONTEND_URL = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';
const KEYCLOAK_URL = process.env['E2E_KEYCLOAK_URL'] ?? 'http://localhost:8080';
const ADMIN_USER = process.env['E2E_ADMIN_USER'] ?? 'a.admin@ahs.test';
const ADMIN_PASS = process.env['E2E_ADMIN_PASS'] ?? 'admin';

const frontendHost = new URL(FRONTEND_URL).host;
const keycloakHost = new URL(KEYCLOAK_URL).host;

async function loginViaKeycloak(page: Page): Promise<void> {
  // /queue ist eine geschuetzte Route -> Keycloak-Redirect erzwingen.
  await page.goto('/queue');
  await page.waitForURL(
    (url) => url.host === keycloakHost,
    { timeout: 30_000 },
  );

  await page.locator('input[name="username"]').fill(ADMIN_USER);
  await page.locator('input[name="password"]').fill(ADMIN_PASS);
  await Promise.all([
    page.waitForURL((url) => url.host === frontendHost, { timeout: 30_000 }),
    page.locator('input[type="submit"], button[type="submit"]').first().click(),
  ]);
}

test.describe('Smoke', () => {
  test('Login als Admin und Dashboard wird angezeigt', async ({ page }) => {
    await loginViaKeycloak(page);

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/dashboard$/);

    const dashboardHeading = page.getByRole('heading', {
      name: 'Dashboard',
      level: 1,
    });
    await expect(dashboardHeading).toBeVisible();

    // Authenticated-only: die Action-Karten unter dem Header zeigen,
    // dass die Rollen geladen wurden (Admin sieht "Neuer Scan").
    await expect(page.locator('[data-testid="dashboard-actions"]')).toBeVisible();
  });
});
