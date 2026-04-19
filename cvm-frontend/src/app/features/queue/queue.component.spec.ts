import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { QueueComponent } from './queue.component';
import { QueueApiService } from './queue-api.service';
import { QueueStore } from './queue-store';
import { AuthService } from '../../core/auth/auth.service';

class FakeApi {
  list = jasmine.createSpy('list').and.returnValue(of([]));
  approve = jasmine.createSpy('approve').and.returnValue(of({}));
  reject = jasmine.createSpy('reject').and.returnValue(of({}));
}

class FakeAuth {
  loggedIn = () => true;
  userRoles = () => ['CVM_APPROVER'];
  username = () => 't.tester@ahs.test';
  hasRole = () => true;
  refreshFromKeycloak(): void {}
  async login(): Promise<void> {}
  async logout(): Promise<void> {}
  async getToken(): Promise<string> {
    return '';
  }
}

describe('QueueComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      imports: [QueueComponent],
      providers: [
        provideRouter([]),
        QueueStore,
        { provide: QueueApiService, useClass: FakeApi },
        { provide: AuthService, useClass: FakeAuth },
        { provide: ActivatedRoute, useValue: { queryParamMap: paramMap$ } }
      ]
    });
  });

  it('kompiliert und rendert Header', () => {
    const fixture = TestBed.createComponent(QueueComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Bewertungs-Queue');
  });

  it('Iteration 80: Empty-State zeigt die drei Workflow-CTAs', () => {
    const fixture = TestBed.createComponent(QueueComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const empty = el.querySelector('[data-testid="queue-empty-state"]');
    expect(empty).withContext('Empty-State sichtbar').not.toBeNull();
    expect(el.querySelector('[data-testid="queue-empty-cta-scan"]'))
      .not.toBeNull();
    expect(el.querySelector('[data-testid="queue-empty-cta-reports"]'))
      .not.toBeNull();
    expect(el.querySelector('[data-testid="queue-empty-cta-dashboard"]'))
      .not.toBeNull();
  });

  it('Iteration 80: liest queryParams und setzt den Store-Filter', () => {
    const fixture = TestBed.createComponent(QueueComponent);
    const store = TestBed.inject(QueueStore);
    fixture.detectChanges();
    paramMap$.next(convertToParamMap({
      productVersionId: 'pv-1',
      environmentId: 'env-1',
      status: 'PROPOSED'
    }));
    fixture.detectChanges();
    const filter = store.filter();
    expect(filter.productVersionId).toBe('pv-1');
    expect(filter.environmentId).toBe('env-1');
    expect(filter.status).toBe('PROPOSED');
  });
});
