/* eslint-disable no-console */
/**
 * UI-Exploration-Skript (Iteration 23C, CVM-60).
 *
 * Faehrt headless Chromium gegen die laufende CVM-Anwendung, loggt sich
 * als Admin in Keycloak ein, iteriert die in `routes.ts` gepflegten
 * Sidebar-Routen, sammelt pro Route:
 *   - HTTP-Status der Navigation
 *   - alle /api/-Requests (Methode, Pfad, Response-Status)
 *   - alle Browser-Konsolen-Fehler/Warnungen
 *   - DOM-Stichprobe (table/form/canvas/placeholder + Kinder im Main)
 *   - Screenshot als PNG
 *
 * Danach faellt pro Route ein Verdict (INHALT | PLATZHALTER | LEER |
 * FEHLER | NICHT_ERREICHBAR) und es werden zwei Reports erzeugt:
 *   docs/YYYYMMDD/ui-exploration-report.md
 *   docs/YYYYMMDD/ui-exploration.json
 *
 * Das Skript ist diagnostisch, nicht behauptend: es meldet fakten-basiert
 * "Route X hatte keine API-Calls", nicht "Route X ist kaputt". Die
 * Bewertung macht der Leser (Mensch oder Claude in spaeteren Sessions).
 *
 * Usage:
 *   CVM_TEST_ADMIN_PASS=admin npx tsx explore-ui.ts --target=local
 *   npx tsx explore-ui.ts --target=ci
 */

import { chromium, Browser, Page, Request, Response } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import * as path from 'node:path';
import { EXPLORE_ROUTES, SETTINGS_SECTIONS, ExploreRoute } from './routes.js';

type Verdict =
  | 'INHALT'
  | 'PLATZHALTER'
  | 'LEER'
  | 'FEHLER'
  | 'NICHT_ERREICHBAR';

interface ApiCall {
  readonly method: string;
  readonly url: string;
  readonly status: number | null;
}

interface ConsoleIssue {
  readonly type: 'error' | 'warning';
  readonly text: string;
}

interface DomSample {
  readonly mainChildren: number;
  readonly hasTable: boolean;
  readonly hasForm: boolean;
  readonly hasChart: boolean;
  readonly hasPlaceholder: boolean;
  readonly hasEmptyState: boolean;
  readonly headingText: string;
}

interface RouteOutcome {
  readonly path: string;
  readonly slug: string;
  readonly requiredRoleHint: string;
  readonly navigationStatus: number | null;
  readonly verdict: Verdict;
  readonly apiCalls: readonly ApiCall[];
  readonly consoleIssues: readonly ConsoleIssue[];
  readonly dom: DomSample | null;
  readonly screenshot: string;
  readonly note: string;
}

interface Config {
  readonly target: 'local' | 'ci';
  readonly frontendUrl: string;
  readonly keycloakUrl: string;
  readonly realm: string;
  readonly username: string;
  readonly password: string;
  readonly outputRoot: string;
  readonly dateStamp: string;
}

function parseArgs(): Config {
  const args = new Map<string, string>();
  for (const arg of process.argv.slice(2)) {
    const m = arg.match(/^--([^=]+)=(.*)$/);
    if (m) {
      args.set(m[1], m[2]);
    }
  }
  const target = (args.get('target') ?? 'local') as 'local' | 'ci';
  const dateStamp = yyyymmdd(new Date());
  const repoRoot = path.resolve(process.cwd(), '..', '..');
  const outputRoot =
    args.get('output-root') ?? path.join(repoRoot, 'docs', dateStamp);
  const frontendUrl =
    args.get('frontend-url') ??
    (target === 'ci' ? 'http://localhost:4200' : 'http://localhost:4200');
  const keycloakUrl =
    args.get('keycloak-url') ??
    (target === 'ci' ? 'http://localhost:8080' : 'http://localhost:8080');
  const realm = args.get('realm') ?? 'cvm-local';
  const username = args.get('user') ?? 'a.admin@ahs.test';
  const password = process.env.CVM_TEST_ADMIN_PASS ?? args.get('password') ?? '';

  if (!password) {
    throw new Error(
      'Passwort fehlt. Setze CVM_TEST_ADMIN_PASS oder uebergib --password=...'
    );
  }
  return {
    target,
    frontendUrl,
    keycloakUrl,
    realm,
    username,
    password,
    outputRoot,
    dateStamp
  };
}

function yyyymmdd(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}`;
}

async function login(page: Page, cfg: Config): Promise<void> {
  console.log('[login] Oeffne Shell, erwarte Keycloak-Redirect...');
  await page.goto(cfg.frontendUrl + '/queue', { waitUntil: 'domcontentloaded' });
  // Die Shell schickt via check-sso auf Keycloak. Warte auf das
  // username-Feld im Login-Formular.
  await page.waitForURL(
    (url) => url.hostname === new URL(cfg.keycloakUrl).hostname,
    { timeout: 20_000 }
  );
  await page.fill('input[name="username"]', cfg.username);
  await page.fill('input[name="password"]', cfg.password);
  await page.click('input[type="submit"], button[type="submit"]');
  await page.waitForURL(
    (url) => url.hostname === new URL(cfg.frontendUrl).hostname,
    { timeout: 20_000 }
  );
  console.log('[login] OK, zurueck im Frontend.');
}

async function exploreRoute(
  page: Page,
  route: ExploreRoute,
  cfg: Config
): Promise<RouteOutcome> {
  const apiCalls: ApiCall[] = [];
  const consoleIssues: ConsoleIssue[] = [];
  let navigationStatus: number | null = null;

  const requestListener = (req: Request) => {
    const url = req.url();
    if (url.includes('/api/')) {
      apiCalls.push({
        method: req.method(),
        url: new URL(url).pathname + new URL(url).search,
        status: null
      });
    }
  };
  const responseListener = (res: Response) => {
    const url = res.url();
    if (url.includes('/api/')) {
      const idx = apiCalls.findIndex(
        (c) => c.url === new URL(url).pathname + new URL(url).search && c.status === null
      );
      if (idx >= 0) {
        (apiCalls[idx] as { status: number | null }).status = res.status();
      }
    }
  };
  const consoleListener = (msg: { type(): string; text(): string }) => {
    const t = msg.type();
    if (t === 'error' || t === 'warning') {
      consoleIssues.push({ type: t, text: msg.text() });
    }
  };

  page.on('request', requestListener);
  page.on('response', responseListener);
  page.on('console', consoleListener);

  let verdict: Verdict = 'LEER';
  let dom: DomSample | null = null;
  let note = '';
  const screenshotRel = path.join(
    'ui-exploration',
    'screenshots',
    `${route.slug}.png`
  );
  const screenshotAbs = path.join(cfg.outputRoot, screenshotRel);

  try {
    const resp = await page.goto(cfg.frontendUrl + '/' + route.path, {
      waitUntil: 'networkidle',
      timeout: 20_000
    });
    navigationStatus = resp?.status() ?? null;
    // Kleine Pause fuer async-Effekte nach networkidle
    await page.waitForTimeout(500);
    dom = await sampleDom(page);
    await page.screenshot({ path: screenshotAbs, fullPage: true });

    // Verdict-Heuristik
    if (
      consoleIssues.some((c) => c.type === 'error') ||
      apiCalls.some((c) => c.status !== null && c.status >= 500)
    ) {
      verdict = 'FEHLER';
      note = 'Konsolen-Fehler oder HTTP 5xx in API-Antworten.';
    } else if (
      apiCalls.some((c) => c.status !== null && c.status >= 400 && c.status < 500)
    ) {
      verdict = 'FEHLER';
      note = 'HTTP 4xx-Antwort im /api/-Aufruf (Auth/Validation?).';
    } else if (dom.hasPlaceholder) {
      verdict = 'PLATZHALTER';
      note = '<cvm-page-placeholder> gefunden.';
    } else if (dom.hasTable || dom.hasForm || dom.hasChart) {
      verdict = 'INHALT';
    } else if (dom.hasEmptyState && apiCalls.length > 0) {
      verdict = 'INHALT';
      note = 'Leerzustand mit echtem API-Call (Backend liefert leere Liste).';
    } else if (dom.mainChildren <= 1 && apiCalls.length === 0) {
      verdict = 'LEER';
      note = 'Keine API-Calls, fast leerer Main-Content.';
    } else {
      verdict = 'INHALT';
    }
  } catch (err) {
    verdict = 'NICHT_ERREICHBAR';
    note = err instanceof Error ? err.message : String(err);
  } finally {
    page.off('request', requestListener);
    page.off('response', responseListener);
    page.off('console', consoleListener);
  }

  return {
    path: '/' + route.path,
    slug: route.slug,
    requiredRoleHint: route.requiredRoleHint,
    navigationStatus,
    verdict,
    apiCalls,
    consoleIssues,
    dom,
    screenshot: screenshotRel,
    note
  };
}

async function sampleDom(page: Page): Promise<DomSample> {
  return await page.evaluate(() => {
    const main =
      document.querySelector('mat-sidenav-content') ||
      document.querySelector('main') ||
      document.body;
    const mainChildren = main?.children.length ?? 0;
    const heading = document.querySelector('h1, h2');
    return {
      mainChildren,
      hasTable: !!main?.querySelector('table'),
      hasForm: !!main?.querySelector('form, mat-form-field'),
      hasChart: !!main?.querySelector('canvas, svg.chart, [echarts]'),
      hasPlaceholder: !!document.querySelector(
        '[data-testid="cvm-page-placeholder"], cvm-page-placeholder'
      ),
      hasEmptyState: !!main?.querySelector('ahs-empty-state'),
      headingText: heading?.textContent?.trim() ?? ''
    };
  });
}

async function writeMarkdown(
  outcomes: readonly RouteOutcome[],
  cfg: Config
): Promise<void> {
  const countByVerdict: Record<Verdict, number> = {
    INHALT: 0,
    PLATZHALTER: 0,
    LEER: 0,
    FEHLER: 0,
    NICHT_ERREICHBAR: 0
  };
  for (const o of outcomes) {
    countByVerdict[o.verdict]++;
  }
  const lines: string[] = [];
  lines.push('# UI-Exploration-Report');
  lines.push('');
  lines.push(`**Ziel**: \`${cfg.target}\` (${cfg.frontendUrl})`);
  lines.push(`**Stand**: ${cfg.dateStamp}`);
  lines.push(`**User**: ${cfg.username}`);
  lines.push('');
  lines.push('## Zusammenfassung');
  lines.push('');
  lines.push('| Verdict | Anzahl |');
  lines.push('|---|---|');
  (
    ['INHALT', 'PLATZHALTER', 'LEER', 'FEHLER', 'NICHT_ERREICHBAR'] as Verdict[]
  ).forEach((v) => {
    lines.push(`| ${v} | ${countByVerdict[v]} |`);
  });
  lines.push('');
  lines.push('## Routen im Detail');
  lines.push('');
  for (const o of outcomes) {
    lines.push(`### ${o.path} (${o.verdict})`);
    lines.push('');
    lines.push(
      `- Rolle (Hinweis): \`${o.requiredRoleHint}\` - Navigation: ${o.navigationStatus ?? '–'}`
    );
    lines.push(`- Screenshot: \`${o.screenshot}\``);
    if (o.note) {
      lines.push(`- Notiz: ${o.note}`);
    }
    if (o.dom) {
      lines.push(
        `- DOM: main-children=${o.dom.mainChildren}, table=${o.dom.hasTable}, form=${o.dom.hasForm}, chart=${o.dom.hasChart}, placeholder=${o.dom.hasPlaceholder}, empty-state=${o.dom.hasEmptyState}, heading="${o.dom.headingText}"`
      );
    }
    if (o.apiCalls.length > 0) {
      lines.push('- API-Calls:');
      for (const c of o.apiCalls) {
        lines.push(`  - ${c.method} ${c.url} -> ${c.status ?? '(pending)'}`);
      }
    } else {
      lines.push('- API-Calls: (keine)');
    }
    if (o.consoleIssues.length > 0) {
      lines.push('- Konsole:');
      for (const c of o.consoleIssues.slice(0, 5)) {
        lines.push(`  - [${c.type}] ${c.text}`);
      }
      if (o.consoleIssues.length > 5) {
        lines.push(`  - ... (${o.consoleIssues.length - 5} weitere)`);
      }
    }
    lines.push('');
  }
  const mdPath = path.join(cfg.outputRoot, 'ui-exploration-report.md');
  await writeFile(mdPath, lines.join('\n'), 'utf8');
  console.log(`[report] Markdown geschrieben: ${mdPath}`);
}

async function writeJson(
  outcomes: readonly RouteOutcome[],
  cfg: Config
): Promise<void> {
  const jsonPath = path.join(cfg.outputRoot, 'ui-exploration.json');
  await writeFile(
    jsonPath,
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        target: cfg.target,
        frontendUrl: cfg.frontendUrl,
        user: cfg.username,
        outcomes
      },
      null,
      2
    ),
    'utf8'
  );
  console.log(`[report] JSON geschrieben: ${jsonPath}`);
}

async function main(): Promise<number> {
  const cfg = parseArgs();
  console.log(`[setup] target=${cfg.target}, frontend=${cfg.frontendUrl}`);
  await mkdir(path.join(cfg.outputRoot, 'ui-exploration', 'screenshots'), {
    recursive: true
  });

  let browser: Browser | null = null;
  let exitCode = 0;
  try {
    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ ignoreHTTPSErrors: true });
    const page = await context.newPage();
    await login(page, cfg);

    const outcomes: RouteOutcome[] = [];
    for (const route of EXPLORE_ROUTES) {
      console.log(`[route] ${route.path}`);
      const outcome = await exploreRoute(page, route, cfg);
      outcomes.push(outcome);
      if (outcome.verdict === 'FEHLER' || outcome.verdict === 'NICHT_ERREICHBAR') {
        exitCode = 1;
      }
    }

    // Settings-Rubriken (Tabs innerhalb einer Komponente)
    try {
      await page.goto(cfg.frontendUrl + '/settings', { waitUntil: 'networkidle' });
      for (const section of SETTINGS_SECTIONS) {
        const shotRel = path.join(
          'ui-exploration',
          'screenshots',
          `${section.slug}.png`
        );
        const shotAbs = path.join(cfg.outputRoot, shotRel);
        try {
          const locator = page.locator(section.clickSelector).first();
          if ((await locator.count()) > 0) {
            await locator.click({ timeout: 5_000 });
            await page.waitForTimeout(400);
            await page.screenshot({ path: shotAbs, fullPage: true });
            console.log(`[settings] ${section.label} -> ${shotRel}`);
          } else {
            console.log(`[settings] ${section.label}: Selector nicht gefunden.`);
          }
        } catch (err) {
          console.log(
            `[settings] ${section.label}: Klick-Fehler: ${
              err instanceof Error ? err.message : String(err)
            }`
          );
        }
      }
    } catch (err) {
      console.log(
        `[settings] konnten nicht geoeffnet werden: ${
          err instanceof Error ? err.message : String(err)
        }`
      );
    }

    await writeMarkdown(outcomes, cfg);
    await writeJson(outcomes, cfg);
  } catch (err) {
    console.error(`[fatal] ${err instanceof Error ? err.message : String(err)}`);
    exitCode = 2;
  } finally {
    await browser?.close();
  }
  return exitCode;
}

main()
  .then((code) => process.exit(code))
  .catch((err) => {
    console.error(err);
    process.exit(2);
  });
