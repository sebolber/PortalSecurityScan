import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { AppConfigService } from '../config/app-config.service';

class FakeAuth {
  loggedIn = () => true;
  userRoles = () => [];
  hasRole = () => false;
  getToken = jasmine.createSpy('getToken').and.resolveTo('test-token');
  logout = jasmine.createSpy('logout').and.resolveTo();
}

class FakeConfig {
  get() {
    return {
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    };
  }
}

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: FakeAuth;

  beforeEach(() => {
    auth = new FakeAuth();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: AppConfigService, useClass: FakeConfig }
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('setzt Bearer-Token fuer API-Calls', async () => {
    const promise = new Promise<void>((resolve) => {
      http.get('http://api.test/v1/findings').subscribe(() => resolve());
    });
    await Promise.resolve();
    const req = httpMock.expectOne('http://api.test/v1/findings');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush({});
    await promise;
  });

  it('laesst lokale Asset-Calls unveraendert', () => {
    http.get('assets/config.json').subscribe();
    const req = httpMock.expectOne('assets/config.json');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('triggert logout bei 401', async () => {
    http.get('http://api.test/v1/findings').subscribe({
      error: () => undefined
    });
    await Promise.resolve();
    const req = httpMock.expectOne('http://api.test/v1/findings');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalled();
  });
});
