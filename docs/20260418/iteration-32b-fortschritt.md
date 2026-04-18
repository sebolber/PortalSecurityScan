# Iteration 32b – Fortschritt

**Thema**: Offene Punkte aus 32 (Offline-Icons + Karma-Chromium)
**Jira**: CVM-75
**Datum**: 2026-04-18

## Umgesetzt

### Material-Icons air-gapped-faehig

- npm-Pakete `material-icons` (1.x) und `@fontsource/fira-sans`
  als Runtime-Abhaengigkeiten installiert.
- `cvm-frontend/src/styles.scss` importiert die CSS-Bundles direkt
  aus `node_modules` - die TTF/WOFF-Dateien werden von Angulars
  `ng build` in den Output kopiert.
- `cvm-frontend/src/index.html`: CDN-`<link>`-Tags auf
  `fonts.googleapis.com` entfernt. Der Kommentar zeigt auf die
  neue SCSS-Einbindung.
- Ergebnis: CVM laedt Icons und Schrift ausschliesslich aus dem
  eigenen Bundle; keine Netzwerkabhaengigkeit zu
  `fonts.googleapis.com` mehr.

### Karma mit Puppeteer-Chromium

- `puppeteer` als Dev-Dependency installiert (liefert einen
  eigenen Chromium-Binary).
- Neue `cvm-frontend/karma.conf.cjs` setzt
  `process.env.CHROME_BIN` auf Puppeteers Pfad und definiert den
  Launcher `ChromeHeadlessNoSandbox` mit
  `--no-sandbox --disable-gpu --disable-dev-shm-usage --headless=new`.
- `cvm-frontend/angular.json`: `test.options.karmaConfig` zeigt
  auf diese Config. `browsers` bleibt Default (`ChromeHeadlessNoSandbox`
  aus der Karma-Config).
- Sanity-Check: `npx ng test --watch=false` laeuft jetzt vollstaendig
  durch (siehe Test-Summary).

### Test-Fix

- `queue-api.service.spec.ts`: Der Test "list mit Filter
  serialisiert Query-Parameter" griff auf `r.params.get(...)` zu,
  obwohl der Service die Query-Parameter direkt in den URL-String
  schreibt (`/api/v1/findings?status=PROPOSED&...`). Das war
  seit Iteration 02 inkonsistent und blieb unentdeckt, weil Karma
  in der Sandbox bisher gar nicht laufen konnte. Jetzt mit
  Puppeteer lief der Test rot. Fix: `r.urlWithParams`-basierte
  Assertion.

## Dateien

- `cvm-frontend/package.json` + `package-lock.json`
- `cvm-frontend/src/styles.scss`
- `cvm-frontend/src/index.html`
- `cvm-frontend/karma.conf.cjs` (neu)
- `cvm-frontend/angular.json`
- `cvm-frontend/src/app/features/queue/queue-api.service.spec.ts`

## Verifikation

- `./mvnw -T 1C test` &rarr; **BUILD SUCCESS** (9 Module).
- `npx ng build --configuration=development` &rarr; erfolgreich;
  Initial Bundle steigt nur geringfuegig (Font-Woff2-Dateien liegen
  jetzt im Output statt per CDN geladen).
- `npx ng test --watch=false` &rarr; **TOTAL: 68 SUCCESS** (vorher
  gar nicht lauffaehig).

## Offene Punkte

- Keine aus den beiden Backlog-Eintraegen. Der alte Eintrag
  "FullNavigationWalkThroughTest + axe-core in Playwright" bleibt
  separat bestehen.
