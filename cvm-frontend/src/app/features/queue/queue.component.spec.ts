import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import { QueueComponent } from './queue.component';
import { QueueApiService } from './queue-api.service';
import { QueueStore } from './queue-store';
import { AuthService } from '../../core/auth/auth.service';
import { EnvironmentsService } from '../../core/environments/environments.service';
import { ProductsService } from '../../core/products/products.service';

class FakeApi {
  list = jasmine.createSpy('list').and.returnValue(of([]));
  approve = jasmine.createSpy('approve').and.returnValue(of({}));
  reject = jasmine.createSpy('reject').and.returnValue(of({}));
}

class FakeProducts {
  list = () => Promise.resolve([]);
  versions = () => Promise.resolve([]);
}
class FakeEnvs {
  list = () => Promise.resolve([]);
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
  let routeSnapshotMap = convertToParamMap({});

  beforeEach(() => {
    paramMap$ = new BehaviorSubject(convertToParamMap({}));
    routeSnapshotMap = convertToParamMap({});
    const fakeRoute = {
      queryParamMap: paramMap$,
      get snapshot() {
        return { queryParamMap: routeSnapshotMap };
      }
    };
    TestBed.configureTestingModule({
      imports: [QueueComponent],
      providers: [
        provideRouter([]),
        QueueStore,
        { provide: QueueApiService, useClass: FakeApi },
        { provide: AuthService, useClass: FakeAuth },
        { provide: ProductsService, useClass: FakeProducts },
        { provide: EnvironmentsService, useClass: FakeEnvs },
        { provide: ActivatedRoute, useValue: fakeRoute }
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

  it('Iteration 82: Store-Filter-Aenderung navigiert zur URL', () => {
    const router = TestBed.inject(Router);
    const spy = spyOn(router, 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(QueueComponent);
    const store = TestBed.inject(QueueStore);
    fixture.detectChanges();
    spy.calls.reset();

    store.setFilter({ status: 'APPROVED' });
    fixture.detectChanges();

    expect(spy).toHaveBeenCalled();
    const lastCall = spy.calls.mostRecent();
    const params = (lastCall.args[1] as { queryParams: Record<string, unknown> })
      .queryParams;
    expect(params['status']).toBe('APPROVED');
    expect(params['productVersionId']).toBeNull();
  });
});
