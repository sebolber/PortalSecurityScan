import { TestBed } from '@angular/core/testing';
import { QueueFilterBarComponent } from './queue-filter-bar.component';
import { QueueStore } from './queue-store';

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
      providers: [{ provide: QueueStore, useValue: store }]
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
});
