import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
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
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [QueueComponent],
      providers: [
        QueueStore,
        { provide: QueueApiService, useClass: FakeApi },
        { provide: AuthService, useClass: FakeAuth }
      ]
    });
  });

  it('kompiliert und rendert Header', () => {
    const fixture = TestBed.createComponent(QueueComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Bewertungs-Queue');
  });
});
