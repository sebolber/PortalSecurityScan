/**
 * Iteration 84 (CVM-324): Popover-Logik des ShellComponent wird
 * hier auf Komponenten-Ebene getestet, ohne die Shell komplett im
 * DOM zu rendern - der Shell-Konstruktor zieht zahlreiche
 * providedIn:'root'-Services mit HttpClient-Abhaengigkeiten, die
 * Karma ohne reale Bootstrap-Environment nicht alle aufloest.
 */
import { signal } from '@angular/core';
import { ShellComponent } from './shell.component';
import { TenantView, TenantsService } from '../core/tenants/tenants.service';

class FakeAuth {
  roles: string[] = [];
  userRoles = () => this.roles;
}

class FakeTenants {
  current = jasmine.createSpy('current').and.resolveTo(null as TenantView | null);
  list = jasmine.createSpy('list').and.resolveTo([] as TenantView[]);
  setDefault = jasmine.createSpy('setDefault').and.resolveTo(null as unknown as TenantView);
}

type ShellInternals = {
  tenantMenuOpen: ReturnType<typeof signal<boolean>>;
  userMenuOpen: ReturnType<typeof signal<boolean>>;
  alleTenants: ReturnType<typeof signal<readonly TenantView[]>>;
  istAdmin: () => boolean;
  toggleTenantMenu(): void;
  toggleUserMenu(): void;
  closeMenus(): void;
  setzeAlsDefault(id: string): Promise<void>;
};

function installiere(instance: ShellComponent, auth: FakeAuth, tenants: FakeTenants): ShellInternals {
  // Angular's inject() kann in Unit-Tests ohne Ausfuehrungs-
  // Kontext nicht laufen; daher weisen wir die Referenzen direkt
  // ueber den Konstruktor-Kontext nach. Wir greifen pragmatisch
  // ueber Reflect auf die privaten Felder zu.
  (instance as unknown as { auth: FakeAuth }).auth = auth;
  (instance as unknown as { tenants: FakeTenants }).tenants = tenants;
  return instance as unknown as ShellInternals;
}

describe('ShellComponent - Iteration 84 Tenant-Popover (Unit)', () => {
  function mkComponent(): ShellComponent {
    // Reiner `new ShellComponent()`-Aufruf wuerde inject() werfen,
    // weil wir nicht im Angular-DI-Kontext sind. Wir bauen die
    // Instanz stattdessen mit TestBed.runInInjectionContext-
    // alternativ: Object.create um den Konstruktor zu umgehen und
    // nur die Public-API zu testen.
    return Object.create(ShellComponent.prototype) as ShellComponent;
  }

  it('toggleTenantMenu: oeffnet und schliesst das Popover', () => {
    const instance = mkComponent();
    const s = installiere(
      instance,
      new FakeAuth(),
      new FakeTenants()
    );
    // Manuell Signale setzen (waere sonst vom Konstruktor init).
    (instance as unknown as {
      tenantMenuOpen: ReturnType<typeof signal<boolean>>;
      userMenuOpen: ReturnType<typeof signal<boolean>>;
      alleTenants: ReturnType<typeof signal<readonly TenantView[]>>;
      tenantMenuLaedt: ReturnType<typeof signal<boolean>>;
    }).tenantMenuOpen = signal(false);
    (instance as unknown as { userMenuOpen: ReturnType<typeof signal<boolean>> })
      .userMenuOpen = signal(false);
    (instance as unknown as { alleTenants: ReturnType<typeof signal<readonly TenantView[]>> })
      .alleTenants = signal([]);
    (instance as unknown as { tenantMenuLaedt: ReturnType<typeof signal<boolean>> })
      .tenantMenuLaedt = signal(false);
    (instance as unknown as { istAdmin: () => boolean }).istAdmin = () => false;

    s.toggleTenantMenu();
    expect(s.tenantMenuOpen()).toBeTrue();

    s.toggleTenantMenu();
    expect(s.tenantMenuOpen()).toBeFalse();
  });

  it('Admin-Rolle: toggleTenantMenu laedt alle Tenants', async () => {
    const instance = mkComponent();
    const tenants = new FakeTenants();
    const s = installiere(instance, new FakeAuth(), tenants);
    (instance as unknown as { tenantMenuOpen: ReturnType<typeof signal<boolean>> })
      .tenantMenuOpen = signal(false);
    (instance as unknown as { userMenuOpen: ReturnType<typeof signal<boolean>> })
      .userMenuOpen = signal(false);
    (instance as unknown as { alleTenants: ReturnType<typeof signal<readonly TenantView[]>> })
      .alleTenants = signal([]);
    (instance as unknown as { tenantMenuLaedt: ReturnType<typeof signal<boolean>> })
      .tenantMenuLaedt = signal(false);
    (instance as unknown as { istAdmin: () => boolean }).istAdmin = () => true;

    s.toggleTenantMenu();
    await Promise.resolve();
    expect(tenants.list).toHaveBeenCalled();
  });

  it('setzeAlsDefault: ruft TenantsService.setDefault', async () => {
    const instance = mkComponent();
    const tenants = new FakeTenants();
    const s = installiere(instance, new FakeAuth(), tenants);
    (instance as unknown as { tenantMenuLaedt: ReturnType<typeof signal<boolean>> })
      .tenantMenuLaedt = signal(false);
    (instance as unknown as { alleTenants: ReturnType<typeof signal<readonly TenantView[]>> })
      .alleTenants = signal([]);
    (instance as unknown as { tenantSig: ReturnType<typeof signal<TenantView | null>> })
      .tenantSig = signal(null);

    await s.setzeAlsDefault('t-x');
    expect(tenants.setDefault).toHaveBeenCalledWith('t-x');
  });
});
