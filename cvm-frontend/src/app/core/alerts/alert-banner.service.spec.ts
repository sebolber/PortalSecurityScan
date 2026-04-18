import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { AlertBannerService } from './alert-banner.service';
import { ApiClient } from '../api/api-client.service';
import { AppConfigService } from '../config/app-config.service';
import { HttpErrorHandler } from '../api/http-error-handler';

class FakeConfig {
  get() {
    return {
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    };
  }
}

class FakeErrorHandler {
  show = jasmine.createSpy('show');
  format = () => '';
}

describe('AlertBannerService', () => {
  let service: AlertBannerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AlertBannerService,
        ApiClient,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useClass: FakeConfig },
        { provide: HttpErrorHandler, useClass: FakeErrorHandler }
      ]
    });
    service = TestBed.inject(AlertBannerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('uebernimmt den Status aus dem Backend', async () => {
    const refresh = service.refresh();
    const req = httpMock.expectOne('http://api.test/api/v1/alerts/banner');
    req.flush({ visible: true, count: 2, t2Minutes: 360 });
    await refresh;
    expect(service.status()).toEqual({
      visible: true,
      count: 2,
      t2Minutes: 360
    });
  });

  it('setzt Status auf null bei Fehler', async () => {
    const refresh = service.refresh();
    const req = httpMock.expectOne('http://api.test/api/v1/alerts/banner');
    req.flush('boom', { status: 500, statusText: 'fail' });
    await refresh;
    expect(service.status()).toBeNull();
  });
});
