import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { ApiClient } from './api-client.service';
import { HttpErrorHandler } from './http-error-handler';
import { AppConfigService } from '../config/app-config.service';

class FakeConfig {
  get() {
    return {
      apiBaseUrl: 'http://api.test/',
      keycloak: { url: '', realm: '', clientId: '' }
    };
  }
}

class FakeErrorHandler {
  show = jasmine.createSpy('show');
  format = () => '';
}

describe('ApiClient', () => {
  let client: ApiClient;
  let httpMock: HttpTestingController;
  let errorHandler: FakeErrorHandler;

  beforeEach(() => {
    errorHandler = new FakeErrorHandler();
    TestBed.configureTestingModule({
      providers: [
        ApiClient,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useClass: FakeConfig },
        { provide: HttpErrorHandler, useValue: errorHandler }
      ]
    });
    client = TestBed.inject(ApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('haengt Pfad an die normalisierte Base-URL', () => {
    expect(client.url('v1/findings')).toBe('http://api.test/v1/findings');
    expect(client.url('/v1/findings')).toBe('http://api.test/v1/findings');
  });

  it('GET liefert Body und ruft errorHandler nicht', () => {
    client.get<{ ok: boolean }>('/v1/findings').subscribe((res) => {
      expect(res.ok).toBeTrue();
    });
    const req = httpMock.expectOne('http://api.test/v1/findings');
    req.flush({ ok: true });
    expect(errorHandler.show).not.toHaveBeenCalled();
  });

  it('GET ruft errorHandler bei 5xx', () => {
    client.get('/v1/findings').subscribe({ error: () => undefined });
    const req = httpMock.expectOne('http://api.test/v1/findings');
    req.flush('boom', { status: 500, statusText: 'Server Error' });
    expect(errorHandler.show).toHaveBeenCalled();
  });
});
