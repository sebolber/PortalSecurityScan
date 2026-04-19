import { TestBed } from '@angular/core/testing';
import { RulesComponent } from './rules.component';
import { RulesService, RuleResponse } from '../../core/rules/rules.service';
import { AuthService } from '../../core/auth/auth.service';
import { CvmConfirmService } from '../../shared/components/cvm-confirm.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

class FakeToast {
  success = jasmine.createSpy('success');
  warning = jasmine.createSpy('warning');
  error = jasmine.createSpy('error');
}

class FakeAuth {
  loggedIn = () => true;
  userRoles = () => ['CVM_ADMIN', 'CVM_RULE_AUTHOR', 'CVM_RULE_APPROVER'];
  username = () => 'a.admin@ahs.test';
  hasRole = () => true;
  refreshFromKeycloak(): void {}
  async login(): Promise<void> {}
  async logout(): Promise<void> {}
  async getToken(): Promise<string> {
    return '';
  }
}

class FakeConfirm {
  result = true;
  lastOptions: unknown = null;
  confirm(options: unknown): Promise<boolean> {
    this.lastOptions = options;
    return Promise.resolve(this.result);
  }
}

/**
 * Iteration 90 (CVM-330): Rules-Loesche nutzt CvmConfirmService.
 */
describe('RulesComponent - Iteration 90', () => {
  let service: jasmine.SpyObj<RulesService>;
  let toast: FakeToast;
  let confirmService: FakeConfirm;

  const rule: RuleResponse = {
    id: 'r1',
    ruleKey: 'kev-critical',
    name: 'KEV erzwingt CRITICAL',
    description: null,
    status: 'ACTIVE',
    version: 1,
    proposedSeverity: 'CRITICAL',
    conditionJson: '{}',
    rationaleTemplate: null,
    rationaleSourceFields: [],
    origin: 'MANUAL',
    createdBy: 'a.admin@ahs.test',
    createdAt: '2026-04-19T00:00:00Z',
    activatedBy: null,
    activatedAt: null
  } as RuleResponse;

  beforeEach(() => {
    service = jasmine.createSpyObj<RulesService>('RulesService', [
      'list',
      'activate',
      'delete',
      'dryRun',
      'update',
      'create'
    ]);
    service.list.and.returnValue(Promise.resolve([]));
    service.delete.and.returnValue(Promise.resolve());
    toast = new FakeToast();
    confirmService = new FakeConfirm();

    TestBed.configureTestingModule({
      imports: [RulesComponent],
      providers: [
        { provide: RulesService, useValue: service },
        { provide: AuthService, useClass: FakeAuth },
        { provide: CvmToastService, useValue: toast },
        { provide: CvmConfirmService, useValue: confirmService }
      ]
    });
  });

  it('loesche ruft CvmConfirmService mit danger-Variante und DELETE-Endpunkt', async () => {
    const component = TestBed.createComponent(RulesComponent)
      .componentInstance;
    confirmService.result = true;

    await component.loesche(rule);

    expect(service.delete).toHaveBeenCalledOnceWith('r1');
    expect((confirmService.lastOptions as { variant: string }).variant)
      .toBe('danger');
    expect(toast.success).toHaveBeenCalled();
  });

  it('Abbruch verhindert DELETE', async () => {
    const component = TestBed.createComponent(RulesComponent)
      .componentInstance;
    confirmService.result = false;

    await component.loesche(rule);
    expect(service.delete).not.toHaveBeenCalled();
  });
});
