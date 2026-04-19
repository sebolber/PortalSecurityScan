import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppConfigService } from '../config/app-config.service';
import { HttpErrorHandler } from '../api/http-error-handler';
import { WaiversService } from './waivers.service';

describe('WaiversService - Iteration 85 extend/revoke', () => {
  let service: WaiversService;
  let http: HttpTestingController;

  beforeEach(() => {
    const config = jasmine.createSpyObj<AppConfigService>(
      'AppConfigService',
      ['get']
    );
    config.get.and.returnValue({
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    } as any);
    const errorHandler = jasmine.createSpyObj<HttpErrorHandler>(
      'HttpErrorHandler',
      ['show']
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: config },
        { provide: HttpErrorHandler, useValue: errorHandler }
      ]
    });
    service = TestBed.inject(WaiversService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('extend: POST /waivers/{id}/extend mit validUntil und extendedBy', async () => {
    const promise = service.extend('w1', '2026-12-31T00:00:00Z', 'a.admin');
    const req = http.expectOne('http://api.test/api/v1/waivers/w1/extend');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      validUntil: '2026-12-31T00:00:00Z',
      extendedBy: 'a.admin'
    });
    req.flush({
      id: 'w1',
      assessmentId: 'a1',
      reason: 'risk',
      grantedBy: 't.tester',
      validUntil: '2026-12-31T00:00:00Z',
      reviewIntervalDays: 90,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      extendedAt: '2026-04-19T00:00:00Z',
      revokedAt: null
    });
    const res = await promise;
    expect(res.validUntil).toBe('2026-12-31T00:00:00Z');
  });

  it('revoke: POST /waivers/{id}/revoke mit revokedBy und reason', async () => {
    const promise = service.revoke('w1', 'a.admin', 'Risiko nicht mehr akzeptabel');
    const req = http.expectOne('http://api.test/api/v1/waivers/w1/revoke');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      revokedBy: 'a.admin',
      reason: 'Risiko nicht mehr akzeptabel'
    });
    req.flush({
      id: 'w1',
      assessmentId: 'a1',
      reason: 'risk',
      grantedBy: 't.tester',
      validUntil: '2026-12-31T00:00:00Z',
      reviewIntervalDays: 90,
      status: 'REVOKED',
      createdAt: '2026-01-01T00:00:00Z',
      extendedAt: null,
      revokedAt: '2026-04-19T00:00:00Z'
    });
    const res = await promise;
    expect(res.status).toBe('REVOKED');
  });
});
