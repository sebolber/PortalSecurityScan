import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ReachabilityComponent } from './reachability.component';
import {
  ReachabilityQueryService
} from '../../core/reachability/reachability.service';
import { AuthService } from '../../core/auth/auth.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

class FakeApi {
  async list(): Promise<never[]> { return []; }
}
class FakeAuth {
  username = () => 't.tester@ahs.test';
  hasRole = () => true;
  loggedIn = () => true;
  userRoles = () => [];
  refreshFromKeycloak(): void {}
  async login(): Promise<void> {}
  async logout(): Promise<void> {}
  async getToken(): Promise<string> { return ''; }
}
class FakeToast {
  success = jasmine.createSpy();
  warning = jasmine.createSpy();
  error = jasmine.createSpy();
}

describe('ReachabilityComponent - Iteration 81 Workflow-CTA', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReachabilityComponent],
      providers: [
        provideRouter([]),
        { provide: ReachabilityQueryService, useClass: FakeApi },
        { provide: AuthService, useClass: FakeAuth },
        { provide: CvmToastService, useClass: FakeToast }
      ]
    });
  });

  it('hat einen "Zur Fix-Verifikation"-Header-Button', () => {
    const fixture = TestBed.createComponent(ReachabilityComponent);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="reachability-to-fix-verification"]'
    ) as HTMLAnchorElement;
    expect(link).not.toBeNull();
    expect(link.getAttribute('href')).toBe('/fix-verification');
  });
});
