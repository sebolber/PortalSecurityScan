import { RoleMenuService } from './role-menu.service';
import { CVM_ROLES } from './cvm-roles';

describe('RoleMenuService', () => {
  const service = new RoleMenuService();

  it('Viewer sieht Dashboard, CVEs, Komponenten, Berichte (kein Admin-Eintrag)', () => {
    const sichtbar = service.visibleEntries([CVM_ROLES.VIEWER]);
    const ids = sichtbar.map((e) => e.id);
    expect(ids).toContain('dashboard');
    expect(ids).toContain('cves');
    expect(ids).toContain('components');
    expect(ids).toContain('reports');
    expect(ids).not.toContain('profiles');
    expect(ids).not.toContain('rules');
    expect(ids).not.toContain('ai-audit');
  });

  it('Approver sieht die Bewertungs-Queue', () => {
    const ids = service.visibleEntries([CVM_ROLES.APPROVER]).map((e) => e.id);
    expect(ids).toContain('queue');
    expect(ids).toContain('dashboard');
  });

  it('AI-Auditor sieht KI-Audit, aber keine Profile', () => {
    const ids = service.visibleEntries([CVM_ROLES.AI_AUDITOR]).map((e) => e.id);
    expect(ids).toEqual(['ai-audit']);
  });

  it('Admin sieht alle Menue-Eintraege', () => {
    const ids = service.visibleEntries([CVM_ROLES.ADMIN]).map((e) => e.id);
    expect(ids.length).toBe(service.allEntries().length);
  });

  it('Ohne Rolle ist die Liste leer', () => {
    expect(service.visibleEntries([])).toEqual([]);
  });

  it('hasAccess akzeptiert nur Pfade, deren Rolle gesetzt ist', () => {
    expect(service.hasAccess('/profiles', [CVM_ROLES.ADMIN])).toBeTrue();
    expect(service.hasAccess('/profiles', [CVM_ROLES.VIEWER])).toBeFalse();
    expect(service.hasAccess('/unbekannt', [CVM_ROLES.ADMIN])).toBeFalse();
  });
});
