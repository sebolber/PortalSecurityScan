/**
 * Keycloak-Realm-Rollen, die das CVM-Frontend kennt.
 *
 * Spiegelt {@code infra/keycloak/dev-realm.json} eins zu eins. Bei
 * Anpassungen am Realm muessen die Konstanten hier mitgepflegt werden.
 *
 * Zuordnung (siehe 07-Frontend-Shell.md, Konzept v0.2 Abschnitt 4.5):
 * - CVM_VIEWER     → Lesezugriff (Dashboard, Queue read-only).
 * - CVM_ASSESSOR   → darf PROPOSED-Vorschlaege anlegen.
 * - CVM_APPROVER   → darf approve/reject (Vier-Augen).
 * - CVM_ADMIN      → Profile/Regeln/Workflow-Admin.
 * - PRODUCT_OWNER  → Produkt-/Versionspflege, Berichte (kuenftig).
 * - AI_AUDITOR     → KI-Audit-Trail Lesezugriff (kuenftig).
 */
export const CVM_ROLES = {
  VIEWER: 'CVM_VIEWER',
  ASSESSOR: 'CVM_ASSESSOR',
  APPROVER: 'CVM_APPROVER',
  ADMIN: 'CVM_ADMIN',
  PRODUCT_OWNER: 'PRODUCT_OWNER',
  AI_AUDITOR: 'AI_AUDITOR'
} as const;

export type CvmRole = (typeof CVM_ROLES)[keyof typeof CVM_ROLES];
