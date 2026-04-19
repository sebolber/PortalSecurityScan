import { TestBed } from '@angular/core/testing';
import { AdminEnvironmentsComponent } from './admin-environments.component';
import {
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import { CvmConfirmService } from '../../shared/components/cvm-confirm.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

class FakeToast {
  success = jasmine.createSpy('success');
  warning = jasmine.createSpy('warning');
  error = jasmine.createSpy('error');
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
 * Iteration 90 (CVM-330): window.confirm -> CvmConfirmService.
 */
describe('AdminEnvironmentsComponent - Iteration 90', () => {
  let service: jasmine.SpyObj<EnvironmentsService>;
  let toast: FakeToast;
  let confirmService: FakeConfirm;

  const env: EnvironmentView = {
    id: 'e1',
    key: 'REF-TEST',
    name: 'Referenz Test',
    stage: 'REF',
    tenant: 'default'
  } as EnvironmentView;

  beforeEach(() => {
    service = jasmine.createSpyObj<EnvironmentsService>('EnvironmentsService', [
      'list',
      'create',
      'delete'
    ]);
    service.list.and.returnValue(Promise.resolve([]));
    service.delete.and.returnValue(Promise.resolve());
    toast = new FakeToast();
    confirmService = new FakeConfirm();

    TestBed.configureTestingModule({
      imports: [AdminEnvironmentsComponent],
      providers: [
        { provide: EnvironmentsService, useValue: service },
        { provide: CvmToastService, useValue: toast },
        { provide: CvmConfirmService, useValue: confirmService }
      ]
    });
  });

  it('loesche ruft CvmConfirmService mit danger-Variante und DELETE-Endpunkt', async () => {
    const component = TestBed.createComponent(AdminEnvironmentsComponent)
      .componentInstance;
    confirmService.result = true;

    await component.loesche(env);

    expect(service.delete).toHaveBeenCalledOnceWith('e1');
    expect((confirmService.lastOptions as { variant: string }).variant)
      .toBe('danger');
    expect(toast.success).toHaveBeenCalled();
  });

  it('Abbruch durch CvmConfirmService verhindert DELETE', async () => {
    const component = TestBed.createComponent(AdminEnvironmentsComponent)
      .componentInstance;
    confirmService.result = false;

    await component.loesche(env);

    expect(service.delete).not.toHaveBeenCalled();
  });
});
