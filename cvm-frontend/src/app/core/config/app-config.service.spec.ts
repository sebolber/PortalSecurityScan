import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AppConfigService } from './app-config.service';

describe('AppConfigService', () => {
  let service: AppConfigService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AppConfigService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AppConfigService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('get() vor load() wirft Fehler', () => {
    expect(() => service.get()).toThrowError(/noch nicht geladen/);
  });

  it('load() liefert assets/config.json und cached den Wert', async () => {
    const promise = service.load();
    const req = httpMock.expectOne('assets/config.json');
    req.flush({
      apiBaseUrl: 'http://api.test',
      keycloak: { url: 'http://kc.test', realm: 'cvm', clientId: 'cvm-client' }
    });
    const cfg = await promise;

    expect(cfg.apiBaseUrl).toBe('http://api.test');
    expect(service.get().keycloak.realm).toBe('cvm');
  });
});
