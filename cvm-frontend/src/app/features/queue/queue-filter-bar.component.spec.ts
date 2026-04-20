import { TestBed } from '@angular/core/testing';
import { QueueFilterBarComponent } from './queue-filter-bar.component';
import { QueueStore } from './queue-store';
import { EnvironmentsService } from '../../core/environments/environments.service';
import { ProductsService } from '../../core/products/products.service';

class FakeProducts {
  list = () => Promise.resolve([]);
  versions = () => Promise.resolve([]);
}
class FakeEnvs {
  list = () => Promise.resolve([]);
}

/**
 * Iteration 47: der Filter liegt jetzt als Balken **oberhalb** der Tabelle
 * statt als linke Sidebar. Der Spec ueberprueft die identische Logik.
 */
describe('QueueFilterBarComponent', () => {
  let store: jasmine.SpyObj<QueueStore>;

  beforeEach(() => {
    store = jasmine.createSpyObj<QueueStore>('QueueStore', [
      'filter',
      'setFilter',
      'toggleSeverityFilter',
      'resetFilter'
    ]);
    store.filter.and.returnValue({} as never);

    TestBed.configureTestingModule({
      imports: [QueueFilterBarComponent],
      providers: [
        { provide: QueueStore, useValue: store },
        { provide: ProductsService, useClass: FakeProducts },
        { provide: EnvironmentsService, useClass: FakeEnvs }
      ]
    });
  });

  it('setzt produktversion auf undefined wenn Feld geleert wird', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.componentInstance.auf('productVersionId', '   ');
    expect(store.setFilter).toHaveBeenCalledWith({ productVersionId: undefined });
  });

  it('uebernimmt die getrimmte Umgebungs-UUID', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.componentInstance.auf('environmentId', ' abc ');
    expect(store.setFilter).toHaveBeenCalledWith({ environmentId: 'abc' });
  });

  it('reset ruft resetFilter am Store auf', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.componentInstance.reset();
    expect(store.resetFilter).toHaveBeenCalled();
  });

  it('toggle gibt Severity an toggleSeverityFilter durch', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.componentInstance.toggle('HIGH');
    expect(store.toggleSeverityFilter).toHaveBeenCalledWith('HIGH');
  });

  it('Iteration 82: rendert sechs Status-Chips (ALLE/PROPOSED/NEEDS_REVIEW/APPROVED/REJECTED/EXPIRED)', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="queue-status-ALL"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="queue-status-PROPOSED"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="queue-status-NEEDS_REVIEW"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="queue-status-APPROVED"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="queue-status-REJECTED"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="queue-status-EXPIRED"]')).not.toBeNull();
  });

  it('Iteration 82: Klick auf APPROVED-Chip setzt Store-Filter-Status', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector(
      '[data-testid="queue-status-APPROVED"]'
    ) as HTMLButtonElement;
    btn.click();
    expect(store.setFilter).toHaveBeenCalledWith({ status: 'APPROVED' });
  });

  it('Iteration 82: Klick auf ALLE-Chip setzt status=undefined', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector(
      '[data-testid="queue-status-ALL"]'
    ) as HTMLButtonElement;
    btn.click();
    expect(store.setFilter).toHaveBeenCalledWith({ status: undefined });
  });

  it('Iteration 98: uebernehmeProduktVersion setzt Store-Filter und Label', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.componentInstance.pvPickerOffen.set(true);
    fixture.componentInstance.uebernehmeProduktVersion({
      versionId: 'abc-123',
      label: 'PortalCore-Test 1.14.2'
    });
    expect(store.setFilter).toHaveBeenCalledWith({ productVersionId: 'abc-123' });
    expect(fixture.componentInstance.pvLabel()).toBe('PortalCore-Test 1.14.2');
    expect(fixture.componentInstance.pvPickerOffen()).toBeFalse();
  });

  it('Iteration 98: uebernehmeUmgebung setzt Store-Filter und Label', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.componentInstance.envPickerOffen.set(true);
    fixture.componentInstance.uebernehmeUmgebung({
      environmentId: 'env-1',
      label: 'Referenz Test (REF)'
    });
    expect(store.setFilter).toHaveBeenCalledWith({ environmentId: 'env-1' });
    expect(fixture.componentInstance.envLabel()).toBe('Referenz Test (REF)');
    expect(fixture.componentInstance.envPickerOffen()).toBeFalse();
  });

  it('Iteration 98: manuelles Editieren der UUID verwirft das Label', () => {
    const fixture = TestBed.createComponent(QueueFilterBarComponent);
    fixture.componentInstance.pvLabel.set('alt');
    fixture.componentInstance.auf('productVersionId', 'neu');
    expect(fixture.componentInstance.pvLabel()).toBeNull();
  });
});
