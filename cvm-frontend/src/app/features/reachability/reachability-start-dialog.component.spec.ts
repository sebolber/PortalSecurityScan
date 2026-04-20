import { TestBed } from '@angular/core/testing';
import { ReachabilityStartDialogComponent } from './reachability-start-dialog.component';
import {
  ReachabilityQueryService,
  ReachabilityResult,
  ReachabilityStartContext,
  ReachabilityStartRequest
} from '../../core/reachability/reachability.service';

class FakeReach {
  lastRequest?: ReachabilityStartRequest;
  startAnalysis = jasmine
    .createSpy('startAnalysis')
    .and.callFake(
      (_id: string, req: ReachabilityStartRequest): Promise<ReachabilityResult> => {
        this.lastRequest = req;
        return Promise.resolve({
          recommendation: 'ACCEPT',
          summary: 'ok',
          callSites: [],
          available: true,
          noteIfUnavailable: null
        } as ReachabilityResult);
      }
    );
  suggestion = jasmine.createSpy('suggestion').and.rejectWith(new Error('no'));
  contextResponse: ReachabilityStartContext = {
    findingId: 'f1',
    repoUrl: null,
    commitSha: null,
    rationale: null
  };
  context = jasmine
    .createSpy('context')
    .and.callFake(() => Promise.resolve(this.contextResponse));
}

/**
 * Iteration 97 (CVM-337): Pflichtfeld-Validierung fuer commitSha,
 * damit das UI vor dem Backend-400 greift.
 */
describe('ReachabilityStartDialogComponent - commitSha Pflichtfeld', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [ReachabilityStartDialogComponent],
      providers: [{ provide: ReachabilityQueryService, useClass: FakeReach }]
    });
  });

  afterEach(() => localStorage.clear());

  function mkComponent() {
    const fixture = TestBed.createComponent(ReachabilityStartDialogComponent);
    fixture.componentInstance.open = true;
    fixture.componentInstance.data = {
      findingId: 'f1',
      triggeredBy: 't.tester@ahs.test'
    };
    return fixture.componentInstance;
  }

  it('istFormularGueltig false, wenn commitSha leer ist', () => {
    const c = mkComponent();
    c.updateFeld('repoUrl', 'https://x/y.git');
    c.updateFeld('vulnerableSymbol', 'X.y');
    c.updateFeld('commitSha', '');
    expect(c.istFormularGueltig()).toBeFalse();
  });

  it('istFormularGueltig true, wenn alle drei Pflichtfelder gefuellt sind', () => {
    const c = mkComponent();
    c.updateFeld('repoUrl', 'https://x/y.git');
    c.updateFeld('vulnerableSymbol', 'X.y');
    c.updateFeld('commitSha', 'abc1234');
    expect(c.istFormularGueltig()).toBeTrue();
  });

  it('starteDirekt sendet commitSha getrimmt ohne null-Fallback', async () => {
    const c = mkComponent();
    const reach = TestBed.inject(ReachabilityQueryService) as unknown as FakeReach;
    c.updateFeld('repoUrl', 'https://x/y.git');
    c.updateFeld('vulnerableSymbol', 'X.y');
    c.updateFeld('commitSha', '  abc1234  ');

    await c.starteDirekt();

    expect(reach.startAnalysis).toHaveBeenCalled();
    expect(reach.lastRequest?.commitSha).toBe('abc1234');
  });

  it('starteDirekt setzt Fehler-Text, wenn commitSha leer ist', async () => {
    const c = mkComponent();
    const reach = TestBed.inject(ReachabilityQueryService) as unknown as FakeReach;
    c.updateFeld('repoUrl', 'https://x/y.git');
    c.updateFeld('vulnerableSymbol', 'X.y');

    await c.starteDirekt();

    expect(reach.startAnalysis).not.toHaveBeenCalled();
    expect(c.fehler()).toContain('Commit-SHA');
  });

  it('CVM-339: ladeKontext befuellt leere Pflichtfelder aus Product/Version', async () => {
    const c = mkComponent();
    const reach = TestBed.inject(ReachabilityQueryService) as unknown as FakeReach;
    reach.contextResponse = {
      findingId: 'f1',
      repoUrl: 'https://git.example/portalcore.git',
      commitSha: 'a3f9beef',
      rationale: 'Vorbelegt aus Produkt und Produkt-Version.'
    };
    // Trigger ngOnChanges-Pfad auf open=true.
    c.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false }
    });
    await Promise.resolve();
    await Promise.resolve();

    expect(reach.context).toHaveBeenCalledWith('f1');
    expect(c.formular().repoUrl).toBe('https://git.example/portalcore.git');
    expect(c.formular().commitSha).toBe('a3f9beef');
    expect(c.kontext()?.rationale).toContain('Vorbelegt');
  });

  it('CVM-339: leere Context-Felder belassen das Formular unveraendert', async () => {
    const c = mkComponent();
    const reach = TestBed.inject(ReachabilityQueryService) as unknown as FakeReach;
    reach.contextResponse = {
      findingId: 'f1',
      repoUrl: null,
      commitSha: null,
      rationale: 'Produkt hat keine Repo-URL - bitte manuell eintragen.'
    };
    c.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false }
    });
    await Promise.resolve();
    await Promise.resolve();

    expect(c.formular().repoUrl).toBe('');
    expect(c.formular().commitSha).toBe('');
    expect(c.kontext()?.rationale).toContain('Produkt hat keine Repo-URL');
  });
});
