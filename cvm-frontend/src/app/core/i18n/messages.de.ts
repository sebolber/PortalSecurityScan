/**
 * Statisches Sprach-Map fuer Deutsch. Iteration 07 nutzt nur ein
 * funktionales Lookup, ein i18n-Framework wird mit Iteration 08
 * (Queue-UI) entschieden.
 */
export const MESSAGES_DE = {
  app: {
    title: 'CVE-Relevance-Manager',
    login: 'Anmelden',
    logout: 'Abmelden',
    notLoggedIn: 'Nicht angemeldet'
  },
  shell: {
    productSelector: 'Produkt / Umgebung waehlen',
    productSelectorHint: 'Auswahl folgt in Iteration 08'
  },
  dashboard: {
    title: 'Dashboard',
    openCves: 'Offene CVEs',
    openCvesSubtitle: 'Summe aller PROPOSED- und NEEDS_REVIEW-Eintraege',
    severityChart: 'Severity-Verteilung',
    oldestCritical: 'Aelteste offene CRITICAL-CVE',
    operationStatus: 'Weiterbetrieb moeglich?',
    operationStatusOk: 'Ja - keine kritischen offenen CVEs.',
    operationStatusWarning: 'Achtung - offene CRITICAL-Eintraege vorhanden.'
  },
  errors: {
    backendUnreachable: 'Backend nicht erreichbar.',
    forbidden: 'Keine Berechtigung.',
    unknown: 'Unbekannter Fehler.'
  }
} as const;

export type Messages = typeof MESSAGES_DE;
