import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { QueueStore } from './queue-store';
import { QueueApiService } from './queue-api.service';
import { QueueEntry } from './queue.types';

function entry(
  id: string,
  overrides: Partial<QueueEntry> = {}
): QueueEntry {
  return {
    id,
    findingId: 'f-' + id,
    cveId: 'c-' + id,
    cveKey: 'CVE-2024-' + id,
    severity: 'HIGH',
    status: 'PROPOSED',
    source: 'RULE',
    rationale: null,
    decidedBy: null,
    version: 1,
    createdAt: '2026-04-17T10:00:00Z',
    ...overrides
  };
}

class FakeApi {
  list = jasmine
    .createSpy('list')
    .and.returnValue(of<QueueEntry[]>([]));
  approve = jasmine
    .createSpy('approve')
    .and.returnValue(of<QueueEntry>(entry('any')));
  reject = jasmine
    .createSpy('reject')
    .and.returnValue(of<QueueEntry>(entry('any')));
}

describe('QueueStore', () => {
  let store: QueueStore;
  let api: FakeApi;

  beforeEach(() => {
    api = new FakeApi();
    TestBed.configureTestingModule({
      providers: [
        QueueStore,
        { provide: QueueApiService, useValue: api }
      ]
    });
    store = TestBed.inject(QueueStore);
  });

  it('sortiert Eintraege nach Severity absteigend, dann nach createdAt aufsteigend', () => {
    store.seed([
      entry('a', { severity: 'LOW', createdAt: '2026-04-01' }),
      entry('b', { severity: 'CRITICAL', createdAt: '2026-04-16' }),
      entry('c', { severity: 'CRITICAL', createdAt: '2026-04-10' }),
      entry('d', { severity: 'HIGH', createdAt: '2026-04-15' })
    ]);
    const ids = store.entries().map((e) => e.id);
    expect(ids).toEqual(['c', 'b', 'd', 'a']);
  });

  it('Severity-Filter beschraenkt client-seitig', () => {
    store.seed([
      entry('a', { severity: 'LOW' }),
      entry('b', { severity: 'CRITICAL' }),
      entry('c', { severity: 'HIGH' })
    ]);
    store.toggleSeverityFilter('CRITICAL');
    store.toggleSeverityFilter('HIGH');
    expect(store.entries().map((e) => e.id)).toEqual(['b', 'c']);
  });

  it('moveSelection springt zum naechsten Eintrag', () => {
    store.seed([
      entry('a', { severity: 'CRITICAL', createdAt: '2026-04-01' }),
      entry('b', { severity: 'HIGH', createdAt: '2026-04-02' })
    ]);
    store.select(null);
    store.moveSelection(1);
    expect(store.selectedId()).toBe('a');
    store.moveSelection(1);
    expect(store.selectedId()).toBe('b');
    store.moveSelection(1);
    // Am Ende bleibt die Auswahl beim letzten Eintrag.
    expect(store.selectedId()).toBe('b');
    store.moveSelection(-1);
    expect(store.selectedId()).toBe('a');
  });

  it('reload fuellt aus der API und reset-et die Auswahl, wenn nicht mehr vorhanden', async () => {
    api.list.and.returnValue(of([entry('x')]));
    await store.reload();
    store.select('x');
    api.list.and.returnValue(of([]));
    await store.reload();
    expect(store.selectedId()).toBeNull();
    expect(store.loading()).toBeFalse();
  });

  it('reload haelt Fehler im error-Signal', async () => {
    api.list.and.returnValue(throwError(() => new Error('offline')));
    await store.reload();
    expect(store.error()).toContain('offline');
    expect(store.loading()).toBeFalse();
  });

  it('approve entfernt optimistisch und rollt bei Fehler zurueck', async () => {
    store.seed([entry('a'), entry('b')]);
    api.approve.and.returnValue(throwError(() => new Error('409')));
    const ok = await store.approve('a', { approverId: 'u1' });
    expect(ok).toBeFalse();
    expect(store.entries().map((e) => e.id).sort()).toEqual(['a', 'b']);
    expect(store.error()).toContain('409');
  });

  it('approve entfernt den Eintrag bei Erfolg und putzt die Auswahl', async () => {
    store.seed([entry('a'), entry('b')]);
    store.select('a');
    const ok = await store.approve('a', { approverId: 'u1' });
    expect(ok).toBeTrue();
    expect(store.entries().map((e) => e.id)).toEqual(['b']);
    expect(store.selectedId()).toBeNull();
    expect(api.approve).toHaveBeenCalledWith('a', { approverId: 'u1' });
  });

  it('reject entfernt optimistisch und meldet Erfolg', async () => {
    store.seed([entry('a')]);
    const ok = await store.reject('a', { approverId: 'u1', comment: 'nope' });
    expect(ok).toBeTrue();
    expect(store.entries()).toEqual([]);
  });

  it('toggleChecked sammelt und entfernt IDs', () => {
    store.seed([entry('a'), entry('b')]);
    store.toggleChecked('a');
    store.toggleChecked('b');
    expect(store.checkedCount()).toBe(2);
    store.toggleChecked('a');
    expect(store.checkedCount()).toBe(1);
    store.clearChecked();
    expect(store.checkedCount()).toBe(0);
  });
});
