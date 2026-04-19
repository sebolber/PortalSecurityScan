import { TestBed } from '@angular/core/testing';
import { AdminParametersComponent } from './admin-parameters.component';
import {
  SystemParameterService,
  SystemParameterView
} from '../../core/parameters/system-parameter.service';
import { CvmConfirmService } from '../../shared/components/cvm-confirm.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

class FakeToast {
  success = jasmine.createSpy('success');
  warning = jasmine.createSpy('warning');
  error = jasmine.createSpy('error');
}

class FakeConfirm {
  result = true;
  confirm = jasmine
    .createSpy('confirm')
    .and.callFake(() => Promise.resolve(this.result));
}

/**
 * Iteration 95 (CVM-335): Secret-Rotations-Dialog und
 * CvmConfirmService-Integration.
 */
describe('AdminParametersComponent - Iteration 95', () => {
  let service: jasmine.SpyObj<SystemParameterService>;
  let toast: FakeToast;
  let confirmService: FakeConfirm;

  const sensitiverEintrag: SystemParameterView = {
    id: 'p1',
    paramKey: 'cvm.llm.claude.api-key',
    label: 'Claude API Key',
    description: null,
    handbook: null,
    category: 'AI_LLM',
    subcategory: null,
    type: 'PASSWORD',
    value: null,
    defaultValue: null,
    required: true,
    validationRules: null,
    options: null,
    unit: null,
    sensitive: true,
    hotReload: true,
    adminOnly: true,
    restartRequired: false
  } as SystemParameterView;

  const nichtSensiblerEintrag: SystemParameterView = {
    ...sensitiverEintrag,
    id: 'p2',
    paramKey: 'cvm.llm.injection.mode',
    type: 'STRING',
    sensitive: false,
    value: 'STRICT'
  } as SystemParameterView;

  beforeEach(() => {
    service = jasmine.createSpyObj<SystemParameterService>(
      'SystemParameterService',
      ['list', 'create', 'update', 'changeValue', 'reset', 'delete', 'auditLog']
    );
    service.list.and.returnValue(Promise.resolve([]));
    service.auditLog.and.returnValue(Promise.resolve([]));
    service.changeValue.and.returnValue(Promise.resolve(sensitiverEintrag));
    toast = new FakeToast();
    confirmService = new FakeConfirm();

    TestBed.configureTestingModule({
      imports: [AdminParametersComponent],
      providers: [
        { provide: SystemParameterService, useValue: service },
        { provide: CvmToastService, useValue: toast },
        { provide: CvmConfirmService, useValue: confirmService }
      ]
    });
  });

  it('wertAendern oeffnet bei sensiblem Eintrag den Rotations-Dialog', async () => {
    const component = TestBed.createComponent(AdminParametersComponent)
      .componentInstance;

    await component.wertAendern(sensitiverEintrag);

    expect(component.rotationOffen()).toBeTrue();
    expect(component.rotationEintrag()?.id).toBe('p1');
    expect(component.rotationSichtbar()).toBeFalse();
  });

  it('speichereRotation validiert leeren Wert', async () => {
    const component = TestBed.createComponent(AdminParametersComponent)
      .componentInstance;
    component.oeffneRotation(sensitiverEintrag);
    component.rotationGrund.set('egal');

    await component.speichereRotation();

    expect(component.rotationFehler()).toContain('Wert');
    expect(service.changeValue).not.toHaveBeenCalled();
  });

  it('speichereRotation verlangt Aenderungsgrund', async () => {
    const component = TestBed.createComponent(AdminParametersComponent)
      .componentInstance;
    component.oeffneRotation(sensitiverEintrag);
    component.rotationWert.set('new-secret');
    component.rotationGrund.set('  ');

    await component.speichereRotation();

    expect(component.rotationFehler()).toContain('Aenderungsgrund');
    expect(service.changeValue).not.toHaveBeenCalled();
  });

  it('speichereRotation ruft changeValue und schliesst Dialog bei Erfolg', async () => {
    const component = TestBed.createComponent(AdminParametersComponent)
      .componentInstance;
    component.oeffneRotation(sensitiverEintrag);
    component.rotationWert.set('new-secret');
    component.rotationGrund.set('Quartalsrotation 2026Q2');

    await component.speichereRotation();

    expect(service.changeValue).toHaveBeenCalledWith('p1', {
      value: 'new-secret',
      reason: 'Quartalsrotation 2026Q2'
    });
    expect(component.rotationOffen()).toBeFalse();
    expect(toast.success).toHaveBeenCalled();
  });

  it('rotationUmschalten togglet den Sichtbar-Zustand', () => {
    const component = TestBed.createComponent(AdminParametersComponent)
      .componentInstance;
    component.oeffneRotation(sensitiverEintrag);

    expect(component.rotationSichtbar()).toBeFalse();
    component.rotationUmschalten();
    expect(component.rotationSichtbar()).toBeTrue();
    component.rotationUmschalten();
    expect(component.rotationSichtbar()).toBeFalse();
  });

  it('brecheRotationAb schliesst den Dialog und leert den Fehlertext', () => {
    const component = TestBed.createComponent(AdminParametersComponent)
      .componentInstance;
    component.oeffneRotation(sensitiverEintrag);
    component.rotationFehler.set('err');
    component.brecheRotationAb();

    expect(component.rotationOffen()).toBeFalse();
    expect(component.rotationEintrag()).toBeNull();
    expect(component.rotationFehler()).toBeNull();
  });

  it('loesche nutzt CvmConfirmService statt window.confirm', async () => {
    const component = TestBed.createComponent(AdminParametersComponent)
      .componentInstance;
    confirmService.result = true;
    service.delete.and.returnValue(Promise.resolve());

    await component.loesche(nichtSensiblerEintrag);

    expect(confirmService.confirm).toHaveBeenCalled();
    expect(service.delete).toHaveBeenCalledWith('p2');
  });
});
