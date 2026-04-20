import { TestBed } from '@angular/core/testing';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Iteration 97 (CVM-338): Registry-Coverage fuer alle Icons, die
 * `RoleMenuService` an den Sidebar-Menues verwendet. Wenn hier ein
 * Name zum `null`-Fallback rutscht, zeigt die Shell einen Punkt
 * statt des erwarteten Icons.
 */
describe('CvmIconComponent - Menue-Icon-Registry', () => {
  const MENUE_ICONS = [
    'dashboard',
    'cloud_upload',
    'rule',
    'account_tree',
    'verified',
    'sensors',
    'rule_folder',
    'description',
    'bug_report',
    'inventory_2',
    'history',
    'fact_check',
    'insights',
    'settings',
    'tune',
    'gavel',
    'category',
    'layers',
    'palette',
    'smart_toy',
    'group',
    'upload',
    'play'
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  for (const name of MENUE_ICONS) {
    it(`registry liefert ein Icon fuer "${name}"`, () => {
      const fixture = TestBed.createComponent(CvmIconComponent);
      fixture.componentInstance.name = name;
      fixture.detectChanges();
      expect(fixture.componentInstance.resolved())
        .withContext(`Icon "${name}" fehlt in der Registry`)
        .not.toBeNull();
    });
  }
});
