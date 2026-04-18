import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AppConfigService } from '../config/app-config.service';
import { AiAuditService } from './ai-audit.service';

describe('AiAuditService', () => {
  let service: AiAuditService;
  let http: HttpTestingController;

  beforeEach(() => {
    const config = jasmine.createSpyObj<AppConfigService>('AppConfigService', ['get']);
    config.get.and.returnValue({
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    } as any);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: config }
      ]
    });
    service = TestBed.inject(AiAuditService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste() ohne Filter -> nur page+size in QueryString', async () => {
    const promise = firstValueFrom(service.liste());
    const req = http.expectOne('http://api.test/api/v1/ai/audits?page=0&size=20');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    await promise;
  });

  it('liste() mit Filter -> status und useCase werden angehaengt', async () => {
    const promise = firstValueFrom(service.liste({
      status: 'INVALID_OUTPUT',
      useCase: 'AUTO_ASSESSMENT',
      page: 2,
      size: 50
    }));
    const req = http.expectOne(
      'http://api.test/api/v1/ai/audits?status=INVALID_OUTPUT&useCase=AUTO_ASSESSMENT&page=2&size=50'
    );
    req.flush({ content: [], page: 2, size: 50, totalElements: 0, totalPages: 0 });
    await promise;
  });
});
