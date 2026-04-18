import { RoleMenuService } from './role-menu.service';
import { CVM_ROLES } from './cvm-roles';

describe('RoleMenuService', () => {
  const service = new RoleMenuService();

  it('Viewer sieht Dashboard, CVEs, Komponenten, Berichte, Einstellungen (keine Autor-/Admin-Eintraege)', () => {
    const ids = service.visibleEntries([CVM_ROLES.VIEWER]).map((e) => e.id);
    expect(ids).toContain('dashboard');
    expect(ids).toContain('cves');
    expect(ids).toContain('components');
    expect(ids).toContain('reports');
    expect(ids).toContain('settings');
    expect(ids).not.toContain('profiles');
    expect(ids).not.toContain('rules');
    expect(ids).not.toContain('queue');
    expect(ids).not.toContain('ai-audit');
  });

  it('Approver sieht die Bewertungs-Queue und das Dashboard', () => {
    const ids = service.visibleEntries([CVM_ROLES.APPROVER]).map((e) => e.id);
    expect(ids).toContain('queue');
    expect(ids).toContain('dashboard');
    expect(ids).not.toContain('profiles');
    expect(ids).not.toContain('rules');
  });

  it('Reviewer sieht die Bewertungs-Queue (Ablehnung moeglich)', () => {
    const ids = service.visibleEntries([CVM_ROLES.REVIEWER]).map((e) => e.id);
    expect(ids).toContain('queue');
  });

  it('Profile-Autor sieht Profile, aber keine Regeln', () => {
    const ids = service.visibleEntries([CVM_ROLES.PROFILE_AUTHOR]).map((e) => e.id);
    expect(ids).toContain('profiles');
    expect(ids).not.toContain('rules');
  });

  it('Regel-Freigeber sieht Regeln, aber keine Profile', () => {
    const ids = service.visibleEntries([CVM_ROLES.RULE_APPROVER]).map((e) => e.id);
    expect(ids).toContain('rules');
    expect(ids).not.toContain('profiles');
  });

  it('Reporter sieht Berichte und Komponenten, keine Queue', () => {
    const ids = service.visibleEntries([CVM_ROLES.REPORTER]).map((e) => e.id);
    expect(ids).toContain('reports');
    expect(ids).toContain('components');
    expect(ids).not.toContain('queue');
  });

  it('AI-Auditor sieht KI-Audit und Einstellungen', () => {
    const ids = service.visibleEntries([CVM_ROLES.AI_AUDITOR]).map((e) => e.id);
    expect(ids).toContain('ai-audit');
    expect(ids).toContain('settings');
    expect(ids).not.toContain('profiles');
  });

  it('Admin sieht alle Menue-Eintraege', () => {
    const ids = service.visibleEntries([CVM_ROLES.ADMIN]).map((e) => e.id);
    expect(ids.length).toBe(service.allEntries().length);
  });

  it('Ohne Rolle ist die Liste leer', () => {
    expect(service.visibleEntries([])).toEqual([]);
  });

  it('hasAccess akzeptiert nur Pfade, deren Rolle gesetzt ist', () => {
    expect(service.hasAccess('/profiles', [CVM_ROLES.PROFILE_AUTHOR])).toBeTrue();
    expect(service.hasAccess('/profiles', [CVM_ROLES.ADMIN])).toBeTrue();
    expect(service.hasAccess('/profiles', [CVM_ROLES.VIEWER])).toBeFalse();
    expect(service.hasAccess('/rules', [CVM_ROLES.RULE_AUTHOR])).toBeTrue();
    expect(service.hasAccess('/unbekannt', [CVM_ROLES.ADMIN])).toBeFalse();
  });

  it('Admin sieht Produkt-Admin und Scan-Upload', () => {
    const ids = service.visibleEntries([CVM_ROLES.ADMIN]).map((e) => e.id);
    expect(ids).toContain('admin-products');
    expect(ids).toContain('scan-upload');
  });

  it('Bewerter sieht Scan-Upload, aber nicht Produkt-Admin', () => {
    const ids = service.visibleEntries([CVM_ROLES.ASSESSOR]).map((e) => e.id);
    expect(ids).toContain('scan-upload');
    expect(ids).not.toContain('admin-products');
  });

  it('Viewer sieht weder Produkt-Admin noch Scan-Upload', () => {
    const ids = service.visibleEntries([CVM_ROLES.VIEWER]).map((e) => e.id);
    expect(ids).not.toContain('admin-products');
    expect(ids).not.toContain('scan-upload');
  });
});
