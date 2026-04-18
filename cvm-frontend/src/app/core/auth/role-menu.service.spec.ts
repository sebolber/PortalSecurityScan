import { MenuEntry, RoleMenuService } from './role-menu.service';
import { CVM_ROLES } from './cvm-roles';

function flatIds(entries: readonly MenuEntry[]): string[] {
  const ids: string[] = [];
  for (const e of entries) {
    ids.push(e.id);
    for (const child of e.children ?? []) {
      ids.push(child.id);
    }
  }
  return ids;
}

describe('RoleMenuService', () => {
  const service = new RoleMenuService();

  it('Viewer sieht Dashboard, CVEs, Komponenten, Berichte, Einstellungen (keine Autor-/Admin-Eintraege)', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.VIEWER]));
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
    const ids = flatIds(service.visibleEntries([CVM_ROLES.APPROVER]));
    expect(ids).toContain('queue');
    expect(ids).toContain('dashboard');
    expect(ids).not.toContain('profiles');
    expect(ids).not.toContain('rules');
  });

  it('Reviewer sieht die Bewertungs-Queue (Ablehnung moeglich)', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.REVIEWER]));
    expect(ids).toContain('queue');
  });

  it('Profile-Autor sieht Profile als Kind von Einstellungen, aber keine Regeln', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.PROFILE_AUTHOR]));
    expect(ids).toContain('profiles');
    expect(ids).not.toContain('rules');
  });

  it('Regel-Freigeber sieht Regeln als Kind von Einstellungen, aber keine Profile', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.RULE_APPROVER]));
    expect(ids).toContain('rules');
    expect(ids).not.toContain('profiles');
  });

  it('Reporter sieht Berichte und Komponenten, keine Queue', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.REPORTER]));
    expect(ids).toContain('reports');
    expect(ids).toContain('components');
    expect(ids).not.toContain('queue');
  });

  it('AI-Auditor sieht KI-Audit und Einstellungen', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.AI_AUDITOR]));
    expect(ids).toContain('ai-audit');
    expect(ids).toContain('settings');
    expect(ids).not.toContain('profiles');
  });

  it('Admin sieht alle Menue-Eintraege (inkl. Kinder)', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.ADMIN]));
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

  it('Admin sieht Produkt-Admin (als Kind) und Scan-Upload (Top-Level)', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.ADMIN]));
    expect(ids).toContain('admin-products');
    expect(ids).toContain('scan-upload');
  });

  it('Bewerter sieht Scan-Upload, aber nicht Produkt-Admin', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.ASSESSOR]));
    expect(ids).toContain('scan-upload');
    expect(ids).not.toContain('admin-products');
  });

  it('Viewer sieht weder Produkt-Admin noch Scan-Upload', () => {
    const ids = flatIds(service.visibleEntries([CVM_ROLES.VIEWER]));
    expect(ids).not.toContain('admin-products');
    expect(ids).not.toContain('scan-upload');
  });

  it('Workflow-Reihenfolge: Dashboard vor Scan-Upload vor Queue vor Reports', () => {
    const admin = service.visibleEntries([CVM_ROLES.ADMIN]);
    const topIds = admin.map((e) => e.id);
    const expectBefore = (earlier: string, later: string): void => {
      expect(topIds.indexOf(earlier))
        .withContext(`${earlier} vor ${later}`)
        .toBeLessThan(topIds.indexOf(later));
    };
    expectBefore('dashboard', 'scan-upload');
    expectBefore('scan-upload', 'queue');
    expectBefore('queue', 'reachability');
    expectBefore('reachability', 'fix-verification');
    expectBefore('waivers', 'reports');
    expectBefore('reports', 'cves');
    expectBefore('cves', 'components');
  });

  it('Einstellungen steht am Ende und haelt die Konfigurations-Unterpunkte', () => {
    const admin = service.visibleEntries([CVM_ROLES.ADMIN]);
    const settingsEntry = admin.find((e) => e.id === 'settings');
    expect(settingsEntry).toBeTruthy();
    expect(admin[admin.length - 1].id).toBe('settings');
    const childIds = (settingsEntry?.children ?? []).map((c) => c.id);
    expect(childIds).toEqual([
      'profiles',
      'rules',
      'admin-products',
      'admin-environments',
      'admin-theme'
    ]);
  });
});
